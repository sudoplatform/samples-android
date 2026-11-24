/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sudoplatform.emailexample.App
import com.sudoplatform.emailexample.NavArgs
import com.sudoplatform.emailexample.R
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudoemail.types.BatchOperationStatus
import com.sudoplatform.sudoemail.types.EmailAttachment
import com.sudoplatform.sudoemail.types.EmailMessage
import com.sudoplatform.sudoemail.types.EmailMessageWithBody
import com.sudoplatform.sudoemail.types.inputs.GetEmailMessageInput
import com.sudoplatform.sudoemail.types.inputs.GetEmailMessageWithBodyInput
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
import java.io.File

data class ReadEmailMessageUiState(
    val isLoading: Boolean = false,
    val message: EmailMessage? = null,
    /** The body and attachments, loaded with the message. */
    val body: EmailMessageWithBody? = null,
    val loadError: String? = null,
    val isBlocking: Boolean = false,
    /** The sender has been blocked; the screen confirms, then closes. */
    val senderBlocked: Boolean = false,
    val blockError: String? = null,
    val attachmentError: String? = null,
)

/** An attachment written to a file the viewer app is given access to. */
data class AttachmentFile(
    val uri: Uri,
    val mimeType: String,
)

/** Loads an email message with its body, opens its attachments and blocks its sender. */
class ReadEmailMessageViewModel(
    private val emailAddressId: String,
    private val messageId: String,
    private val app: App,
) : ViewModel() {
    companion object {
        private const val FILE_PROVIDER_AUTHORITY = "com.sudoplatform.emailexample.fileprovider"
        private const val ATTACHMENTS_DIRECTORY = "attachments"

        val Factory =
            viewModelFactory {
                initializer {
                    val savedStateHandle = createSavedStateHandle()
                    ReadEmailMessageViewModel(
                        emailAddressId = checkNotNull(savedStateHandle.get<String>(NavArgs.EMAIL_ADDRESS_ID)),
                        messageId = checkNotNull(savedStateHandle.get<String>(NavArgs.MESSAGE_ID)),
                        app = this[APPLICATION_KEY] as App,
                    )
                }
            }
    }

    private val emailClient: SudoEmailClient get() = app.sudoEmailClient

    private val _uiState = MutableStateFlow(ReadEmailMessageUiState())
    val uiState: StateFlow<ReadEmailMessageUiState> = _uiState.asStateFlow()

    private val _attachments = Channel<AttachmentFile>(Channel.BUFFERED)

    /** Attachments ready to open in a viewer app. */
    val attachments: Flow<AttachmentFile> = _attachments.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            try {
                val (message, body) =
                    withContext(Dispatchers.IO) {
                        val message = emailClient.getEmailMessage(GetEmailMessageInput(id = messageId))
                        val body =
                            message?.let {
                                emailClient.getEmailMessageWithBody(
                                    GetEmailMessageWithBodyInput(messageId, emailAddressId),
                                )
                            }
                        message to body
                    }
                _uiState.update {
                    if (message == null) {
                        it.copy(isLoading = false, loadError = app.getString(R.string.email_message_not_found))
                    } else {
                        it.copy(isLoading = false, message = message, body = body)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                app.logger.error("Failed to read email message: $e")
                _uiState.update { it.copy(isLoading = false, loadError = e.describe()) }
            }
        }
    }

    /** Write an attachment to a temporary file and hand it to [attachments] to open. */
    fun openAttachment(attachment: EmailAttachment) {
        viewModelScope.launch {
            try {
                val file =
                    withContext(Dispatchers.IO) {
                        val directory = File(app.cacheDir, ATTACHMENTS_DIRECTORY).apply { mkdirs() }
                        val name = attachment.fileName.substringAfterLast('/').ifBlank { ATTACHMENTS_DIRECTORY }
                        File(directory, name).apply { writeBytes(attachment.data) }
                    }
                val uri = FileProvider.getUriForFile(app, FILE_PROVIDER_AUTHORITY, file)
                _attachments.send(AttachmentFile(uri, attachment.mimeType))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(attachmentError = e.describe()) }
            }
        }
    }

    /** Block the sender's address, so the service drops further messages from it. */
    fun blockSender() {
        val sender =
            _uiState.value.message
                ?.from
                ?.firstOrNull()
                ?.emailAddress ?: return
        _uiState.update { it.copy(isBlocking = true, blockError = null) }
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { emailClient.blockEmailAddresses(listOf(sender)) }
                _uiState.update {
                    if (result.status == BatchOperationStatus.SUCCESS) {
                        it.copy(senderBlocked = true)
                    } else {
                        it.copy(blockError = app.getString(R.string.something_wrong))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                app.logger.error("Failed to block address: $e")
                _uiState.update { it.copy(blockError = e.describe()) }
            } finally {
                _uiState.update { it.copy(isBlocking = false) }
            }
        }
    }

    fun dismissLoadError() {
        _uiState.update { it.copy(loadError = null) }
    }

    fun dismissBlockError() {
        _uiState.update { it.copy(blockError = null) }
    }

    fun dismissAttachmentError() {
        _uiState.update { it.copy(attachmentError = null) }
    }

    /** Report a failure to open an attachment, such as no app being able to view it. */
    fun reportAttachmentError(message: String) {
        _uiState.update { it.copy(attachmentError = message) }
    }

    private fun Throwable.describe(): String = localizedMessage ?: toString()
}
