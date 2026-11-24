/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaddresses

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.emailaliases.EmailAliasAction
import com.sudoplatform.emailexample.emailaliases.aliasActionOptions
import com.sudoplatform.emailexample.emailaliases.expiryDateToPickerMillis
import com.sudoplatform.emailexample.emailaliases.filterAliasesByType
import com.sudoplatform.emailexample.emailaliases.formatExpiresAtForDisplay
import com.sudoplatform.emailexample.emailaliases.formatExpiryDate
import com.sudoplatform.emailexample.emailaliases.formatMetadataJson
import com.sudoplatform.emailexample.emailaliases.pickerMillisToExpiryDate
import com.sudoplatform.emailexample.emailaliases.showVerifyButton
import com.sudoplatform.emailexample.ui.TestTags
import com.sudoplatform.emailexample.ui.components.DefaultButton
import com.sudoplatform.emailexample.ui.components.DialogTextButton
import com.sudoplatform.emailexample.ui.components.FormDialog
import com.sudoplatform.emailexample.ui.components.ItemCell
import com.sudoplatform.emailexample.ui.components.LoadingDialog
import com.sudoplatform.emailexample.ui.components.MenuButton
import com.sudoplatform.emailexample.ui.components.MessageDialog
import com.sudoplatform.emailexample.ui.components.RetryDialog
import com.sudoplatform.emailexample.ui.components.SampleTopAppBar
import com.sudoplatform.emailexample.ui.components.SectionHeader
import com.sudoplatform.emailexample.ui.components.SwipeToDeleteRow
import com.sudoplatform.emailexample.ui.copyToClipboard
import com.sudoplatform.emailexample.ui.theme.EmailExampleTheme
import com.sudoplatform.emailexample.ui.theme.NegativeMessage
import com.sudoplatform.emailexample.ui.theme.PositiveMessage
import com.sudoplatform.emailexample.ui.theme.PrimaryText
import com.sudoplatform.emailexample.ui.theme.SecondaryText
import com.sudoplatform.emailexample.ui.theme.WarningIcon
import com.sudoplatform.emailexample.ui.theme.WarningMessage
import com.sudoplatform.sudoemail.types.EmailAddress
import com.sudoplatform.sudoemail.types.EmailAlias
import com.sudoplatform.sudoemail.types.EmailAliasRealAddressType
import com.sudoplatform.sudoemail.types.EmailAliasStatus
import java.util.Date

/**
 * Lists a Sudo's email addresses, then its email aliases when the configuration enables them.
 *
 * Links From:
 *  - Sudos: the user taps a Sudo.
 *  - Create Sudo: once a Sudo is created.
 *
 * Links To:
 *  - Email Messages: the user taps an email address.
 *  - Provision Email Address: the user taps "+ Create Email Address".
 *  - Provision Email Alias: the user taps "+ Create Email Alias" or "+ Create External Email
 *    Alias".
 *  - Key Backup: the user taps "Import/Export Keys" in the notice shown when this device doesn't
 *    have an address's keys.
 */
