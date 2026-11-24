/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import androidx.annotation.StringRes
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
import com.sudoplatform.emailexample.util.Rfc822MessageParser
import com.sudoplatform.emailexample.util.SimplifiedEmailMessage
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudoemail.types.BatchOperationResult
import com.sudoplatform.sudoemail.types.BatchOperationStatus
import com.sudoplatform.sudoemail.types.DraftEmailMessageWithContent
import com.sudoplatform.sudoemail.types.EmailFolder
import com.sudoplatform.sudoemail.types.ListAPIResult
import com.sudoplatform.sudoemail.types.ScheduledDraftMessage
import com.sudoplatform.sudoemail.types.ScheduledDraftMessageState
import com.sudoplatform.sudoemail.types.inputs.CreateCustomEmailFolderInput
import com.sudoplatform.sudoemail.types.inputs.DeleteCustomEmailFolderInput
import com.sudoplatform.sudoemail.types.inputs.DeleteDraftEmailMessagesInput
import com.sudoplatform.sudoemail.types.inputs.GetDraftEmailMessageInput
import com.sudoplatform.sudoemail.types.inputs.GetEmailAddressInput
import com.sudoplatform.sudoemail.types.inputs.ListDraftEmailMessageMetadataForEmailAddressIdInput
import com.sudoplatform.sudoemail.types.inputs.ListEmailFoldersForEmailAddressIdInput
import com.sudoplatform.sudoemail.types.inputs.ListEmailMessagesForEmailFolderIdInput
import com.sudoplatform.sudoemail.types.inputs.ListScheduledDraftMessagesForEmailAddressIdInput
import com.sudoplatform.sudoemail.types.inputs.NotEqualStateFilter
import com.sudoplatform.sudoemail.types.inputs.ScheduledDraftMessageFilterInput
import com.sudoplatform.sudoemail.types.inputs.UpdateCustomEmailFolderInput
import com.sudoplatform.sudoemail.types.inputs.UpdateEmailMessagesInput
import com.sudoplatform.sudokeymanager.KeyManagerException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The name entered in the create or rename folder dialog. */
data class FolderForm(
    /** The custom folder being renamed, or null when creating one. */
    val folder: EmailFolder?,
    val name: String,
    /** The user tried to save without a name. */
    val showNameRequired: Boolean = false,
)

data class EmailMessagesUiState(
    /** The address whose messages are shown, once loaded. */
    val emailAddress: String? = null,
    val folders: List<EmailFolder> = emptyList(),
    val selectedFolder: FolderOption? = null,
    val messages: List<MessageListItem> = emptyList(),
    val isLoading: Boolean = false,
    /** True once the selected folder has been listed, so an empty list means it is empty. */
    val hasLoaded: Boolean = false,
    /**
     * True if the address or its messages couldn't be read because this device doesn't have the
     * address's keys, e.g. it was provisioned on another device. Importing a key backup fixes this.
     */
    val needsKeyImport: Boolean = false,
    /** Whether notifications are on for this address, or null if unknown or unavailable. */
    val notificationsEnabled: Boolean? = null,
    val notificationSettingError: String? = null,
    /** A trashed message the user swiped away, awaiting confirmation before it is deleted. */
    val pendingDelete: MessageListItem? = null,
    val confirmEmptyTrash: Boolean = false,
    /** A custom folder awaiting confirmation before it is deleted. */
    val pendingFolderDelete: EmailFolder? = null,
    val folderForm: FolderForm? = null,
    /** The caption of the blocking progress dialog while an action runs. */
    @StringRes val busyResId: Int? = null,
    /** A failure to load the address, its folders or the selected folder's messages. */
    val loadError: ActionError? = null,
    val actionError: ActionError? = null,
) {
    /** The address's folders followed by its drafts. */
    val folderOptions: List<FolderOption> get() = folders.map { FolderOption.Folder(it) } + FolderOption.Drafts

    /** The selected folder, if it is a custom folder. */
    val selectedCustomFolder: EmailFolder? get() = (selectedFolder as? FolderOption.Folder)?.folder?.takeIf { it.isCustom }

    val isTrashSelected: Boolean get() = (selectedFolder as? FolderOption.Folder)?.folder?.isTrash == true
}

