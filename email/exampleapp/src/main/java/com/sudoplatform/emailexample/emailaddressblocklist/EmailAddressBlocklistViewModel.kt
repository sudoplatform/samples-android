/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaddressblocklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sudoplatform.emailexample.App
import com.sudoplatform.emailexample.R
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudoemail.types.BatchOperationStatus
import com.sudoplatform.sudoemail.types.UnsealedBlockedAddress
import com.sudoplatform.sudoemail.types.UnsealedBlockedAddressStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EmailAddressBlocklistUiState(
    val isLoading: Boolean = false,
    /** True once the blocklist has loaded, so an empty list means nothing is blocked. */
    val hasLoaded: Boolean = false,
    val blockedAddresses: List<UnsealedBlockedAddress> = emptyList(),
    /** The hashed values of the addresses the user has ticked. */
    val selected: Set<String> = emptySet(),
    val isUnblocking: Boolean = false,
    val listError: String? = null,
    val unblockError: String? = null,
)

/** Lists the user's blocked addresses and unblocks those selected. */
class EmailAddressBlocklistViewModel(
    private val app: App,
) : ViewModel() {
    companion object {
        val Factory =
            viewModelFactory {
                initializer {
                    EmailAddressBlocklistViewModel(app = this[APPLICATION_KEY] as App)
                }
            }
    }

    private val emailClient: SudoEmailClient get() = app.sudoEmailClient

    private val _uiState = MutableStateFlow(EmailAddressBlocklistUiState())
    val uiState: StateFlow<EmailAddressBlocklistUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Fetch the blocklist. Addresses that couldn't be unsealed, most likely because this device
     * doesn't have the key, are left out.
     */
    fun load() {
        _uiState.update { it.copy(isLoading = true, listError = null) }
        viewModelScope.launch {
            try {
                val blocklist = withContext(Dispatchers.IO) { emailClient.getEmailAddressBlocklist() }
                val completed = blocklist.filter { it.status == UnsealedBlockedAddressStatus.Completed }
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        hasLoaded = true,
                        blockedAddresses = completed,
                        selected = state.selected.intersect(completed.map { it.hashedBlockedValue }.toSet()),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                app.logger.error("Failed to list blocked addresses: $e")
                _uiState.update { it.copy(isLoading = false, listError = e.describe()) }
            }
        }
    }

    fun toggleSelection(hashedBlockedValue: String) {
        _uiState.update { state ->
            val selected =
                if (hashedBlockedValue in
                    state.selected
                ) {
                    state.selected - hashedBlockedValue
                } else {
                    state.selected + hashedBlockedValue
                }
            state.copy(selected = selected)
        }
    }

    /** Unblock the selected addresses. A partial result lists the blocklist again. */
    fun unblockSelected() {
        val hashedValues = _uiState.value.selected.toList()
        if (hashedValues.isEmpty()) return
        _uiState.update { it.copy(isUnblocking = true, unblockError = null) }
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { emailClient.unblockEmailAddressesByHashedValue(hashedValues) }
                when (result.status) {
                    BatchOperationStatus.SUCCESS ->
                        _uiState.update { state ->
                            state.copy(
                                blockedAddresses = state.blockedAddresses.filterNot { it.hashedBlockedValue in hashedValues },
                                selected = emptySet(),
                            )
                        }
                    BatchOperationStatus.PARTIAL -> {
                        _uiState.update { it.copy(selected = emptySet()) }
                        load()
                    }
                    BatchOperationStatus.FAILURE -> _uiState.update { it.copy(unblockError = app.getString(R.string.something_wrong)) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                app.logger.error("Failed to unblock addresses: $e")
                _uiState.update { it.copy(unblockError = e.describe()) }
            } finally {
                _uiState.update { it.copy(isUnblocking = false) }
            }
        }
    }

    fun dismissListError() {
        _uiState.update { it.copy(listError = null) }
    }

    fun dismissUnblockError() {
        _uiState.update { it.copy(unblockError = null) }
    }

    private fun Throwable.describe(): String = localizedMessage ?: toString()
}