@Composable
fun EmailAddressesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProvisionAddress: () -> Unit,
    onNavigateToProvisionAlias: (external: Boolean) -> Unit,
    onNavigateToMessages: (emailAddressId: String) -> Unit,
    onNavigateToKeyBackup: () -> Unit,
    viewModel: EmailAddressesViewModel = viewModel(factory = EmailAddressesViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.verified.collect {
            Toast.makeText(context, R.string.verification_success, Toast.LENGTH_SHORT).show()
        }
    }

    // Refresh on every return to the screen so addresses and aliases provisioned elsewhere show up.
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    EmailAddressesContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onRefresh = viewModel::refresh,
        onOpenAddress = { onNavigateToMessages(it.id) },
        onCopyAddress = { emailAddress ->
            val label = context.getString(R.string.email_address)
            context.copyToClipboard(label, emailAddress.emailAddress, context.getString(R.string.value_copied, label))
        },
        onDeprovisionAddress = viewModel::requestDeprovisionAddress,
        onCreateAddress = onNavigateToProvisionAddress,
        onOpenKeyBackup = onNavigateToKeyBackup,
        onShowExternalAliases = viewModel::showExternalAliases,
        onSelectAlias = viewModel::selectAlias,
        onCopyAlias = { alias ->
            val label = context.getString(R.string.email_alias)
            context.copyToClipboard(label, alias.aliasAddress, context.getString(R.string.value_copied, label))
        },
        onVerifyAlias = viewModel::startVerification,
        onDeprovisionAlias = viewModel::requestDeprovisionAlias,
        onCreateAlias = { onNavigateToProvisionAlias(uiState.showExternalAliases) },
    )

    uiState.pendingAddressDeprovision?.let { emailAddress ->
        MessageDialog(
            title = stringResource(R.string.delete_email_address_title),
            message = stringResource(R.string.delete_email_address_confirmation, emailAddress.emailAddress),
            confirmText = stringResource(R.string.delete),
            onConfirm = viewModel::confirmDeprovisionAddress,
            dismissText = stringResource(R.string.cancel),
            onDismissRequest = viewModel::cancelDeprovisionAddress,
        )
    }
    uiState.pendingAliasDeprovision?.let { alias ->
        MessageDialog(
            title = stringResource(R.string.delete_email_alias_title),
            message = stringResource(R.string.delete_email_alias_confirmation, alias.aliasAddress),
            confirmText = stringResource(R.string.delete),
            onConfirm = viewModel::confirmDeprovisionAlias,
            dismissText = stringResource(R.string.cancel),
            onDismissRequest = viewModel::cancelDeprovisionAlias,
        )
    }
    uiState.selectedAlias?.let { alias ->
        AliasActionsDialog(
            alias = alias,
            onEnable = { viewModel.enableAlias(alias.id) },
            onDisable = { viewModel.disableAlias(alias.id) },
            onUpdate = { viewModel.startUpdate(alias) },
            onDismiss = viewModel::dismissAliasActions,
        )
    }
    uiState.updateForm?.let { form ->
        UpdateAliasDialog(
            form = form,
            onMetadataChange = viewModel::updateMetadataJson,
            onExpiresAtChange = viewModel::updateExpiresAt,
            onSave = viewModel::saveUpdate,
            onCancel = viewModel::cancelUpdate,
        )
    }
    uiState.verificationForm?.let { form ->
        VerificationCodeDialog(
            form = form,
            onCodeChange = viewModel::updateVerificationCode,
            onSubmit = viewModel::submitVerificationCode,
            onCancel = viewModel::cancelVerification,
        )
    }
    uiState.busyResId?.let { LoadingDialog(stringResource(it)) }

    uiState.listAddressesError?.let { message ->
        RetryDialog(
            title = stringResource(R.string.list_email_addresses_failure),
            message = message,
            onRetry = viewModel::refresh,
            onDismiss = viewModel::dismissListAddressesError,
        )
    }
    uiState.configurationError?.let { message ->
        RetryDialog(
            title = stringResource(R.string.configuration_load_failure),
            message = message,
            onRetry = viewModel::loadConfiguration,
            onDismiss = viewModel::dismissConfigurationError,
        )
    }
    uiState.listAliasesError?.let { message ->
        RetryDialog(
            title = stringResource(R.string.list_email_aliases_failure),
            message = message,
            onRetry = viewModel::listEmailAliases,
            onDismiss = viewModel::dismissListAliasesError,
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
private fun EmailAddressesContent(
    uiState: EmailAddressesUiState,
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenAddress: (EmailAddress) -> Unit,
    onCopyAddress: (EmailAddress) -> Unit,
    onDeprovisionAddress: (emailAddressId: String) -> Unit,
    onCreateAddress: () -> Unit,
    onOpenKeyBackup: () -> Unit,
    onShowExternalAliases: (Boolean) -> Unit,
    onSelectAlias: (EmailAlias) -> Unit,
    onCopyAlias: (EmailAlias) -> Unit,
    onVerifyAlias: (EmailAlias) -> Unit,
    onDeprovisionAlias: (aliasId: String) -> Unit,
    onCreateAlias: () -> Unit,
) {
    Scaffold(
        topBar = { SampleTopAppBar(title = stringResource(R.string.email_addresses), onNavigateBack = onNavigateBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading && uiState.emailAddresses.isEmpty(),
            onRefresh = onRefresh,
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).testTag(TestTags.EMAIL_ADDRESSES_LIST)) {
                item { SectionHeader(stringResource(R.string.email_addresses_section_title)) }
                if (uiState.needsKeyImport) {
                    item { KeyImportNotice(onOpenKeyBackup = onOpenKeyBackup) }
                }
                items(uiState.emailAddresses, key = { it.id }) { emailAddress ->
                    EmailAddressRow(
                        emailAddress = emailAddress,
                        onOpen = { onOpenAddress(emailAddress) },
                        onCopy = { onCopyAddress(emailAddress) },
                        onDeprovision = { onDeprovisionAddress(emailAddress.id) },
                    )
                }
                item {
                    MenuButton(
                        text = stringResource(R.string.create_email_address_button),
                        onClick = onCreateAddress,
                        modifier = Modifier.testTag(TestTags.CREATE_EMAIL_ADDRESS_BUTTON),
                    )
                }
                item { SectionHeader(stringResource(R.string.email_aliases_section_title), modifier = Modifier.padding(top = 16.dp)) }
                aliasesSection(
                    uiState = uiState,
                    onShowExternalAliases = onShowExternalAliases,
                    onSelectAlias = onSelectAlias,
                    onCopyAlias = onCopyAlias,
                    onVerifyAlias = onVerifyAlias,
                    onDeprovisionAlias = onDeprovisionAlias,
                    onCreateAlias = onCreateAlias,
                )
            }
        }
    }
}

/**
 * The aliases of the selected type, if the configuration enables them. Internal aliases forward to one
 * of the user's addresses; external ones forward to an address outside the platform, once the
 * user has verified it.
 */
@OptIn(ExperimentalMaterial3Api::class)
private fun LazyListScope.aliasesSection(
    uiState: EmailAddressesUiState,
    onShowExternalAliases: (Boolean) -> Unit,
    onSelectAlias: (EmailAlias) -> Unit,
    onCopyAlias: (EmailAlias) -> Unit,
    onVerifyAlias: (EmailAlias) -> Unit,
    onDeprovisionAlias: (aliasId: String) -> Unit,
    onCreateAlias: () -> Unit,
) {
    val configuration = uiState.aliasConfiguration
    if (configuration == null) {
        item { SectionNote(stringResource(R.string.loading_configuration)) }
        return
    }
    if (!configuration.aliasesEnabled) {
        item { SectionNote(stringResource(R.string.email_aliases_disabled_message), Modifier.testTag(TestTags.ALIASES_DISABLED_TEXT)) }
        return
    }
    val showExternal = uiState.showExternalAliases
    if (configuration.externalAliasesEnabled) {
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                SegmentedButton(
                    selected = !showExternal,
                    onClick = { onShowExternalAliases(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag(TestTags.INTERNAL_ALIASES_BUTTON),
                ) {
                    Text(stringResource(R.string.internal_email_aliases))
                }
                SegmentedButton(
                    selected = showExternal,
                    onClick = { onShowExternalAliases(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag(TestTags.EXTERNAL_ALIASES_BUTTON),
                ) {
                    Text(stringResource(R.string.external_email_aliases_switch_label))
                }
            }
        }
    }
    val aliases = filterAliasesByType(uiState.aliases, showExternal)
    items(aliases, key = { it.id }) { alias ->
        SwipeToDeleteRow(
            onDelete = { onDeprovisionAlias(alias.id) },
            modifier = Modifier.testTag(TestTags.EMAIL_ALIAS_ROW),
        ) {
            EmailAliasRow(
                alias = alias,
                showVerify = showVerifyButton(showExternal, alias.realAddressType, alias.status),
                onClick = { onSelectAlias(alias) },
                onLongClick = { onCopyAlias(alias) },
                onVerify = { onVerifyAlias(alias) },
            )
        }
    }
    if (aliases.isEmpty()) {
        item {
            SectionNote(
                stringResource(if (showExternal) R.string.no_external_aliases else R.string.no_internal_aliases),
                Modifier.testTag(TestTags.EMPTY_ALIASES_TEXT),
            )
        }
    }
    item {
        MenuButton(
            text = stringResource(if (showExternal) R.string.create_external_email_alias_button else R.string.create_email_alias_button),
            onClick = onCreateAlias,
            modifier = Modifier.testTag(TestTags.CREATE_EMAIL_ALIAS_BUTTON),
        )
    }
}

/** Secondary text in place of a list, such as an empty state. */
@Composable
private fun SectionNote(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        color = SecondaryText,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
    )
}

/**
 * An address row that opens its messages when tapped, copies the address on a long press and is
 * deprovisioned by swiping it to the left.
 */
@Composable
private fun EmailAddressRow(
    emailAddress: EmailAddress,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onDeprovision: () -> Unit,
) {
    val displayName = emailAddress.displayName
    val text =
        if (displayName != null) {
            stringResource(R.string.email_address_with_display_name, displayName, emailAddress.emailAddress)
        } else {
            emailAddress.emailAddress
        }
    SwipeToDeleteRow(onDelete = onDeprovision, modifier = Modifier.testTag(TestTags.EMAIL_ADDRESS_ROW)) {
        ItemCell(text = text, onClick = onOpen, onLongClick = onCopy)
    }
}

/**
 * An alias with its status, real address, metadata and expiry, which opens the alias's actions
 * when tapped and copies the alias address on a long press.
 */
@Composable
private fun EmailAliasRow(
    alias: EmailAlias,
    showVerify: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onVerify: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .padding(bottom = 2.dp)
                .fillMaxWidth()
                .background(Color.White)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(alias.aliasAddress, color = PrimaryText, style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(alias.status.labelResId()),
                color = alias.status.color(),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag(TestTags.ALIAS_STATUS_TEXT),
            )
            Text(alias.realAddress, color = SecondaryText, style = MaterialTheme.typography.bodySmall)
            Text(
                stringResource(R.string.alias_metadata_value, formatMetadataJson(alias.metadata).ifEmpty { "-" }),
                color = SecondaryText,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag(TestTags.ALIAS_METADATA_TEXT),
            )
            Text(
                formatExpiresAtForDisplay(alias.expiresAt, stringResource(R.string.set_expiration)),
                color = SecondaryText,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag(TestTags.ALIAS_EXPIRES_TEXT),
            )
        }
        if (showVerify) {
            DefaultButton(
                text = stringResource(R.string.verify),
                onClick = onVerify,
                modifier = Modifier.padding(start = 8.dp).testTag(TestTags.VERIFY_BUTTON),
            )
        }
    }
}

