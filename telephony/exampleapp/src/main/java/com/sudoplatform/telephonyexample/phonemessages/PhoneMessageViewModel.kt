/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.phonemessages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudotelephony.SudoTelephonyClient
import com.sudoplatform.sudotelephony.SudoTelephonyClient.PhoneMessageException
import com.sudoplatform.sudotelephony.types.PhoneMessage
import com.sudoplatform.sudotelephony.types.inputs.DeletePhoneMessagesInput
import com.sudoplatform.sudotelephony.types.inputs.UpdatePhoneMessagesInput
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

data class PhoneMessageUiState(
    val isLoading: Boolean = true,
    val message: PhoneMessage? = null,
    /** True while the user is asked to confirm deleting the message. */
    val confirmDelete: Boolean = false,
    val isDeleting: Boolean = false,
    val loadError: String? = null,
    val deleteFailure: DeleteFailure? = null,
)

/** Emitted when the screen should close: the message was deleted or doesn't exist. */
data object PhoneMessageClosed

/** Shows one message, marks it as read, and deletes it on request. */
class PhoneMessageViewModel(
    private val messageId: String,
    private val telephonyClient: SudoTelephonyClient,
    private val logger: Logger,
) : ViewModel() {
    companion object {
        const val MESSAGE_ID_ARG = "messageId"

        val Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as App
                    PhoneMessageViewModel(
                        messageId = checkNotNull(createSavedStateHandle().get<String>(MESSAGE_ID_ARG)),
                        telephonyClient = app.sudoTelephonyClient,
                        logger = app.logger,
                    )
                }
            }
    }

    private val _uiState = MutableStateFlow(PhoneMessageUiState())
    val uiState: StateFlow<PhoneMessageUiState> = _uiState.asStateFlow()

    private val _closed = Channel<PhoneMessageClosed>(Channel.BUFFERED)
    val closed: Flow<PhoneMessageClosed> = _closed.receiveAsFlow()

    init {
        load()
    }

    /** getPhoneMessage unseals the message's text and sender with the number's key on this device. */
    fun load() {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            try {
                val message = withContext(Dispatchers.IO) { telephonyClient.getPhoneMessage(messageId) }
                if (message == null) {
                    _closed.send(PhoneMessageClosed)
                    return@launch
                }
                _uiState.update { it.copy(isLoading = false, message = message) }
                if (!message.seen) markSeen()
            } catch (e: PhoneMessageException) {
                _uiState.update { it.copy(isLoading = false, loadError = e.describe()) }
            }
        }
    }

    fun requestDelete() {
        _uiState.update { it.copy(confirmDelete = true) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(confirmDelete = false) }
    }

    fun confirmDelete() {
        _uiState.update { it.copy(confirmDelete = false, isDeleting = true, deleteFailure = null) }
        viewModelScope.launch {
            try {
                val result =
                    withContext(Dispatchers.IO) { telephonyClient.deletePhoneMessages(DeletePhoneMessagesInput(ids = listOf(messageId))) }
                if (messageId in result.succeededIds) {
                    _closed.send(PhoneMessageClosed)
                } else {
                    _uiState.update { it.copy(isDeleting = false, deleteFailure = DeleteFailure()) }
                }
            } catch (e: PhoneMessageException) {
                _uiState.update { it.copy(isDeleting = false, deleteFailure = DeleteFailure(e.describe())) }
            }
        }
    }

    fun dismissLoadError() {
        _uiState.update { it.copy(loadError = null) }
    }

    fun dismissDeleteFailure() {
        _uiState.update { it.copy(deleteFailure = null) }
    }

    /**
     * Reading a message marks it as read. The read state is stored by the service, so it is the
     * same on every device. A failure only means the message stays unread, so it is logged.
     */
    private suspend fun markSeen() {
        try {
            withContext(
                Dispatchers.IO,
            ) { telephonyClient.updatePhoneMessages(UpdatePhoneMessagesInput(ids = listOf(messageId), seen = true)) }
            _uiState.update { state -> state.copy(message = state.message?.copy(seen = true)) }
        } catch (e: PhoneMessageException) {
            logger.warning("Failed to mark message $messageId as read: ${e.describe()}")
        }
    }

    private fun Throwable.describe(): String = localizedMessage ?: toString()
}
