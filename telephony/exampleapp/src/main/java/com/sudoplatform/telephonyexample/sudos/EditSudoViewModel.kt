/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.sudos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import com.sudoplatform.sudoprofiles.exceptions.SudoProfileException
import com.sudoplatform.telephonyexample.App
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

data class EditSudoUiState(
    val label: String = "",
    /** True while the Sudo is being fetched. */
    val isLoading: Boolean = true,
    /** True while the Sudo is being saved. */
    val isBusy: Boolean = false,
    /** The user tried to save without a label. Cleared as soon as they type. */
    val showLabelRequired: Boolean = false,
    val saveError: String? = null,
    /** Set when the Sudo couldn't be loaded, so there is nothing to edit. */
    val loadError: String? = null,
)

/** Emitted once the Sudo has been saved, so the screen can close. */
data object SudoSaved

/** Changes the label of an existing Sudo. */
class EditSudoViewModel(
    private val sudoId: String,
    private val sudoProfilesClient: SudoProfilesClient,
) : ViewModel() {
    companion object {
        const val SUDO_ID_ARG = "sudoId"

        val Factory =
            viewModelFactory {
                initializer {
                    val sudoId = checkNotNull(createSavedStateHandle().get<String>(SUDO_ID_ARG))
                    EditSudoViewModel(sudoId, (this[APPLICATION_KEY] as App).sudoProfilesClient)
                }
            }
    }

    private val _uiState = MutableStateFlow(EditSudoUiState())
    val uiState: StateFlow<EditSudoUiState> = _uiState.asStateFlow()

    private val _saved = Channel<SudoSaved>(Channel.BUFFERED)
    val saved: Flow<SudoSaved> = _saved.receiveAsFlow()

    /** The Sudo as last loaded; updates must carry its version. */
    private var sudo: Sudo? = null

    init {
        loadSudo()
    }

    fun updateLabel(label: String) {
        _uiState.update { it.copy(label = label, showLabelRequired = false) }
    }

    fun save() {
        val original = sudo ?: return
        val label = _uiState.value.label.trim()
        if (label.isEmpty()) {
            _uiState.update { it.copy(showLabelRequired = true) }
            return
        }
        _uiState.update { it.copy(isBusy = true, saveError = null) }
        viewModelScope.launch {
            try {
                // Sudo claims are mutable, so update a copy and leave the loaded Sudo intact if it fails.
                val updated = original.copy(claims = original.claims.toMutableMap()).apply { this.label = label }
                sudo = withContext(Dispatchers.IO) { sudoProfilesClient.updateSudo(updated) }
                _saved.send(SudoSaved)
            } catch (e: SudoProfileException) {
                _uiState.update { it.copy(saveError = e.localizedMessage ?: e.toString()) }
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun dismissSaveError() {
        _uiState.update { it.copy(saveError = null) }
    }

    private fun loadSudo() {
        viewModelScope.launch {
            try {
                val found = withContext(Dispatchers.IO) { sudoProfilesClient.findSudo(sudoId) }
                sudo = found
                _uiState.update { it.copy(label = found.label.orEmpty(), isLoading = false) }
            } catch (e: SudoProfileException) {
                _uiState.update { it.copy(isLoading = false, loadError = e.localizedMessage ?: e.toString()) }
            }
        }
    }
}