/** Explains that this device can't read some of the addresses until the user imports their keys. */
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
                stringResource(R.string.addresses_need_key_import),
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

/** Offers Enable or Disable as the alias's status allows, then Update and Cancel. */
@Composable
private fun AliasActionsDialog(
    alias: EmailAlias,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(2.dp),
        containerColor = Color.White,
        tonalElevation = 0.dp,
        title = { Text(alias.aliasAddress, color = PrimaryText, style = MaterialTheme.typography.titleLarge) },
        confirmButton = {
            Row {
                aliasActionOptions(alias.status).forEach { option ->
                    when (option) {
                        EmailAliasAction.ENABLE ->
                            DialogTextButton(stringResource(R.string.enable), onEnable, TestTags.ENABLE_ALIAS_ACTION)
                        EmailAliasAction.DISABLE ->
                            DialogTextButton(stringResource(R.string.disable), onDisable, TestTags.DISABLE_ALIAS_ACTION)
                        EmailAliasAction.UPDATE ->
                            DialogTextButton(stringResource(R.string.update), onUpdate, TestTags.UPDATE_ALIAS_ACTION)
                        EmailAliasAction.CANCEL ->
                            DialogTextButton(stringResource(R.string.cancel), onDismiss, TestTags.DIALOG_DISMISS)
                    }
                }
            }
        },
    )
}

