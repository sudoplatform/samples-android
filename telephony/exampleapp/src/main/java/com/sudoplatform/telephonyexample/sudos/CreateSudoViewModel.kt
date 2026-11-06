/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.sudos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
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

data class CreateSudoUiState(
    val label: String = "",
    /** True while the Sudo is being created. */
    val isBusy: Boolean = false,
    /** The user tried to create without a label. Cleared as soon as they type. */
    val showLabelRequired: Boolean = false,
    val createError: String? = null,
)

/** Emitted once the Sudo has been created, so the app can move on to it. */
data class SudoCreated(
    val sudoId: String,
)

/** Creates a Sudo with the label the user entered. */
class CreateSudoViewModel(
    private val sudoProfilesClient: SudoProfilesClient,
) : ViewModel() {
    companion object {
        val Factory =
            viewModelFactory {
                initializer { CreateSudoViewModel((this[APPLICATION_KEY] as App).sudoProfilesClient) }
            }
    }

    private val _uiState = MutableStateFlow(CreateSudoUiState())
    val uiState: StateFlow<CreateSudoUiState> = _uiState.asStateFlow()

    private val _created = Channel<SudoCreated>(Channel.BUFFERED)
    val created: Flow<SudoCreated> = _created.receiveAsFlow()

    fun updateLabel(label: String) {
        _uiState.update { it.copy(label = label, showLabelRequired = false) }
    }

    fun createSudo() {
        val label = _uiState.value.label.trim()
        if (label.isEmpty()) {
            _uiState.update { it.copy(showLabelRequired = true) }
            return
        }
        _uiState.update { it.copy(isBusy = true, createError = null) }
        viewModelScope.launch {
            try {
                val sudo = Sudo().apply { this.label = label }
                val created = withContext(Dispatchers.IO) { sudoProfilesClient.createSudo(sudo) }
                _created.send(SudoCreated(checkNotNull(created.id)))
            } catch (e: SudoProfileException) {
                _uiState.update { it.copy(createError = e.localizedMessage ?: e.toString()) }
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun dismissCreateError() {
        _uiState.update { it.copy(createError = null) }
    }
}
