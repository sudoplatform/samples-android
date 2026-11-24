/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.ui.TestTags
import com.sudoplatform.emailexample.ui.components.DropdownField
import com.sudoplatform.emailexample.ui.components.FormDialog
import com.sudoplatform.emailexample.ui.components.LoadingDialog
import com.sudoplatform.emailexample.ui.components.MessageDialog
import com.sudoplatform.emailexample.ui.components.RetryDialog
import com.sudoplatform.emailexample.ui.components.SampleTopAppBar
import com.sudoplatform.emailexample.ui.components.SwipeToDeleteRow
import com.sudoplatform.emailexample.ui.components.ToolbarTextAction
import com.sudoplatform.emailexample.ui.components.whiteOutlinedTextFieldColors
import com.sudoplatform.emailexample.ui.theme.EmailExampleTheme
import com.sudoplatform.emailexample.ui.theme.PrimaryText
import com.sudoplatform.emailexample.ui.theme.SecondaryText
import com.sudoplatform.emailexample.ui.theme.WarningIcon
import com.sudoplatform.sudoemail.types.EmailFolder
import java.text.DateFormat
import java.util.Date

/**
 * Lists an email address's messages, one folder at a time, and its drafts, and mutes or unmutes
 * notifications for the address.
 *
 * Links From:
 *  - Email Addresses: the user taps an email address.
 *  - A tapped notification, under the message it opens.
 *
 * Links To:
 *  - Read Email Message: the user taps a message.
 *  - Compose: the user taps the compose button, or a draft.
 *  - Blocked Addresses: the user chooses "Blocked Addresses" from the menu.
 *  - Key Backup: the user taps "Import/Export Keys" in the notice shown when this device doesn't
 *    have the address's keys.
 */