/** Edits an alias's metadata, as JSON, and its expiry date. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateAliasDialog(
    form: AliasUpdateForm,
    onMetadataChange: (String) -> Unit,
    onExpiresAtChange: (Date?) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    FormDialog(
        title = form.alias.aliasAddress,
        confirmText = stringResource(R.string.save),
        onConfirm = onSave,
        onDismiss = onCancel,
    ) {
        Column {
            OutlinedTextField(
                value = form.metadataJson,
                onValueChange = onMetadataChange,
                label = { Text(stringResource(R.string.metadata_json_label)) },
                placeholder = { Text(stringResource(R.string.metadata_json_hint)) },
                minLines = 3,
                isError = form.showInvalidMetadata,
                supportingText =
                    if (form.showInvalidMetadata) {
                        { Text(stringResource(R.string.invalid_json_error)) }
                    } else {
                        null
                    },
                modifier = Modifier.fillMaxWidth().testTag(TestTags.ALIAS_METADATA_FIELD),
            )
            Text(
                stringResource(R.string.set_expiration),
                color = SecondaryText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f).testTag(TestTags.ALIAS_EXPIRY_BUTTON),
                ) {
                    Text(form.expiresAt?.let(::formatExpiryDate) ?: stringResource(R.string.pick_date))
                }
                TextButton(
                    onClick = { onExpiresAtChange(null) },
                    modifier = Modifier.padding(start = 8.dp).testTag(TestTags.CLEAR_ALIAS_EXPIRY_BUTTON),
                ) {
                    Text(stringResource(R.string.clear_date).uppercase())
                }
            }
        }
    }
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = form.expiresAt?.let(::expiryDateToPickerMillis))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                DialogTextButton(
                    text = stringResource(R.string.ok),
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onExpiresAtChange(pickerMillisToExpiryDate(it)) }
                        showDatePicker = false
                    },
                    testTag = TestTags.DATE_PICKER_CONFIRM,
                )
            },
            dismissButton = {
                DialogTextButton(stringResource(R.string.cancel), { showDatePicker = false }, TestTags.DATE_PICKER_DISMISS)
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/** Asks for the code the service emailed to an external alias's real address. */
@Composable
private fun VerificationCodeDialog(
    form: AliasVerificationForm,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    FormDialog(
        title = stringResource(R.string.verification_code_prompt),
        confirmText = stringResource(R.string.ok),
        onConfirm = onSubmit,
        onDismiss = onCancel,
    ) {
        OutlinedTextField(
            value = form.code,
            onValueChange = onCodeChange,
            label = { Text(stringResource(R.string.verification_code)) },
            singleLine = true,
            isError = form.showCodeRequired,
            supportingText =
                if (form.showCodeRequired) {
                    { Text(stringResource(R.string.verification_code_required)) }
                } else {
                    null
                },
            modifier = Modifier.fillMaxWidth().testTag(TestTags.VERIFICATION_CODE_FIELD),
        )
    }
}

