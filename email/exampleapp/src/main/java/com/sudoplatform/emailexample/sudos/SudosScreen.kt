/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.sudos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.ui.TestTags
import com.sudoplatform.emailexample.ui.components.ItemCell
import com.sudoplatform.emailexample.ui.components.LoadingDialog
import com.sudoplatform.emailexample.ui.components.MenuButton
import com.sudoplatform.emailexample.ui.components.MessageDialog
import com.sudoplatform.emailexample.ui.components.RetryDialog
import com.sudoplatform.emailexample.ui.components.SampleTopAppBar
import com.sudoplatform.emailexample.ui.components.SectionFooter
import com.sudoplatform.emailexample.ui.components.SectionHeader
import com.sudoplatform.emailexample.ui.components.SwipeToDeleteRow
import com.sudoplatform.emailexample.ui.components.ToolbarIconAction
import com.sudoplatform.emailexample.ui.theme.EmailExampleTheme
import com.sudoplatform.emailexample.ui.theme.NegativeMessage
import com.sudoplatform.emailexample.ui.theme.PrimaryText
import com.sudoplatform.emailexample.ui.theme.SecondaryText
import com.sudoplatform.emailexample.ui.theme.WarningIcon
import com.sudoplatform.sudoprofiles.Sudo
import java.util.Date

/**
 * The home screen, listing the user's Sudos, then their account. Email addresses belong to a
 * Sudo, so one must exist first.
 *
 * Links From:
 *  - Register: a user successfully registers or signs in.
 *
 * Links To:
 *  - Email Addresses: the user taps a Sudo.
 *  - Create Sudo: the user taps "+ Create Sudo".
 *  - Edit Sudo: the user taps the edit icon on a Sudo.
 *  - Entitlements: the user taps "Entitlements".
 *  - User Details: the user taps "Signed in with".
 *  - Key Backup: the user taps "Import/Export Keys" after signing in with Federated Sign In, or
 *    "Export Keys First" before removing their data.
 *  - Register: after deregistering or signing out.
 */
