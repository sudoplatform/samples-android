/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.sudos

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sudoplatform.emailexample.App
import com.sudoplatform.emailexample.R
import com.sudoplatform.sudokeymanager.KeyManagerException
import com.sudoplatform.sudoprofiles.ListOption
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import com.sudoplatform.sudoprofiles.exceptions.SudoProfileException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SudosUiState(
    val isLoading: Boolean = false,
    /** True once the service has returned the Sudos, so an empty list means there are none. */
    val hasLoaded: Boolean = false,
    /** Sorted oldest first. */
    val sudos: List<Sudo> = emptyList(),
    /** A Sudo the user swiped away, awaiting confirmation before it is deleted. */
    val pendingDelete: Sudo? = null,
    val isDeleting: Boolean = false,
    /** True while the user is deregistering or signing out. */
    val isSigningOut: Boolean = false,
    val listError: String? = null,
    val deleteError: String? = null,
    /** Why deregistering or signing out failed. */
    val signOutError: String? = null,
    /** Key backup needs Federated Sign In, as a TEST registration can't sign in on another device. */
    val usedFsso: Boolean = false,
    /**
     * True if the Sudos couldn't be decrypted because this device doesn't have their keys, e.g.
     * they were created on another device. Importing a key backup fixes this.
     */
    val sudosNeedKeyImport: Boolean = false,
    /** True if this device created a new shared email key that no key backup contains yet. */
    val emailKeyNeedsBackup: Boolean = false,
)

/** Emitted once the user has deregistered or signed out, so the app can return to registration. */
data object SignedOut

/** Lists the user's Sudos, deletes them on request, and deregisters or signs out the user. */
class SudosViewModel(
    private val app: App,
) : ViewModel() {
    companion object {
        val Factory =
            viewModelFactory {
                initializer { SudosViewModel(this[APPLICATION_KEY] as App) }
            }
    }

    private val sudoProfilesClient: SudoProfilesClient get() = app.sudoProfilesClient

    private val _uiState =
        MutableStateFlow(
            SudosUiState(
                usedFsso =
                    app
                        .getSharedPreferences(App.SIGN_IN_PREFERENCES, Context.MODE_PRIVATE)
                        .getBoolean(App.FSSO_USED_PREFERENCE, false),
            ),
        )
    val uiState: StateFlow<SudosUiState> = _uiState.asStateFlow()

    private val _signedOut = Channel<SignedOut>(Channel.BUFFERED)
    val signedOut: Flow<SignedOut> = _signedOut.receiveAsFlow()

    init {
        // Show cached Sudos straight away; the screen fetches from the service when it resumes.
        listSudos(ListOption.CACHE_ONLY)
    }

    /** Fetch the Sudos from the service, for example after one was created or edited. */
    fun refresh() {
        _uiState.update { it.copy(emailKeyNeedsBackup = app.emailKeyNeedsBackup) }
        listSudos(ListOption.REMOTE_ONLY)
    }

    /**
     * Ask the user to confirm deleting the Sudo with [sudoId]. The list's copy is used because
     * deleteSudo needs the Sudo's current version, which changes each time it is updated.
     */
    fun requestDelete(sudoId: String) {
        _uiState.update { state -> state.copy(pendingDelete = state.sudos.find { it.id == sudoId }) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(pendingDelete = null) }
    }

    /**
     * Delete the Sudo awaiting confirmation. It is removed from the list immediately and restored
     * if the delete fails.
     */
    fun confirmDelete() {
        val sudo = _uiState.value.pendingDelete ?: return
        _uiState.update { state ->
            state.copy(pendingDelete = null, isDeleting = true, sudos = state.sudos.filterNot { it.id == sudo.id })
        }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { sudoProfilesClient.deleteSudo(sudo) }
                _uiState.update { it.copy(isDeleting = false) }
            } catch (e: SudoProfileException) {
                _uiState.update { it.copy(isDeleting = false, deleteError = e.describe()) }
                refresh()
            }
        }
    }

    /** Deregister a TEST registration from the Sudo Platform and clear all local data. */
    fun deregister() {
        signOut(R.string.deregister_failure) { withContext(Dispatchers.IO) { app.deregister() } }
    }

    /**
     * Sign out of Federated Sign In. With [removeData], the user's keys are deleted from this
     * device, so their Sudos and email can only be read here again by importing a key backup.
     */
    fun signOutFederated(removeData: Boolean) {
        signOut(R.string.sign_out_failure) { app.signOutFederated(removeData) }
    }

    private fun signOut(
        @StringRes failureResId: Int,
        operation: suspend () -> Unit,
    ) {
        _uiState.update { it.copy(isSigningOut = true) }
        viewModelScope.launch {
            try {
                operation()
                _signedOut.send(SignedOut)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                app.logger.error("Failed to sign out: $e")
                _uiState.update { it.copy(signOutError = app.getString(failureResId, e.describe())) }
            } finally {
                _uiState.update { it.copy(isSigningOut = false) }
            }
        }
    }

    fun dismissListError() {
        _uiState.update { it.copy(listError = null) }
    }

    fun dismissDeleteError() {
        _uiState.update { it.copy(deleteError = null) }
    }

    fun dismissSignOutError() {
        _uiState.update { it.copy(signOutError = null) }
    }

    private fun listSudos(option: ListOption) {
        _uiState.update { it.copy(isLoading = true, listError = null) }
        viewModelScope.launch {
            try {
                val sudos = withContext(Dispatchers.IO) { sudoProfilesClient.listSudos(option) }.sortedBy(Sudo::createdAt)
                if (option == ListOption.CACHE_ONLY) {
                    // Cached Sudos are shown until the service responds, and never replace its result.
                    if (!_uiState.value.hasLoaded) {
                        _uiState.update { it.copy(sudos = sudos) }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, hasLoaded = true, sudos = sudos, sudosNeedKeyImport = false) }
                }
            } catch (e: SudoProfileException) {
                // A failed cache read is followed by a fetch from the service, so it isn't reported.
                if (option != ListOption.CACHE_ONLY) {
                    if (e.isMissingKey()) {
                        _uiState.update { it.copy(isLoading = false, sudosNeedKeyImport = true) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, listError = e.describe()) }
                    }
                }
            }
        }
    }

    /**
     * One claim this device can't decrypt fails the whole list. The key manager's failure is the
     * cause: a missing key surfaces as a decryption failure, not as KeyNotFoundException.
     */
    private fun SudoProfileException.isMissingKey(): Boolean = generateSequence(cause) { it.cause }.any { it is KeyManagerException }

    private fun Throwable.describe(): String = localizedMessage ?: toString()
}