/**
 * Lists an email address's messages by folder, and its drafts, keeping them up to date from the
 * email message subscription. Moves messages to the trash and deletes them, empties the trash,
 * manages custom folders, and turns the address's notifications on or off.
 */
class EmailMessagesViewModel(
    private val emailAddressId: String,
    private val app: App,
) : ViewModel() {
    companion object {
        val Factory =
            viewModelFactory {
                initializer {
                    EmailMessagesViewModel(
                        emailAddressId = checkNotNull(createSavedStateHandle().get<String>(NavArgs.EMAIL_ADDRESS_ID)),
                        app = this[APPLICATION_KEY] as App,
                    )
                }
            }
    }

    private val emailClient: SudoEmailClient get() = app.sudoEmailClient

    private val _uiState = MutableStateFlow(EmailMessagesUiState())
    val uiState: StateFlow<EmailMessagesUiState> = _uiState.asStateFlow()

    private val _toasts = Channel<Int>(Channel.BUFFERED)

    /** String resources for short confirmations, such as a message being deleted. */
    val toasts: Flow<Int> = _toasts.receiveAsFlow()

    /** Repeats the action that produced [EmailMessagesUiState.actionError]. */
    private var retryAction: (() -> Unit)? = null

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            app.emailEvents.emailMessages.collect { event ->
                _uiState.update { state ->
                    state.copy(messages = applyMessageEvent(state.messages, event, state.selectedFolder, emailAddressId))
                }
            }
        }
        loadNotificationSetting()
    }

    /** Load the address and its folders, then list the selected folder. */
    fun refresh() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadError = null, needsKeyImport = false) }
        loadJob =
            viewModelScope.launch {
                // Subscribe before listing, so that a message arriving in between isn't missed.
                app.emailEvents.subscribe()
                if (_uiState.value.emailAddress == null) {
                    loadStep(R.string.get_email_address_failure) {
                        val emailAddress = withContext(Dispatchers.IO) { emailClient.getEmailAddress(GetEmailAddressInput(emailAddressId)) }
                        _uiState.update { it.copy(emailAddress = emailAddress?.emailAddress) }
                    }
                }
                val foldersLoaded =
                    loadStep(R.string.list_email_folders_failure) {
                        val folders =
                            withContext(Dispatchers.IO) {
                                emailClient.listEmailFoldersForEmailAddressId(ListEmailFoldersForEmailAddressIdInput(emailAddressId)).items
                            }
                        _uiState.update { state -> state.copy(folders = folders, selectedFolder = reselect(state.selectedFolder, folders)) }
                    }
                if (foldersLoaded) {
                    listSelectedFolder()
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
    }

    fun selectFolder(option: FolderOption) {
        if (option == _uiState.value.selectedFolder) return
        loadJob?.cancel()
        _uiState.update { it.copy(selectedFolder = option, messages = emptyList(), hasLoaded = false, isLoading = true, loadError = null) }
        loadJob = viewModelScope.launch { listSelectedFolder() }
    }

    /**
     * Remove a message the user swiped away: a draft is deleted, a trashed message is deleted
     * once the user confirms, and any other message is moved to the trash.
     */
    fun requestDelete(item: MessageListItem) {
        val state = _uiState.value
        when {
            state.selectedFolder == FolderOption.Drafts -> deleteDraft(item.id)
            state.isTrashSelected -> _uiState.update { it.copy(pendingDelete = item) }
            else -> moveToTrash(item.id)
        }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(pendingDelete = null) }
    }

    /** Permanently delete the trashed message awaiting confirmation. */
    fun confirmDelete() {
        val item = _uiState.value.pendingDelete ?: return
        _uiState.update { state -> state.copy(pendingDelete = null, messages = state.messages.filterNot { it.id == item.id }) }
        runAction(R.string.deleting_email_message, R.string.deleting_email_message_failure, reloadOnFailure = true) {
            withContext(Dispatchers.IO) { emailClient.deleteEmailMessage(item.id) }
            _toasts.send(R.string.deleted)
        }
    }

    fun requestEmptyTrash() {
        _uiState.update { it.copy(confirmEmptyTrash = true) }
    }

    fun cancelEmptyTrash() {
        _uiState.update { it.copy(confirmEmptyTrash = false) }
    }

    /** Delete the messages listed in the trash, then list it again. */
    fun confirmEmptyTrash() {
        val ids = _uiState.value.messages.map { it.id }
        _uiState.update { it.copy(confirmEmptyTrash = false) }
        if (ids.isEmpty()) return
        runAction(R.string.deleting_all_email_messages, R.string.deleting_all_email_messages_failure, reloadOnFailure = true) {
            val results = withContext(Dispatchers.IO) { deletionBatches(ids).map { emailClient.deleteEmailMessages(it) } }
            results.forEach { it.requireSuccess() }
            _toasts.send(R.string.deleted)
            reloadSelectedFolder()
        }
    }

    fun requestCreateFolder() {
        _uiState.update { it.copy(folderForm = FolderForm(folder = null, name = "")) }
    }

    fun requestRenameFolder() {
        val folder = _uiState.value.selectedCustomFolder ?: return
        _uiState.update { it.copy(folderForm = FolderForm(folder = folder, name = folder.customFolderName.orEmpty())) }
    }

    fun updateFolderName(name: String) {
        _uiState.update { state -> state.copy(folderForm = state.folderForm?.copy(name = name, showNameRequired = false)) }
    }

    fun cancelFolderForm() {
        _uiState.update { it.copy(folderForm = null) }
    }

    /** Create the custom folder, or rename the one being edited, with the entered name. */
    fun saveFolderForm() {
        val form = _uiState.value.folderForm ?: return
        val name = form.name.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(folderForm = form.copy(showNameRequired = true)) }
            return
        }
        _uiState.update { it.copy(folderForm = null) }
        val folder = form.folder
        if (folder == null) createFolder(name) else renameFolder(folder.id, name)
    }

    fun requestDeleteFolder() {
        val folder = _uiState.value.selectedCustomFolder ?: return
        _uiState.update { it.copy(pendingFolderDelete = folder) }
    }

    fun cancelDeleteFolder() {
        _uiState.update { it.copy(pendingFolderDelete = null) }
    }

    fun confirmDeleteFolder() {
        val folder = _uiState.value.pendingFolderDelete ?: return
        _uiState.update { it.copy(pendingFolderDelete = null) }
        deleteFolder(folder.id)
    }

    fun toggleNotifications() {
        val notifications = app.emailNotifications ?: return
        val enabled = _uiState.value.notificationsEnabled ?: return
        _uiState.update { it.copy(notificationsEnabled = !enabled) }
        viewModelScope.launch {
            try {
                notifications.setEnabled(emailAddressId, !enabled)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(notificationsEnabled = enabled, notificationSettingError = e.describe()) }
            }
        }
    }

    fun dismissNotificationSettingError() {
        _uiState.update { it.copy(notificationSettingError = null) }
    }

    fun retryAction() {
        retryAction?.invoke()
    }

    fun dismissActionError() {
        _uiState.update { it.copy(actionError = null) }
    }

    fun dismissLoadError() {
        _uiState.update { it.copy(loadError = null) }
    }

    private fun moveToTrash(messageId: String) {
        val trash = _uiState.value.folders.firstOrNull { it.isTrash } ?: return
        _uiState.update { state -> state.copy(messages = state.messages.filterNot { it.id == messageId }) }
        runAction(R.string.moving_email_message_to_trash, R.string.moving_email_message_failure, reloadOnFailure = true) {
            val result =
                withContext(Dispatchers.IO) {
                    emailClient.updateEmailMessages(
                        UpdateEmailMessagesInput(
                            ids = listOf(messageId),
                            values = UpdateEmailMessagesInput.UpdatableValues(folderId = trash.id),
                        ),
                    )
                }
            result.requireSuccess()
        }
    }

    private fun deleteDraft(draftId: String) {
        _uiState.update { state -> state.copy(messages = state.messages.filterNot { it.id == draftId }) }
        runAction(R.string.deleting_draft_email_message, R.string.deleting_draft_email_message_failure, reloadOnFailure = true) {
            val result =
                withContext(Dispatchers.IO) {
                    emailClient.deleteDraftEmailMessages(
                        DeleteDraftEmailMessagesInput(ids = listOf(draftId), emailAddressId = emailAddressId),
                    )
                }
            result.requireSuccess()
            _toasts.send(R.string.deleted)
        }
    }

    private fun createFolder(name: String) {
        runAction(R.string.creating_custom_folder, R.string.create_custom_folder_failure, retry = { createFolder(name) }) {
            withContext(Dispatchers.IO) {
                emailClient.createCustomEmailFolder(CreateCustomEmailFolderInput(emailAddressId = emailAddressId, customFolderName = name))
            }
            refresh()
        }
    }

    private fun renameFolder(
        folderId: String,
        name: String,
    ) {
        runAction(R.string.saving_custom_folder, R.string.save_custom_folder_failure, retry = { renameFolder(folderId, name) }) {
            withContext(Dispatchers.IO) {
                emailClient.updateCustomEmailFolder(
                    UpdateCustomEmailFolderInput(emailAddressId = emailAddressId, emailFolderId = folderId, customFolderName = name),
                )
            }
            refresh()
        }
    }

    private fun deleteFolder(folderId: String) {
        runAction(R.string.deleting_custom_folder, R.string.delete_custom_folder_failure, retry = { deleteFolder(folderId) }) {
            withContext(Dispatchers.IO) {
                emailClient.deleteCustomEmailFolder(DeleteCustomEmailFolderInput(emailFolderId = folderId, emailAddressId = emailAddressId))
            }
            refresh()
        }
    }

    private fun reloadSelectedFolder() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        loadJob = viewModelScope.launch { listSelectedFolder() }
    }

    /** The selection kept across a reload of the folders, or the inbox if it has gone. */
    private fun reselect(
        selected: FolderOption?,
        folders: List<EmailFolder>,
    ): FolderOption? {
        if (selected == FolderOption.Drafts) return selected
        val selectedId = (selected as? FolderOption.Folder)?.folder?.id
        val folder = folders.firstOrNull { it.id == selectedId } ?: defaultFolder(folders)
        return folder?.let { FolderOption.Folder(it) }
    }

    private suspend fun listSelectedFolder() {
        when (val selected = _uiState.value.selectedFolder) {
            FolderOption.Drafts -> loadStep(R.string.list_email_messages_failure) { listDrafts() }
            is FolderOption.Folder -> loadStep(R.string.list_email_messages_failure) { listFolder(selected.folder) }
            null -> {}
        }
        _uiState.update { it.copy(isLoading = false) }
    }

    private suspend fun listFolder(folder: EmailFolder) {
        val result =
            withContext(Dispatchers.IO) {
                emailClient.listEmailMessagesForEmailFolderId(ListEmailMessagesForEmailFolderIdInput(folderId = folder.id))
            }
        val emailAddress = _uiState.value.emailAddress
        when (result) {
            is ListAPIResult.Success ->
                _uiState.update {
                    it.copy(hasLoaded = true, messages = folderMessageItems(result.result.items, emailAddressId, emailAddress))
                }
            // Messages this device has no keys for are returned as partial results.
            is ListAPIResult.Partial -> {
                val causes = result.result.failed.map { it.cause }
                val needsKeyImport = causes.all { it.isMissingKey() }
                _uiState.update {
                    it.copy(
                        hasLoaded = true,
                        messages = folderMessageItems(result.result.items, emailAddressId, emailAddress),
                        needsKeyImport = it.needsKeyImport || needsKeyImport,
                        loadError =
                            if (needsKeyImport) {
                                null
                            } else {
                                ActionError(
                                    R.string.list_email_messages_failure,
                                    causes.first().describe(),
                                    true,
                                )
                            },
                    )
                }
            }
        }
    }

    /** List the drafts, rebuilding the list each time, with the send time of those scheduled. */
    private suspend fun listDrafts() {
        val items =
            withContext(Dispatchers.IO) {
                val drafts = mutableListOf<Pair<DraftEmailMessageWithContent, SimplifiedEmailMessage>>()
                var nextToken: String? = null
                do {
                    val page =
                        emailClient.listDraftEmailMessageMetadataForEmailAddressId(
                            ListDraftEmailMessageMetadataForEmailAddressIdInput(emailAddressId = emailAddressId, nextToken = nextToken),
                        )
                    page.items.forEach { metadata ->
                        val draft =
                            emailClient.getDraftEmailMessage(
                                GetDraftEmailMessageInput(id = metadata.id, emailAddressId = emailAddressId),
                            )
                        drafts += draft to Rfc822MessageParser.parseRfc822Data(draft.rfc822Data)
                    }
                    nextToken = page.nextToken
                } while (nextToken != null)
                draftItems(drafts, if (drafts.isEmpty()) emptyList() else listScheduledDrafts())
            }
        _uiState.update { it.copy(hasLoaded = true, messages = items) }
    }

    private suspend fun listScheduledDrafts(): List<ScheduledDraftMessage> {
        val scheduledDrafts = mutableListOf<ScheduledDraftMessage>()
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
            scheduledDrafts += page.items
            nextToken = page.nextToken
        } while (nextToken != null)
        return scheduledDrafts
    }

    /**
     * Run one step of loading the screen. A missing key shows the key import notice; any other
     * failure is reported under [failureResId].
     *
     * @return true if the step succeeded.
     */
    private suspend fun loadStep(
        @StringRes failureResId: Int,
        step: suspend () -> Unit,
    ): Boolean =
        try {
            step()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            app.logger.error("Loading email messages failed: $e")
            _uiState.update {
                if (e.isMissingKey()) {
                    it.copy(needsKeyImport = true)
                } else {
                    it.copy(loadError = ActionError(failureResId, e.describe(), canRetry = true))
                }
            }
            false
        }

    private fun loadNotificationSetting() {
        val notifications = app.emailNotifications ?: return
        viewModelScope.launch {
            try {
                val enabled = notifications.isEnabled(emailAddressId)
                _uiState.update { it.copy(notificationsEnabled = enabled) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                app.logger.warning("Failed to read the notification setting: ${e.describe()}")
            }
        }
    }

    /**
     * Run [action] behind a progress dialog captioned [busyResId], reporting a failure under
     * [failureResId], with an option to [retry] it if given. With [reloadOnFailure] the selected
     * folder is listed again after a failure, to restore rows removed ahead of the request.
     */
    private fun runAction(
        @StringRes busyResId: Int,
        @StringRes failureResId: Int,
        retry: (() -> Unit)? = null,
        reloadOnFailure: Boolean = false,
        action: suspend () -> Unit,
    ) {
        _uiState.update { it.copy(busyResId = busyResId, actionError = null) }
        viewModelScope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                app.logger.error("Email message action failed: $e")
                retryAction = retry
                _uiState.update { it.copy(actionError = ActionError(failureResId, e.describe(), canRetry = retry != null)) }
                if (reloadOnFailure) reloadSelectedFolder()
            } finally {
                _uiState.update { it.copy(busyResId = null) }
            }
        }
    }

    /** Throws unless every item in the batch succeeded. */
    private fun BatchOperationResult<*, *>.requireSuccess() {
        if (status != BatchOperationStatus.SUCCESS) {
            throw BatchOperationException(app.getString(R.string.batch_operation_failed))
        }
    }

    private class BatchOperationException(
        message: String,
    ) : Exception(message)

    /**
     * Whether this device lacks the keys to read the address or its messages. The key manager's
     * failure is the cause when unsealing fails.
     */
    private fun Throwable.isMissingKey(): Boolean =
        generateSequence(this) { it.cause }.any {
            it is SudoEmailClient.EmailAddressException.UnsealingException ||
                it is SudoEmailClient.EmailAddressException.PublicKeyException ||
                it is SudoEmailClient.EmailMessageException.UnsealingException ||
                it is KeyManagerException
        }

    private fun Throwable.describe(): String = localizedMessage ?: toString()
}
