/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.keybackup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sudoplatform.telephonyexample.R
import com.sudoplatform.telephonyexample.ui.TestTags
import com.sudoplatform.telephonyexample.ui.components.LoadingDialog
import com.sudoplatform.telephonyexample.ui.components.MenuButton
import com.sudoplatform.telephonyexample.ui.components.MessageDialog
import com.sudoplatform.telephonyexample.ui.components.SampleTopAppBar
import com.sudoplatform.telephonyexample.ui.theme.PrimaryText
import com.sudoplatform.telephonyexample.ui.theme.SecondaryText
import com.sudoplatform.telephonyexample.ui.theme.TelephonyExampleTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** The shortest password accepted for a new backup, matching the iOS sample. */
private const val MINIMUM_PASSWORD_LENGTH = 8

/**
 * Exports this device's Sudo and phone message keys to a password-protected file, and imports
 * them from one, so the same user can read their Sudos and messages on another device.
 *
 * Links From:
 *  - Sudos: the user taps "Import/Export Keys" after signing in with Federated Sign In, or
 *    "Export Keys First" before removing their data.
 */
@Composable
fun KeyBackupScreen(
    onNavigateBack: () -> Unit,
    viewModel: KeyBackupViewModel = viewModel(factory = KeyBackupViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val createDocument =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(KeyBackupViewModel.BACKUP_MIME_TYPE)) { uri ->
            viewModel.saveExport(uri)
        }
    val openDocument =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            viewModel.readBackup(uri)
        }

    LaunchedEffect(viewModel) {
        viewModel.createDocument.collect { createDocument.launch(it.fileName) }
    }

    KeyBackupContent(
        onNavigateBack = onNavigateBack,
        onExport = viewModel::startExport,
        onImport = { openDocument.launch(KeyBackupViewModel.IMPORT_MIME_TYPES) },
    )

    uiState.pendingImport?.let { file ->
        MessageDialog(
            title = stringResource(R.string.import_keys_confirmation_title),
            message = stringResource(R.string.import_keys_confirmation, describe(file)),
            confirmText = stringResource(R.string.continue_action),
            onConfirm = viewModel::confirmImport,
            dismissText = stringResource(R.string.cancel),
            onDismissRequest = viewModel::cancelImport,
        )
    }
    uiState.passwordPrompt?.let { prompt ->
        PasswordDialog(
            prompt = prompt,
            errorMessage = uiState.passwordError,
            enabled = !uiState.isWorking,
            onSubmit = { password ->
                when (prompt) {
                    PasswordPrompt.Export -> viewModel.export(password)
                    is PasswordPrompt.Import -> viewModel.import(password)
                }
            },
            onCancel = viewModel::cancelPasswordPrompt,
        )
    }
    uiState.importResult?.let { result ->
        MessageDialog(
            title = stringResource(R.string.import_keys_result_title),
            message = describe(result),
            confirmText = stringResource(R.string.ok),
            onConfirm = viewModel::dismissImportResult,
            onDismissRequest = viewModel::dismissImportResult,
        )
    }
    if (uiState.exportSaved) {
        MessageDialog(
            title = stringResource(R.string.export_keys_saved_title),
            message = stringResource(R.string.export_keys_saved),
            confirmText = stringResource(R.string.ok),
            onConfirm = viewModel::dismissExportSaved,
            onDismissRequest = viewModel::dismissExportSaved,
        )
    }
    uiState.error?.let { message ->
        MessageDialog(
            title = stringResource(R.string.key_backup_failure),
            message = message,
            confirmText = stringResource(R.string.ok),
            onConfirm = viewModel::dismissError,
            onDismissRequest = viewModel::dismissError,
        )
    }
    if (uiState.isWorking) {
        LoadingDialog(
            stringResource(
                when (uiState.passwordPrompt) {
                    PasswordPrompt.Export -> R.string.exporting_keys
                    is PasswordPrompt.Import -> R.string.importing_keys
                    null -> R.string.please_wait
                },
            ),
        )
    }
}