@Composable
fun EmailMessagesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMessage: (messageId: String) -> Unit,
    onNavigateToCompose: () -> Unit,
    onNavigateToDraft: (draftId: String) -> Unit,
    onNavigateToBlocklist: () -> Unit,
    onNavigateToKeyBackup: () -> Unit,
    viewModel: EmailMessagesViewModel = viewModel(factory = EmailMessagesViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.toasts.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    // Refresh on every return to the screen, to pick up anything the subscription missed.
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    EmailMessagesContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onRefresh = viewModel::refresh,
        onSelectFolder = viewModel::selectFolder,
        onOpenMessage = { item ->
            if (uiState.selectedFolder == FolderOption.Drafts) onNavigateToDraft(item.id) else onNavigateToMessage(item.id)
        },
        onDeleteMessage = viewModel::requestDelete,
        onCompose = onNavigateToCompose,
        onToggleNotifications = viewModel::toggleNotifications,
        onOpenBlocklist = onNavigateToBlocklist,
        onCreateFolder = viewModel::requestCreateFolder,
        onRenameFolder = viewModel::requestRenameFolder,
        onDeleteFolder = viewModel::requestDeleteFolder,
        onEmptyTrash = viewModel::requestEmptyTrash,
        onOpenKeyBackup = onNavigateToKeyBackup,
    )

    if (uiState.pendingDelete != null) {
        MessageDialog(
            title = stringResource(R.string.delete_email_message_title),
            message = stringResource(R.string.delete_email_message_confirmation),
            confirmText = stringResource(R.string.delete),
            onConfirm = viewModel::confirmDelete,
            dismissText = stringResource(R.string.cancel),
            onDismissRequest = viewModel::cancelDelete,
        )
    }
    if (uiState.confirmEmptyTrash) {
        MessageDialog(
            title = stringResource(R.string.empty_trash_title),
            message = stringResource(R.string.empty_trash_confirmation),
            confirmText = stringResource(R.string.delete),
            onConfirm = viewModel::confirmEmptyTrash,
            dismissText = stringResource(R.string.cancel),
            onDismissRequest = viewModel::cancelEmptyTrash,
        )
    }
    uiState.pendingFolderDelete?.let { folder ->
        MessageDialog(
            title = stringResource(R.string.confirm_delete_custom_folder_title),
            message = stringResource(R.string.confirm_delete_custom_folder, folder.displayName()),
            confirmText = stringResource(R.string.delete),
            onConfirm = viewModel::confirmDeleteFolder,
            dismissText = stringResource(R.string.cancel),
            onDismissRequest = viewModel::cancelDeleteFolder,
        )
    }
    uiState.folderForm?.let { form ->
        FolderNameDialog(
            form = form,
            onNameChange = viewModel::updateFolderName,
            onSave = viewModel::saveFolderForm,
            onCancel = viewModel::cancelFolderForm,
        )
    }
    uiState.busyResId?.let { LoadingDialog(stringResource(it)) }

    uiState.notificationSettingError?.let { message ->
        MessageDialog(
            title = stringResource(R.string.notification_setting_failure),
            message = message,
            confirmText = stringResource(R.string.ok),
            onConfirm = viewModel::dismissNotificationSettingError,
            onDismissRequest = viewModel::dismissNotificationSettingError,
        )
    }
    uiState.loadError?.let { error ->
        RetryDialog(
            title = stringResource(error.titleResId),
            message = error.message,
            onRetry = viewModel::refresh,
            onDismiss = viewModel::dismissLoadError,
        )
    }
    uiState.actionError?.let { error ->
        if (error.canRetry) {
            RetryDialog(
                title = stringResource(error.titleResId),
                message = error.message,
                onRetry = viewModel::retryAction,
                onDismiss = viewModel::dismissActionError,
            )
        } else {
            MessageDialog(
                title = stringResource(error.titleResId),
                message = error.message,
                confirmText = stringResource(R.string.ok),
                onConfirm = viewModel::dismissActionError,
                onDismissRequest = viewModel::dismissActionError,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmailMessagesContent(
    uiState: EmailMessagesUiState,
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit,
    onSelectFolder: (FolderOption) -> Unit,
    onOpenMessage: (MessageListItem) -> Unit,
    onDeleteMessage: (MessageListItem) -> Unit,
    onCompose: () -> Unit,
    onToggleNotifications: () -> Unit,
    onOpenBlocklist: () -> Unit,
    onCreateFolder: () -> Unit,
    onRenameFolder: () -> Unit,
    onDeleteFolder: () -> Unit,
    onEmptyTrash: () -> Unit,
    onOpenKeyBackup: () -> Unit,
) {
    Scaffold(
        topBar = {
            SampleTopAppBar(
                title = uiState.emailAddress ?: stringResource(R.string.email_messages),
                onNavigateBack = onNavigateBack,
                actions = {
                    uiState.notificationsEnabled?.let { enabled ->
                        ToolbarTextAction(
                            text = stringResource(if (enabled) R.string.mute else R.string.unmute),
                            onClick = onToggleNotifications,
                            testTag = TestTags.MUTE_ACTION,
                        )
                    }
                    OverflowMenu(
                        customFolder = uiState.selectedCustomFolder,
                        isTrashSelected = uiState.isTrashSelected,
                        onOpenBlocklist = onOpenBlocklist,
                        onCreateFolder = onCreateFolder,
                        onRenameFolder = onRenameFolder,
                        onDeleteFolder = onDeleteFolder,
                        onEmptyTrash = onEmptyTrash,
                    )
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCompose,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = Color.White,
                modifier = Modifier.testTag(TestTags.COMPOSE_BUTTON),
            ) {
                Icon(
                    painterResource(R.drawable.ic_baseline_compose_24px),
                    contentDescription = stringResource(R.string.compose_email_message),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            DropdownField(
                label = stringResource(R.string.folder),
                options = uiState.folderOptions,
                selected = uiState.selectedFolder,
                optionText = { it.label() },
                onSelected = onSelectFolder,
                enabled = uiState.folders.isNotEmpty(),
                colors = whiteOutlinedTextFieldColors(),
                testTag = TestTags.FOLDER_DROPDOWN,
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 16.dp, end = 24.dp),
            )
            PullToRefreshBox(
                isRefreshing = uiState.isLoading && uiState.messages.isEmpty(),
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).testTag(TestTags.EMAIL_MESSAGES_LIST)) {
                    item { Box(modifier = Modifier.padding(top = 16.dp)) }
                    if (uiState.needsKeyImport) {
                        item { KeyImportNotice(onOpenKeyBackup = onOpenKeyBackup) }
                    }
                    items(uiState.messages, key = { it.id }) { item ->
                        SwipeToDeleteRow(onDelete = { onDeleteMessage(item) }, modifier = Modifier.testTag(TestTags.EMAIL_MESSAGE_ROW)) {
                            EmailMessageRow(item = item, onClick = { onOpenMessage(item) })
                        }
                    }
                    if (uiState.hasLoaded && !uiState.isLoading && uiState.messages.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.no_email_messages),
                                color = SecondaryText,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth().padding(12.dp).testTag(TestTags.EMPTY_MESSAGES_TEXT),
                            )
                        }
                    }
                    // Keeps the last row clear of the compose button.
                    item { Box(modifier = Modifier.padding(bottom = 88.dp)) }
                }
            }
        }
    }
}