private fun EmailAliasStatus.labelResId(): Int =
    when (this) {
        EmailAliasStatus.ENABLED -> R.string.email_alias_status_enabled
        EmailAliasStatus.DISABLED -> R.string.email_alias_status_disabled
        EmailAliasStatus.LOCKED -> R.string.email_alias_status_locked
        EmailAliasStatus.PENDING -> R.string.email_alias_status_pending
        EmailAliasStatus.UNKNOWN -> R.string.email_alias_status_unknown
    }

private fun EmailAliasStatus.color(): Color =
    when (this) {
        EmailAliasStatus.ENABLED -> PositiveMessage
        EmailAliasStatus.DISABLED, EmailAliasStatus.LOCKED -> NegativeMessage
        EmailAliasStatus.PENDING -> WarningMessage
        EmailAliasStatus.UNKNOWN -> SecondaryText
    }

private fun previewAddress(
    id: String,
    address: String,
    displayName: String?,
) = EmailAddress(
    id = id,
    owner = "owner",
    owners = emptyList(),
    emailAddress = address,
    size = 0.0,
    numberOfEmailMessages = 0,
    version = 1,
    createdAt = Date(),
    updatedAt = Date(),
    displayName = displayName,
    folders = emptyList(),
)

private fun previewAlias(
    id: String,
    realAddressType: EmailAliasRealAddressType,
    status: EmailAliasStatus,
    metadata: Map<String, String>? = null,
    expiresAt: Date? = null,
) = EmailAlias(
    id = id,
    owner = "owner",
    owners = emptyList(),
    identityId = "identity",
    aliasAddress = "$id@aliases.example.com",
    realAddress = if (realAddressType == EmailAliasRealAddressType.EXTERNAL) "me@example.org" else "shopping@example.com",
    realAddressType = realAddressType,
    status = status,
    inboundReceived = 0,
    inboundDelivered = 0,
    outboundReceived = 0,
    outboundDelivered = 0,
    spamCount = 0,
    virusCount = 0,
    expiresAt = expiresAt,
    createdAt = Date(),
    updatedAt = Date(),
    version = 1,
    metadata = metadata,
)