@Composable
private fun KeyBackupContent(
    onNavigateBack: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    Scaffold(
        topBar = { SampleTopAppBar(title = stringResource(R.string.key_backup), onNavigateBack = onNavigateBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
        ) {
            DetailText(stringResource(R.string.key_backup_explanation), Modifier.padding(bottom = 24.dp))
            MenuButton(
                text = stringResource(R.string.export_keys),
                onClick = onExport,
                modifier = Modifier.testTag(TestTags.EXPORT_KEYS_BUTTON),
            )
            DetailText(stringResource(R.string.export_keys_detail), Modifier.padding(top = 8.dp, bottom = 24.dp))
            MenuButton(
                text = stringResource(R.string.import_keys),
                onClick = onImport,
                modifier = Modifier.testTag(TestTags.IMPORT_KEYS_BUTTON),
            )
            DetailText(stringResource(R.string.import_keys_detail), Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun DetailText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text, color = SecondaryText, style = MaterialTheme.typography.bodyMedium, modifier = modifier)
}

/**
 * Asks for the backup's password. A new backup's password must be entered twice and be at least
 * [MINIMUM_PASSWORD_LENGTH] characters, as it can't be recovered.
 */
@Composable
private fun PasswordDialog(
    prompt: PasswordPrompt,
    errorMessage: String?,
    enabled: Boolean,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val isExport = prompt == PasswordPrompt.Export
    // Not saveable, so the password never goes into the saved instance state.
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val isValid = if (isExport) password.length >= MINIMUM_PASSWORD_LENGTH && password == confirmation else password.isNotEmpty()
    val hint =
        when {
            !isExport -> null
            password.isNotEmpty() && password.length < MINIMUM_PASSWORD_LENGTH ->
                stringResource(R.string.password_too_short, MINIMUM_PASSWORD_LENGTH)
            confirmation.isNotEmpty() && password != confirmation -> stringResource(R.string.passwords_dont_match)
            else -> null
        }

    AlertDialog(
        onDismissRequest = { if (enabled) onCancel() },
        shape = RoundedCornerShape(2.dp),
        containerColor = Color.White,
        tonalElevation = 0.dp,
        title = {
            Text(
                stringResource(if (isExport) R.string.export_keys else R.string.import_keys),
                color = PrimaryText,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column {
                PasswordField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.password),
                    enabled = enabled,
                    testTag = TestTags.BACKUP_PASSWORD_FIELD,
                )
                if (isExport) {
                    PasswordField(
                        value = confirmation,
                        onValueChange = { confirmation = it },
                        label = stringResource(R.string.confirm_password),
                        enabled = enabled,
                        testTag = TestTags.BACKUP_PASSWORD_CONFIRMATION_FIELD,
                    )
                }
                DetailText(
                    when (prompt) {
                        PasswordPrompt.Export -> stringResource(R.string.export_password_detail)
                        is PasswordPrompt.Import -> stringResource(R.string.import_password_detail, describe(prompt.file))
                    },
                    Modifier.padding(top = 8.dp),
                )
                (hint ?: errorMessage)?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(password) },
                enabled = enabled && isValid,
                modifier = Modifier.testTag(TestTags.DIALOG_CONFIRM),
            ) {
                Text(stringResource(if (isExport) R.string.export else R.string.import_action).uppercase())
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = enabled, modifier = Modifier.testTag(TestTags.DIALOG_DISMISS)) {
                Text(stringResource(R.string.cancel).uppercase())
            }
        },
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    testTag: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth().testTag(testTag),
    )
}

/** Describes where a backup came from, e.g. "Exported from iOS on 3 Nov 2026". */
@Composable
private fun describe(file: KeyBackupFile): String {
    val platform =
        when (file.platform) {
            "ios" -> stringResource(R.string.platform_ios)
            "android" -> stringResource(R.string.platform_android)
            else -> file.platform
        }
    val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault()).format(file.createdAt)
    return stringResource(R.string.backup_description, platform, date)
}

@Composable
private fun describe(result: KeyBackupImportResult): String =
    buildList {
        val importedKeys = if (result.importedTelephonyKeys) R.plurals.imported_sudo_and_phone_number_keys else R.plurals.imported_sudo_keys
        add(pluralStringResource(importedKeys, result.sudoKeyCount, result.sudoKeyCount))
        if (result.unrecognisedSdks.isNotEmpty()) {
            add(stringResource(R.string.unrecognised_sdks_skipped, result.unrecognisedSdks.joinToString(", ")))
        }
    }.joinToString(" ")

@Preview(showSystemUi = true)
@Composable
private fun KeyBackupContentPreview() {
    TelephonyExampleTheme {
        KeyBackupContent(onNavigateBack = {}, onExport = {}, onImport = {})
    }
}