@Composable
private fun FolderOption.label(): String =
    when (this) {
        FolderOption.Drafts -> stringResource(R.string.drafts_folder)
        is FolderOption.Folder -> folder.displayName()
    }

/**
 * Blocked Addresses and Create Custom Folder, then Edit and Delete Custom Folder for a custom
 * folder, or Empty Trash for the trash.
 */
@Composable
private fun OverflowMenu(
    customFolder: EmailFolder?,
    isTrashSelected: Boolean,
    onOpenBlocklist: () -> Unit,
    onCreateFolder: () -> Unit,
    onRenameFolder: () -> Unit,
    onDeleteFolder: () -> Unit,
    onEmptyTrash: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.testTag(TestTags.MORE_ACTIONS)) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = Color.White) {
            val close = { expanded = false }
            OverflowMenuItem(stringResource(R.string.blocked_addresses), TestTags.BLOCKLIST_MENU_ITEM, close, onOpenBlocklist)
            OverflowMenuItem(stringResource(R.string.create_custom_folder), TestTags.CREATE_FOLDER_MENU_ITEM, close, onCreateFolder)
            if (customFolder != null) {
                OverflowMenuItem(stringResource(R.string.edit_custom_folder), TestTags.EDIT_FOLDER_MENU_ITEM, close, onRenameFolder)
                OverflowMenuItem(stringResource(R.string.delete_custom_folder), TestTags.DELETE_FOLDER_MENU_ITEM, close, onDeleteFolder)
            }
            if (isTrashSelected) {
                OverflowMenuItem(stringResource(R.string.empty_trash), TestTags.EMPTY_TRASH_MENU_ITEM, close, onEmptyTrash)
            }
        }
    }
}

/** A menu item that closes the menu, then runs [onClick]. */
@Composable
private fun OverflowMenuItem(
    text: String,
    testTag: String,
    onClose: () -> Unit,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(text, style = MaterialTheme.typography.bodyLarge) },
        onClick = {
            onClose()
            onClick()
        },
        modifier = Modifier.testTag(testTag),
    )
}

/**
 * A message with 🔒 if it was end-to-end encrypted, an attachment icon if it has attachments, who
 * it is from or to, its subject and date, and when it will be sent if it is a scheduled draft.
 */
