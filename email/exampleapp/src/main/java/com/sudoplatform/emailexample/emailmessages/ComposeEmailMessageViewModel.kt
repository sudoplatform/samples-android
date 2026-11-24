/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import androidx.core.text.HtmlCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sudoplatform.emailexample.App
import com.sudoplatform.emailexample.NavArgs
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.emailaddresses.ActionError
import com.sudoplatform.emailexample.util.Rfc822MessageFactory
import com.sudoplatform.emailexample.util.Rfc822MessageParser
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudoemail.types.EmailAlias
import com.sudoplatform.sudoemail.types.EmailAttachment
import com.sudoplatform.sudoemail.types.EmailMessage
import com.sudoplatform.sudoemail.types.ListAPIResult
import com.sudoplatform.sudoemail.types.ScheduledDraftMessageState
import com.sudoplatform.sudoemail.types.inputs.CancelScheduledDraftMessageInput
import com.sudoplatform.sudoemail.types.inputs.CreateDraftEmailMessageInput
import com.sudoplatform.sudoemail.types.inputs.GetDraftEmailMessageInput
import com.sudoplatform.sudoemail.types.inputs.GetEmailAddressInput
import com.sudoplatform.sudoemail.types.inputs.GetEmailMessageInput
import com.sudoplatform.sudoemail.types.inputs.GetEmailMessageWithBodyInput
import com.sudoplatform.sudoemail.types.inputs.ListEmailAliasesForOwnerInput
import com.sudoplatform.sudoemail.types.inputs.ListScheduledDraftMessagesForEmailAddressIdInput
import com.sudoplatform.sudoemail.types.inputs.LookupEmailAddressesPublicInfoInput
import com.sudoplatform.sudoemail.types.inputs.NotEqualStateFilter
import com.sudoplatform.sudoemail.types.inputs.ScheduleSendDraftMessageInput
import com.sudoplatform.sudoemail.types.inputs.ScheduledDraftMessageFilterInput
import com.sudoplatform.sudoemail.types.inputs.UpdateDraftEmailMessageInput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ComposeUiState(
    val mode: ComposeMode,
    /** The sender's address and display name, used as From when not sending via an alias. */
    val sender: EmailMessage.EmailAddress? = null,
    val isLoading: Boolean = false,
    /** True once the message replied to or forwarded, or the draft, has been loaded into [content]. */
    val isMessageLoaded: Boolean = false,
    /** True when the service has email aliases enabled, so the message can be sent from one. */
    val aliasesEnabled: Boolean = false,
    /** The aliases the message can be sent from. */
    val aliases: List<EmailAlias> = emptyList(),
    /** The alias to send from, or null to send from the email address. */
    val selectedAlias: EmailAlias? = null,
    val content: MessageContent = MessageContent(),
    val attachments: List<EmailAttachment> = emptyList(),
    val isEncrypted: Boolean = false,
    /** True for a draft that is scheduled to be sent. */
    val isScheduled: Boolean = false,
    /** The user tried to send without a To address. */
    val showToRequired: Boolean = false,
    /** The user picked a send time that has already passed. */
    val showScheduleInPast: Boolean = false,
    /** The caption of the blocking progress dialog while an action runs. */
    @StringRes val busyResId: Int? = null,
    val loadError: ActionError? = null,
    val listAliasesError: String? = null,
    val encryptionLookupError: String? = null,
    val actionError: ActionError? = null,
) {
    val sendFromOptions: List<SendFromOption> get() = sendFromOptions(aliases)

    val selectedSendFromOption: SendFromOption
        get() = selectedAlias?.let { SendFromOption.Alias(it) } ?: SendFromOption.EmailAddress

    val actions: List<ComposeAction> get() = composeActions(mode, isScheduled)

    val isBusy: Boolean get() = busyResId != null

    /** Whether the message can be sent or saved: its sender and any source are loaded and nothing is running. */
    val canAct: Boolean get() = sender != null && isMessageLoaded && !isBusy
}

/** A one-shot instruction to the compose screen. */
sealed interface ComposeEvent {
    /** The message was sent, saved, scheduled or unscheduled; confirm with [messageResId] and close. */
    data class Done(
        @StringRes val messageResId: Int,
    ) : ComposeEvent