@Composable
fun SudosScreen(
    onSignedOut: () -> Unit,
    onNavigateToEmailAddresses: (sudoId: String) -> Unit,
    onNavigateToCreateSudo: () -> Unit,
    onNavigateToEditSudo: (sudoId: String) -> Unit,
    onNavigateToEntitlements: () -> Unit,
    onNavigateToUserDetails: () -> Unit,
    onNavigateToKeyBackup: () -> Unit,
    viewModel: SudosViewModel = viewModel(factory = SudosViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showInfo by rememberSaveable { mutableStateOf(false) }
    var showDeregisterConfirmation by rememberSaveable { mutableStateOf(false) }
    var showRemoveDataConfirmation by rememberSaveable { mutableStateOf(false) }
    var showKeyBackupNeedsFsso by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val docUrl = stringResource(R.string.email_doc_url)

    LaunchedEffect(viewModel) {
        viewModel.signedOut.collect { onSignedOut() }
    }

    // Refresh on every return to the screen so Sudos created or edited elsewhere show up.
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    SudosContent(
        uiState = uiState,
        onInfo = { showInfo = true },
        onRefresh = viewModel::refresh,
        onOpenSudo = { sudo -> sudo.id?.let(onNavigateToEmailAddresses) },
        onCreateSudo = onNavigateToCreateSudo,
        onEditSudo = { sudo -> sudo.id?.let(onNavigateToEditSudo) },
        onDeleteSudo = viewModel::requestDelete,
        onEntitlements = onNavigateToEntitlements,
        onUserDetails = onNavigateToUserDetails,
        onKeyBackup = {
            if (uiState.usedFsso) onNavigateToKeyBackup() else showKeyBackupNeedsFsso = true
        },
        onDeregister = { showDeregisterConfirmation = true },
        onSignOut = { viewModel.signOutFederated(removeData = false) },
        onSignOutAndRemoveData = { showRemoveDataConfirmation = true },
    )

    uiState.pendingDelete?.let { sudo ->
        MessageDialog(
            title = stringResource(R.string.delete_sudo_title),
            message = stringResource(R.string.delete_sudo_confirmation, sudo.displayLabel),
            confirmText = stringResource(R.string.delete),
            onConfirm = viewModel::confirmDelete,
            dismissText = stringResource(R.string.cancel),
            onDismissRequest = viewModel::cancelDelete,
        )
    }

    uiState.listError?.let { message ->
        RetryDialog(
            title = stringResource(R.string.list_sudos_failure),
            message = message,
            onRetry = viewModel::refresh,
            onDismiss = viewModel::dismissListError,
        )
    }
    uiState.deleteError?.let { message ->
        MessageDialog(
            title = stringResource(R.string.delete_sudo_failure),
            message = message,
            confirmText = stringResource(R.string.ok),
            onConfirm = viewModel::dismissDeleteError,
            onDismissRequest = viewModel::dismissDeleteError,
        )
    }
    if (uiState.isDeleting) {
        LoadingDialog(stringResource(R.string.deleting_sudos))
    }
    if (showDeregisterConfirmation) {
        MessageDialog(
            title = stringResource(R.string.deregister),
            message = stringResource(R.string.deregister_confirmation),
            confirmText = stringResource(R.string.deregister),
            onConfirm = {
                showDeregisterConfirmation = false
                viewModel.deregister()
            },
            dismissText = stringResource(R.string.cancel),
            onDismissRequest = { showDeregisterConfirmation = false },
        )
    }
    if (showRemoveDataConfirmation) {
        RemoveDataDialog(
            onExportKeysFirst = {
                showRemoveDataConfirmation = false
                onNavigateToKeyBackup()
            },
            onConfirm = {
                showRemoveDataConfirmation = false
                viewModel.signOutFederated(removeData = true)
            },
            onDismiss = { showRemoveDataConfirmation = false },
        )
    }
    uiState.signOutError?.let { message ->
        MessageDialog(
            title = stringResource(R.string.error),
            message = message,
            confirmText = stringResource(R.string.ok),
            onConfirm = viewModel::dismissSignOutError,
            onDismissRequest = viewModel::dismissSignOutError,
        )
    }
    if (uiState.isSigningOut) {
        LoadingDialog(stringResource(if (uiState.usedFsso) R.string.signing_out else R.string.deregistering))
    }
    if (showKeyBackupNeedsFsso) {
        MessageDialog(
            title = stringResource(R.string.key_backup_needs_fsso_title),
            message = stringResource(R.string.key_backup_needs_fsso),
            confirmText = stringResource(R.string.ok),
            onConfirm = { showKeyBackupNeedsFsso = false },
            onDismissRequest = { showKeyBackupNeedsFsso = false },
        )
    }
    if (showInfo) {
        MessageDialog(
            title = stringResource(R.string.what_is_a_sudo),
            message = stringResource(R.string.sudo_explanation),
            confirmText = stringResource(R.string.ok),
            onConfirm = { showInfo = false },
            dismissText = stringResource(R.string.learn_more),
            onDismiss = {
                showInfo = false
                uriHandler.openUri(docUrl)
            },
            onDismissRequest = { showInfo = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SudosContent(
    uiState: SudosUiState,
    onInfo: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSudo: (Sudo) -> Unit,
    onCreateSudo: () -> Unit,
    onEditSudo: (Sudo) -> Unit,
    onDeleteSudo: (sudoId: String) -> Unit,
    onEntitlements: () -> Unit,
    onUserDetails: () -> Unit,
    onKeyBackup: () -> Unit,
    onDeregister: () -> Unit,
    onSignOut: () -> Unit,
    onSignOutAndRemoveData: () -> Unit,
) {
    Scaffold(
        topBar = {
            SampleTopAppBar(
                title = stringResource(R.string.sudos),
                actions = {
                    ToolbarIconAction(
                        iconResId = R.drawable.ic_info_24px,
                        contentDescription = stringResource(R.string.what_is_a_sudo),
                        onClick = onInfo,
                        testTag = TestTags.INFO_ACTION,
                    )
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading && uiState.sudos.isEmpty(),
            onRefresh = onRefresh,
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) {
            // One list, so the account section scrolls with the Sudos and an empty list can still
            // be pulled to refresh.
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).testTag(TestTags.SUDOS_LIST)) {
                item { SectionHeader(stringResource(R.string.sudos)) }
                if (uiState.sudosNeedKeyImport) {
                    item { KeyImportNotice() }
                } else if (uiState.emailKeyNeedsBackup) {
                    // Not shown while keys need importing, since importing replaces the new key.
                    item { EmailKeyBackupNotice(canExportKeys = uiState.usedFsso) }
                }
                items(uiState.sudos, key = { it.id ?: it.hashCode() }) { sudo ->
                    SudoRow(
                        sudo = sudo,
                        onOpen = { onOpenSudo(sudo) },
                        onEdit = { onEditSudo(sudo) },
                        onDelete = { sudo.id?.let(onDeleteSudo) },
                    )
                }
                if (uiState.hasLoaded && uiState.sudos.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.no_sudos),
                            color = SecondaryText,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        )
                    }
                }
                item {
                    MenuButton(
                        text = stringResource(R.string.create_sudo_button),
                        onClick = onCreateSudo,
                        // Existing Sudos this device can't read mean keys need importing first.
                        // With no Sudos there is nothing to import, so a new user can create one.
                        enabled = !uiState.isDeleting && !uiState.sudosNeedKeyImport,
                        modifier = Modifier.testTag(TestTags.CREATE_SUDO_BUTTON),
                    )
                }
                item {
                    AccountSection(
                        usedFsso = uiState.usedFsso,
                        enabled = !uiState.isSigningOut,
                        onEntitlements = onEntitlements,
                        onUserDetails = onUserDetails,
                        onKeyBackup = onKeyBackup,
                        onDeregister = onDeregister,
                        onSignOut = onSignOut,
                        onSignOutAndRemoveData = onSignOutAndRemoveData,
                    )
                }
            }
        }
    }
}

/**
 * How the user signed in, their entitlements and key backup, then the ways to leave: TEST
 * registrations deregister, while federated users sign out, keeping or removing their keys.
 */
@Composable
private fun AccountSection(
    usedFsso: Boolean,
    enabled: Boolean,
    onEntitlements: () -> Unit,
    onUserDetails: () -> Unit,
    onKeyBackup: () -> Unit,
    onDeregister: () -> Unit,
    onSignOut: () -> Unit,
    onSignOutAndRemoveData: () -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        SectionHeader(stringResource(R.string.account), modifier = Modifier.padding(top = 16.dp))
        ItemCell(
            text = stringResource(R.string.signed_in_with),
            onClick = onUserDetails,
            modifier = Modifier.testTag(TestTags.USER_DETAILS_ACTION),
        ) {
            Text(
                stringResource(if (usedFsso) R.string.federated_signin else R.string.test_registration),
                color = PrimaryText,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        ItemCell(
            text = stringResource(R.string.entitlements),
            onClick = onEntitlements,
            modifier = Modifier.testTag(TestTags.ENTITLEMENTS_ACTION),
        )
        ItemCell(
            text = stringResource(R.string.key_backup),
            onClick = onKeyBackup,
            modifier = Modifier.testTag(TestTags.KEY_BACKUP_ACTION),
        )
        if (usedFsso) {
            AccountActionRow(
                text = stringResource(R.string.sign_out),
                color = MaterialTheme.colorScheme.primary,
                enabled = enabled,
                onClick = onSignOut,
                testTag = TestTags.SIGN_OUT_ACTION,
            )
            AccountActionRow(
                text = stringResource(R.string.sign_out_and_remove_data),
                color = NegativeMessage,
                enabled = enabled,
                onClick = onSignOutAndRemoveData,
                testTag = TestTags.SIGN_OUT_AND_REMOVE_DATA_ACTION,
            )
            SectionFooter(stringResource(R.string.sign_out_explanation))
        } else {
            AccountActionRow(
                text = stringResource(R.string.deregister),
                color = NegativeMessage,
                enabled = enabled,
                onClick = onDeregister,
                testTag = TestTags.DEREGISTER_ACTION,
            )
        }
    }
}

/** A white row that performs an action, such as signing out, rather than opening a screen. */
@Composable
private fun AccountActionRow(
    text: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .padding(bottom = 2.dp)
                .fillMaxWidth()
                .height(60.dp)
                .background(Color.White)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 12.dp)
                .testTag(testTag),
    ) {
        Text(
            text,
            color = if (enabled) color else color.copy(alpha = 0.38f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * Asks a federated user to confirm removing their keys when signing out, offering to export them
 * first, as their Sudos can't be read again without a backup.
 */
@Composable
private fun RemoveDataDialog(
    onExportKeysFirst: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(2.dp),
        containerColor = Color.White,
        tonalElevation = 0.dp,
        title = { Text(stringResource(R.string.remove_data_title), color = PrimaryText, style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(stringResource(R.string.remove_data_confirmation), color = SecondaryText, style = MaterialTheme.typography.bodyLarge)
        },
        // Three choices don't fit side by side, so they're stacked.
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onExportKeysFirst, modifier = Modifier.testTag(TestTags.EXPORT_KEYS_FIRST)) {
                    Text(stringResource(R.string.export_keys_first).uppercase())
                }
                TextButton(onClick = onConfirm, modifier = Modifier.testTag(TestTags.DIALOG_CONFIRM)) {
                    Text(stringResource(R.string.sign_out_and_remove_data).uppercase(), color = NegativeMessage)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.testTag(TestTags.DIALOG_DISMISS)) {
                    Text(stringResource(R.string.cancel).uppercase())
                }
            }
        },
    )
}

/**
 * A Sudo row that opens the Sudo's email addresses when tapped and is deleted by swiping it to the
 * left. The row springs back while the user is asked to confirm.
 */
@Composable
private fun SudoRow(
    sudo: Sudo,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val label = sudo.displayLabel
    SwipeToDeleteRow(onDelete = onDelete, modifier = Modifier.testTag(TestTags.SUDO_ROW)) {
        ItemCell(text = label, onClick = onOpen) {
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit_sudo_label, label),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * Explains that the Sudos can't be read here, and new ones can't be created, until the user
 * imports their keys.
 */
@Composable
private fun KeyImportNotice() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .padding(bottom = 2.dp)
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
                .testTag(TestTags.KEY_IMPORT_NOTICE),
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = WarningIcon)
        Text(
            stringResource(R.string.sudos_need_key_import),
            color = PrimaryText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

/**
 * Says this device has just created a new shared email key, and suggests exporting a key backup
 * when [canExportKeys]. TEST registrations can't back up keys.
 */
@Composable
private fun EmailKeyBackupNotice(canExportKeys: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .padding(bottom = 2.dp)
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
                .testTag(TestTags.EMAIL_KEY_BACKUP_NOTICE),
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = WarningIcon)
        Text(
            stringResource(if (canExportKeys) R.string.email_key_needs_backup else R.string.email_key_created),
            color = PrimaryText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun SudosContentPreview(uiState: SudosUiState) {
    EmailExampleTheme {
        SudosContent(
            uiState = uiState,
            onInfo = {},
            onRefresh = {},
            onOpenSudo = {},
            onCreateSudo = {},
            onEditSudo = {},
            onDeleteSudo = {},
            onEntitlements = {},
            onUserDetails = {},
            onKeyBackup = {},
            onDeregister = {},
            onSignOut = {},
            onSignOutAndRemoveData = {},
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun SudosContentTestRegistrationPreview() {
    SudosContentPreview(
        SudosUiState(
            hasLoaded = true,
            sudos =
                listOf("Shopping", "Work").mapIndexed { index, label ->
                    Sudo(id = "$index", createdAt = Date(index.toLong())).apply { this.label = label }
                },
        ),
    )
}

@Preview(showSystemUi = true)
@Composable
private fun SudosContentFederatedNeedsKeysPreview() {
    SudosContentPreview(SudosUiState(usedFsso = true, sudosNeedKeyImport = true))
}

@Preview(showSystemUi = true)
@Composable
private fun SudosContentEmailKeyNeedsBackupPreview() {
    SudosContentPreview(SudosUiState(usedFsso = true, emailKeyNeedsBackup = true))
}