@Composable
private fun EmailMessageRow(
    item: MessageListItem,
    onClick: () -> Unit,
) {
    val encryptionIndicator = if (item.isEncrypted) "🔒 " else ""
    val correspondent =
        if (item.isInbound) {
            stringResource(R.string.from_recipient, item.from, encryptionIndicator)
        } else {
            stringResource(R.string.to_recipient, item.to.joinToString(), encryptionIndicator)
        }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .padding(bottom = 2.dp)
                .fillMaxWidth()
                .background(Color.White)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (item.hasAttachments) {
            Icon(
                painterResource(R.drawable.ic_baseline_attachment_24px),
                contentDescription = stringResource(R.string.has_attachments),
                tint = SecondaryText,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                correspondent,
                color = PrimaryText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                item.subject ?: stringResource(R.string.no_subject),
                color = SecondaryText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag(TestTags.EMAIL_MESSAGE_SUBJECT),
            )
            item.sendAt?.let { sendAt ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(R.drawable.ic_baseline_access_time_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        stringResource(
                            R.string.scheduled_at,
                            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(sendAt),
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp).testTag(TestTags.SCHEDULED_SEND_TEXT),
                    )
                }
            }
        }
        Text(
            DateFormat.getDateInstance(DateFormat.SHORT).format(item.date),
            color = SecondaryText,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Explains that this device can't read the address's messages until the user imports their keys. */
@Composable
private fun KeyImportNotice(onOpenKeyBackup: () -> Unit) {
    Column(
        modifier =
            Modifier
                .padding(bottom = 2.dp)
                .fillMaxWidth()
                .background(Color.White)
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp)
                .testTag(TestTags.KEY_IMPORT_NOTICE),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = WarningIcon)
            Text(
                stringResource(R.string.messages_need_key_import),
                color = PrimaryText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
        TextButton(
            onClick = onOpenKeyBackup,
            modifier = Modifier.align(Alignment.End).testTag(TestTags.OPEN_KEY_BACKUP_BUTTON),
        ) {
            Text(stringResource(R.string.key_backup).uppercase(), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Asks for the name of a new custom folder, or a new name for an existing one. */
@Composable
private fun FolderNameDialog(
    form: FolderForm,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val isCreating = form.folder == null
    FormDialog(
        title = stringResource(if (isCreating) R.string.create_custom_folder else R.string.edit_custom_folder),
        confirmText = stringResource(if (isCreating) R.string.create else R.string.save),
        onConfirm = onSave,
        onDismiss = onCancel,
    ) {
        OutlinedTextField(
            value = form.name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.enter_custom_folder_name_label)) },
            singleLine = true,
            isError = form.showNameRequired,
            supportingText =
                if (form.showNameRequired) {
                    { Text(stringResource(R.string.enter_custom_folder_name)) }
                } else {
                    null
                },
            modifier = Modifier.fillMaxWidth().testTag(TestTags.FOLDER_NAME_FIELD),
        )
    }
}

private fun previewFolder(
    id: String,
    folderName: String,
    customFolderName: String? = null,
) = EmailFolder(
    id = id,
    owner = "owner",
    owners = emptyList(),
    emailAddressId = "address",
    folderName = folderName,
    size = 0.0,
    unseenCount = 0,
    version = 1,
    createdAt = Date(),
    updatedAt = Date(),
    customFolderName = customFolderName,
)

private val previewFolders =
    listOf(
        previewFolder("inbox", StandardFolderNames.INBOX),
        previewFolder("sent", StandardFolderNames.SENT),
        previewFolder("trash", StandardFolderNames.TRASH),
        previewFolder("custom", "CUSTOM", "Receipts"),
    )

@Composable
private fun EmailMessagesContentPreview(uiState: EmailMessagesUiState) {
    EmailExampleTheme {
        EmailMessagesContent(
            uiState = uiState,
            onNavigateBack = {},
            onRefresh = {},
            onSelectFolder = {},
            onOpenMessage = {},
            onDeleteMessage = {},
            onCompose = {},
            onToggleNotifications = {},
            onOpenBlocklist = {},
            onCreateFolder = {},
            onRenameFolder = {},
            onDeleteFolder = {},
            onEmptyTrash = {},
            onOpenKeyBackup = {},
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun EmailMessagesContentInboxPreview() {
    EmailMessagesContentPreview(
        EmailMessagesUiState(
            emailAddress = "shopping@example.com",
            folders = previewFolders,
            selectedFolder = FolderOption.Folder(previewFolders.first()),
            hasLoaded = true,
            notificationsEnabled = true,
            messages =
                listOf(
                    MessageListItem(
                        id = "1",
                        isInbound = true,
                        from = "\"Store\" <orders@store.example.com>",
                        to = listOf("shopping@example.com"),
                        subject = "Your order has shipped",
                        date = Date(),
                        isEncrypted = false,
                        hasAttachments = true,
                    ),
                    MessageListItem(
                        id = "2",
                        isInbound = true,
                        from = "friend@example.com",
                        to = listOf("shopping@example.com"),
                        subject = null,
                        date = Date(),
                        isEncrypted = true,
                        hasAttachments = false,
                    ),
                ),
        ),
    )
}

@Preview(showSystemUi = true)
@Composable
private fun EmailMessagesContentDraftsPreview() {
    EmailMessagesContentPreview(
        EmailMessagesUiState(
            emailAddress = "shopping@example.com",
            folders = previewFolders,
            selectedFolder = FolderOption.Drafts,
            hasLoaded = true,
            needsKeyImport = true,
            messages =
                listOf(
                    MessageListItem(
                        id = "3",
                        isInbound = false,
                        from = "shopping@example.com",
                        to = listOf("friend@example.com"),
                        subject = "See you soon",
                        date = Date(),
                        isEncrypted = false,
                        hasAttachments = false,
                        sendAt = Date(),
                    ),
                ),
        ),
    )
}