    /** Open an attachment in a viewer app. */
    data class OpenAttachment(
        val file: AttachmentFile,
    ) : ComposeEvent
}

/**
 * Composes a new message, a reply, a forward or an edit of a draft. Sends it from the email
 * address or one of its aliases, saves it as a draft, and schedules or unschedules a draft.
 * Shows whether every recipient can receive end-to-end encrypted messages.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ComposeEmailMessageViewModel(
    private val emailAddressId: String,
    private val mode: ComposeMode,
    /** The message replied to or forwarded, or the draft being edited. */
    private val messageId: String?,
    private val app: App,
) : ViewModel() {
    companion object {
        private const val FILE_PROVIDER_AUTHORITY = "com.sudoplatform.emailexample.fileprovider"
        private const val ATTACHMENTS_DIRECTORY = "attachments"

        val Factory =
            viewModelFactory {
                initializer {
                    val savedStateHandle = createSavedStateHandle()
                    ComposeEmailMessageViewModel(
                        emailAddressId = checkNotNull(savedStateHandle.get<String>(NavArgs.EMAIL_ADDRESS_ID)),
                        mode = ComposeMode.fromRouteValue(savedStateHandle.get<String>(NavArgs.MODE)),
                        messageId = savedStateHandle.get<String>(NavArgs.MESSAGE_ID),
                        app = this[APPLICATION_KEY] as App,
                    )
                }
            }
    }

    private val emailClient: SudoEmailClient get() = app.sudoEmailClient

    private val _uiState =
        MutableStateFlow(
            ComposeUiState(
                mode = mode,
                isMessageLoaded = mode == ComposeMode.NEW,
                content = if (mode == ComposeMode.NEW) MessageContent(body = app.getString(R.string.email_footer)) else MessageContent(),
            ),
        )
    val uiState: StateFlow<ComposeUiState> = _uiState.asStateFlow()

    private val _events = Channel<ComposeEvent>(Channel.BUFFERED)
    val events: Flow<ComposeEvent> = _events.receiveAsFlow()

    /** The draft's content as last saved, to tell whether it has unsaved edits. */
    private var savedDraftContent: MessageContent? = null

    /** Repeats the action that produced [ComposeUiState.actionError]. */
    private var retryAction: (() -> Unit)? = null

    private val messageReference: MessageReference =
        when (mode) {
            ComposeMode.REPLY -> MessageReference(replyingMessageId = messageId)
            ComposeMode.FORWARD -> MessageReference(forwardingMessageId = messageId)
            else -> MessageReference()
        }

    init {
        load()
        viewModelScope.launch {
            combine(
                fieldEncryption { it.to },
                fieldEncryption { it.cc },
                fieldEncryption { it.bcc },
            ) { to, cc, bcc -> showEncryptedIndicator(listOf(to, cc, bcc)) }
                .distinctUntilChanged()
                .collect { encrypted -> _uiState.update { it.copy(isEncrypted = encrypted) } }
        }
    }

    /** Load the sender, and the message replied to or forwarded, or the draft. */
    fun load() {
        _uiState.update { it.copy(loadError = null, isLoading = true) }
        viewModelScope.launch {
            try {
                if (_uiState.value.sender == null) {
                    val emailAddress =
                        withContext(Dispatchers.IO) { emailClient.getEmailAddress(GetEmailAddressInput(emailAddressId)) }
                            ?: throw NotFoundException(app.getString(R.string.get_email_address_failure))
                    _uiState.update { it.copy(sender = EmailMessage.EmailAddress(emailAddress.emailAddress, emailAddress.displayName)) }
                    loadAliases()
                }
                if (!_uiState.value.isMessageLoaded) {
                    when (mode) {
                        ComposeMode.NEW -> {}
                        ComposeMode.REPLY -> loadSourceMessage(R.string.reply_message, R.string.reply_body)
                        ComposeMode.FORWARD -> loadSourceMessage(R.string.forward_message, R.string.forward_body)
                        ComposeMode.DRAFT -> loadDraft()
                    }
                }
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                app.logger.error("Failed to load the message to compose: $e")
                val senderLoaded = _uiState.value.sender != null
                val titleResId = if (senderLoaded) R.string.read_email_message_failure else R.string.get_email_address_failure
                _uiState.update { it.copy(isLoading = false, loadError = ActionError(titleResId, e.describe(), canRetry = true)) }
            }
        }
    }

    fun updateTo(text: String) = updateContent { it.copy(to = text) }

    fun updateCc(text: String) = updateContent { it.copy(cc = text) }

    fun updateBcc(text: String) = updateContent { it.copy(bcc = text) }

    fun updateSubject(text: String) = updateContent { it.copy(subject = text) }

    fun updateBody(text: String) = updateContent { it.copy(body = text) }

    fun selectSendFrom(option: SendFromOption) {
        _uiState.update { it.copy(selectedAlias = (option as? SendFromOption.Alias)?.alias) }
    }

    /** Read a file the user picked and add it as an attachment. */
    fun addAttachment(uri: Uri) {
        viewModelScope.launch {
            try {
                val attachment =
                    withContext(Dispatchers.IO) {
                        EmailAttachment(
                            fileName = attachmentFileName(uri),
                            contentId = UUID.randomUUID().toString(),
                            mimeType = attachmentMimeType(uri),
                            inlineAttachment = false,
                            data = app.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: byteArrayOf(),
                        )
                    }
                _uiState.update { it.copy(attachments = it.attachments + attachment) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                app.logger.error("Failed to read attachment: $e")
                _uiState.update { it.copy(actionError = ActionError(R.string.add_attachment_failure, e.describe(), canRetry = false)) }
            }
        }
    }

    fun removeAttachment(attachment: EmailAttachment) {
        _uiState.update { state -> state.copy(attachments = state.attachments.filterNot { it === attachment }) }
    }

    /** Write an attachment to a temporary file and ask the screen to open it. */
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
                _events.send(ComposeEvent.OpenAttachment(AttachmentFile(uri, attachment.mimeType)))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                reportAttachmentError(e.describe())
            }
        }
    }

    /** Report a failure to open an attachment, such as no app being able to view it. */
    fun reportAttachmentError(message: String) {
        _uiState.update { it.copy(actionError = ActionError(R.string.open_attachment_failure_title, message, canRetry = false)) }
    }

    /** Send the message via the selected alias, or from the email address. */
    fun send() {
        val state = _uiState.value
        val sender = state.sender ?: return
        if (state.content.to.isBlank()) {
            _uiState.update { it.copy(showToRequired = true) }
            return
        }
        val request = buildSendRequest(emailAddressId, sender, state.selectedAlias, state.content, state.attachments, messageReference)
        runAction(R.string.sending, R.string.send_email_message_failure, retry = ::send) {
            withContext(Dispatchers.IO) {
                when (request) {
                    is SendRequest.FromEmailAddress -> emailClient.sendEmailMessage(request.input)
                    is SendRequest.ViaAlias -> emailClient.sendEmailMessageViaAlias(request.input)
                }
            }
            _events.send(ComposeEvent.Done(R.string.sent))
        }
    }

    /** Save the message as a new draft, or update the draft being edited. */
    fun saveDraft() {
        runAction(R.string.saving, R.string.save_email_message_failure, retry = ::saveDraft) {
            saveDraftContent()
            _events.send(ComposeEvent.Done(R.string.saved))
        }
    }

    /**
     * Schedule the draft to be sent at [sendAt], saving any edits first.
     *
     * @param sendAt rejected if it has already passed.
     */
    fun schedule(sendAt: Date) {
        val draftId = messageId ?: return
        if (mode != ComposeMode.DRAFT) return
        if (!sendAt.after(Date())) {
            _uiState.update { it.copy(showScheduleInPast = true) }
            return
        }
        runAction(R.string.scheduling, R.string.schedule_email_message_failure, retry = { schedule(sendAt) }) {
            if (_uiState.value.content != savedDraftContent) saveDraftContent()
            withContext(Dispatchers.IO) {
                emailClient.scheduleSendDraftMessage(
                    ScheduleSendDraftMessageInput(id = draftId, emailAddressId = emailAddressId, sendAt = sendAt),
                )
            }
            _events.send(ComposeEvent.Done(R.string.scheduled))
        }
    }

    fun cancelSchedule() {
        val draftId = messageId ?: return
        if (mode != ComposeMode.DRAFT) return
        runAction(R.string.cancel_scheduling, R.string.cancel_scheduled_message_failure, retry = ::cancelSchedule) {
            withContext(Dispatchers.IO) {
                emailClient.cancelScheduledDraftMessage(CancelScheduledDraftMessageInput(id = draftId, emailAddressId = emailAddressId))
            }
            _events.send(ComposeEvent.Done(R.string.canceled))
        }
    }

    fun retryAction() {
        retryAction?.invoke()
    }

    fun loadAliases() {
        val emailAddress = _uiState.value.sender?.emailAddress ?: return
        _uiState.update { it.copy(listAliasesError = null) }
        viewModelScope.launch {
            try {
                // If the configuration can't be read, aliases are treated as unavailable so the
                // message can still be sent from the email address.
                val aliasesEnabled =
                    try {
                        withContext(Dispatchers.IO) { app.getConfigurationData() }.emailAliasesEnabled
                    } catch (e: SudoEmailClient.EmailConfigurationException) {
                        app.logger.warning("Failed to get email configuration: $e")
                        false
                    }
                _uiState.update { it.copy(aliasesEnabled = aliasesEnabled) }
                if (!aliasesEnabled) return@launch
                val result = withContext(Dispatchers.IO) { emailClient.listEmailAliasesForOwner(ListEmailAliasesForOwnerInput()) }
                when (result) {
                    is ListAPIResult.Success ->
                        _uiState.update { it.copy(aliases = sendFromAliases(result.result.items, emailAddress)) }
                    is ListAPIResult.Partial ->
                        _uiState.update {
                            it.copy(
                                listAliasesError =
                                    result.result.failed
                                        .first()
                                        .cause
                                        .describe(),
                            )
                        }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                app.logger.error("Failed to list email aliases: $e")
                _uiState.update { it.copy(listAliasesError = e.describe()) }
            }
        }
    }

    fun dismissToRequired() {
        _uiState.update { it.copy(showToRequired = false) }
    }

    fun dismissScheduleInPast() {
        _uiState.update { it.copy(showScheduleInPast = false) }
    }

    fun dismissLoadError() {
        _uiState.update { it.copy(loadError = null) }
    }

    fun dismissListAliasesError() {
        _uiState.update { it.copy(listAliasesError = null) }
    }

    fun dismissEncryptionLookupError() {
        _uiState.update { it.copy(encryptionLookupError = null) }
    }

    fun dismissActionError() {
        _uiState.update { it.copy(actionError = null) }
    }

    private fun updateContent(change: (MessageContent) -> MessageContent) {
        _uiState.update { it.copy(content = change(it.content)) }
    }

    private suspend fun loadSourceMessage(
        @StringRes subjectTemplateResId: Int,
        @StringRes bodyTemplateResId: Int,
    ) {
        val id = checkNotNull(messageId)
        val (message, body) =
            withContext(Dispatchers.IO) {
                val message =
                    emailClient.getEmailMessage(GetEmailMessageInput(id = id))
                        ?: throw NotFoundException(app.getString(R.string.email_message_not_found))
                val body = emailClient.getEmailMessageWithBody(GetEmailMessageWithBodyInput(id, emailAddressId))
                message to body
            }
        val text =
            when {
                body == null -> ""
                body.isHtml -> HtmlCompat.fromHtml(body.body, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
                else -> body.body
            }
        val content = replyOrForwardContent(message, text, app.getString(subjectTemplateResId), app.getString(bodyTemplateResId))
        _uiState.update { it.copy(content = content, isMessageLoaded = true) }
    }

    private suspend fun loadDraft() {
        val id = checkNotNull(messageId)
        val (content, isScheduled) =
            withContext(Dispatchers.IO) {
                val draft = emailClient.getDraftEmailMessage(GetDraftEmailMessageInput(id = id, emailAddressId = emailAddressId))
                draftContent(Rfc822MessageParser.parseRfc822Data(draft.rfc822Data)) to isDraftScheduled(id)
            }
        savedDraftContent = content
        _uiState.update { it.copy(content = content, isScheduled = isScheduled, isMessageLoaded = true) }
    }

    /** Whether the draft has a schedule that hasn't been cancelled. */
    private suspend fun isDraftScheduled(draftId: String): Boolean {
        var nextToken: String? = null
        do {
            val page =
                emailClient.listScheduledDraftMessagesForEmailAddressId(
                    ListScheduledDraftMessagesForEmailAddressIdInput(
                        emailAddressId = emailAddressId,
                        nextToken = nextToken,
                        filter =
                            ScheduledDraftMessageFilterInput(
                                state = NotEqualStateFilter(notEqual = ScheduledDraftMessageState.CANCELLED),
                            ),
                    ),
                )
            if (page.items.any { it.id == draftId }) return true
            nextToken = page.nextToken
        } while (nextToken != null)
        return false
    }

    /** Save the content as a new draft, or over the draft being edited. Attachments and the alias aren't saved. */
    private suspend fun saveDraftContent() {
        val state = _uiState.value
        val from = state.sender?.emailAddress ?: throw NotFoundException(app.getString(R.string.get_email_address_failure))
        val content = state.content
        withContext(Dispatchers.IO) {
            val rfc822Data =
                Rfc822MessageFactory.makeRfc822Data(
                    from = from,
                    to = addressesToArray(content.to).map { it.emailAddress },
                    cc = addressesToArray(content.cc).map { it.emailAddress },
                    bcc = addressesToArray(content.bcc).map { it.emailAddress },
                    subject = content.subject,
                    body = content.body,
                )
            if (mode == ComposeMode.DRAFT && messageId != null) {
                emailClient.updateDraftEmailMessage(
                    UpdateDraftEmailMessageInput(id = messageId, rfc822Data = rfc822Data, senderEmailAddressId = emailAddressId),
                )
            } else {
                emailClient.createDraftEmailMessage(
                    CreateDraftEmailMessageInput(rfc822Data = rfc822Data, senderEmailAddressId = emailAddressId),
                )
            }
        }
        savedDraftContent = content
    }

    /**
     * A recipient field's encryption, looked up once the field has been left unchanged for
     * [ENCRYPTION_CHECK_DELAY_MS]. A newer edit cancels a lookup in progress.
     */
    private fun fieldEncryption(field: (MessageContent) -> String): Flow<RecipientEncryption> {
        val text = _uiState.map { field(it.content) }.distinctUntilChanged()
        val lookups =
            text
                .debounce(ENCRYPTION_CHECK_DELAY_MS)
                .mapLatest<String, Pair<String, Boolean>?> { it to (it.isNotBlank() && areAllEncrypted(it)) }
                .onStart { emit(null) }
        return combine(text, lookups) { current, lookup -> recipientEncryption(current, lookup) }
    }

    private suspend fun areAllEncrypted(addresses: String): Boolean {
        if (!isValidEmailAddressList(addresses)) return false
        val emailAddresses = addressesToArray(addresses).map { it.emailAddress }
        return try {
            val publicInfo =
                withContext(Dispatchers.IO) {
                    emailClient.lookupEmailAddressesPublicInfo(LookupEmailAddressesPublicInfoInput(emailAddresses))
                }
            allAddressesEncrypted(emailAddresses, publicInfo)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            app.logger.error("Failed to look up email address encryption: $e")
            _uiState.update { it.copy(encryptionLookupError = e.describe()) }
            false
        }
    }

    private fun attachmentFileName(uri: Uri): String {
        var name: String? = null
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) name = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            }
        }
        return name ?: uri.path?.substringAfterLast('/') ?: "unknown"
    }

    private fun attachmentMimeType(uri: Uri): String =
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            app.contentResolver.getType(uri).orEmpty()
        } else {
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase(Locale.ROOT)).orEmpty()
        }

    /**
     * Run [action] behind a progress dialog captioned [busyResId], reporting a failure under
     * [failureResId] with the option to [retry] it.
     */
    private fun runAction(
        @StringRes busyResId: Int,
        @StringRes failureResId: Int,
        retry: () -> Unit,
        action: suspend () -> Unit,
    ) {
        if (!_uiState.value.canAct) return
        _uiState.update { it.copy(busyResId = busyResId, actionError = null) }
        viewModelScope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                app.logger.error("Compose action failed: $e")
                retryAction = retry
                _uiState.update { it.copy(actionError = ActionError(failureResId, e.describe(), canRetry = true)) }
            } finally {
                _uiState.update { it.copy(busyResId = null) }
            }
        }
    }

    private class NotFoundException(
        message: String,
    ) : Exception(message)

    private fun Throwable.describe(): String = localizedMessage ?: toString()
}