@Composable
private fun EmailAddressesContentPreview(uiState: EmailAddressesUiState) {
    EmailExampleTheme {
        EmailAddressesContent(
            uiState = uiState,
            onNavigateBack = {},
            onRefresh = {},
            onOpenAddress = {},
            onCopyAddress = {},
            onDeprovisionAddress = {},
            onCreateAddress = {},
            onOpenKeyBackup = {},
            onShowExternalAliases = {},
            onSelectAlias = {},
            onCopyAlias = {},
            onVerifyAlias = {},
            onDeprovisionAlias = {},
            onCreateAlias = {},
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun EmailAddressesContentInternalPreview() {
    EmailAddressesContentPreview(
        EmailAddressesUiState(
            hasLoaded = true,
            emailAddresses =
                listOf(
                    previewAddress("1", "shopping@example.com", "Shopping"),
                    previewAddress("2", "work@example.com", null),
                ),
            aliasConfiguration = AliasConfiguration(aliasesEnabled = true, externalAliasesEnabled = true),
            aliases =
                listOf(
                    previewAlias("deals", EmailAliasRealAddressType.INTERNAL, EmailAliasStatus.ENABLED, mapOf("site" to "shop")),
                    previewAlias("old", EmailAliasRealAddressType.INTERNAL, EmailAliasStatus.DISABLED, expiresAt = Date()),
                ),
        ),
    )
}

@Preview(showSystemUi = true)
@Composable
private fun EmailAddressesContentExternalPreview() {
    EmailAddressesContentPreview(
        EmailAddressesUiState(
            hasLoaded = true,
            needsKeyImport = true,
            aliasConfiguration = AliasConfiguration(aliasesEnabled = true, externalAliasesEnabled = true),
            showExternalAliases = true,
            aliases = listOf(previewAlias("news", EmailAliasRealAddressType.EXTERNAL, EmailAliasStatus.PENDING)),
        ),
    )
}

@Preview(showSystemUi = true)
@Composable
private fun EmailAddressesContentAliasesDisabledPreview() {
    EmailAddressesContentPreview(
        EmailAddressesUiState(
            hasLoaded = true,
            emailAddresses = listOf(previewAddress("1", "shopping@example.com", "Shopping")),
            aliasConfiguration = AliasConfiguration(aliasesEnabled = false, externalAliasesEnabled = false),
        ),
    )
}
