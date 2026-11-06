/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.phonemessages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sudoplatform.sudotelephony.types.PhoneMessage
import com.sudoplatform.telephonyexample.R
import com.sudoplatform.telephonyexample.ui.components.CopyButton
import com.sudoplatform.telephonyexample.ui.components.InPlaceLoading
import com.sudoplatform.telephonyexample.ui.components.LoadingDialog
import com.sudoplatform.telephonyexample.ui.components.MessageDialog
import com.sudoplatform.telephonyexample.ui.components.RetryDialog
import com.sudoplatform.telephonyexample.ui.components.SampleTopAppBar
import com.sudoplatform.telephonyexample.ui.components.ToolbarTextAction
import com.sudoplatform.telephonyexample.ui.copyMessageText
import com.sudoplatform.telephonyexample.ui.formatPhoneNumber
import com.sudoplatform.telephonyexample.ui.theme.PrimaryText
import com.sudoplatform.telephonyexample.ui.theme.SecondaryText
import com.sudoplatform.telephonyexample.ui.theme.TelephonyExampleTheme
import java.text.DateFormat

/**
 * Shows a message received on a phone number.
 *
 * Links From:
 *  - Messages: the user taps a message.
 *
 * Links To:
 *  - Messages: once the message is deleted, or if it couldn't be loaded.
 */
@Composable
fun PhoneMessageScreen(
    onClose: () -> Unit,
    viewModel: PhoneMessageViewModel = viewModel(factory = PhoneMessageViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.closed.collect { onClose() }
    }

    PhoneMessageContent(
        uiState = uiState,
        onCopy = { message -> context.copyMessageText(message.body) },
        onDelete = viewModel::requestDelete,
        onNavigateBack = onClose,
    )

    if (uiState.confirmDelete) {
        MessageDialog(
            title = stringResource(R.string.delete_phone_message_title),
            message = stringResource(R.string.delete_phone_message_confirmation),
            confirmText = stringResource(R.string.delete),
            onConfirm = viewModel::confirmDelete,
            dismissText = stringResource(R.string.cancel),
            onDismissRequest = viewModel::cancelDelete,
        )
    }
    uiState.loadError?.let { message ->
        RetryDialog(
            title = stringResource(R.string.load_phone_message_failure),
            message = message,
            onRetry = viewModel::load,
            onDismiss = {
                viewModel.dismissLoadError()
                onClose()
            },
        )
    }
    uiState.deleteFailure?.let { failure ->
        MessageDialog(
            title = stringResource(R.string.delete_phone_message_failure),
            message = failure.message ?: stringResource(R.string.delete_phone_message_not_deleted),
            confirmText = stringResource(R.string.ok),
            onConfirm = viewModel::dismissDeleteFailure,
            onDismissRequest = viewModel::dismissDeleteFailure,
        )
    }
    if (uiState.isDeleting) {
        LoadingDialog(stringResource(R.string.deleting_phone_message))
    }
}

@Composable
private fun PhoneMessageContent(
    uiState: PhoneMessageUiState,
    onCopy: (PhoneMessage) -> Unit,
    onDelete: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            SampleTopAppBar(
                title = stringResource(R.string.message),
                onNavigateBack = onNavigateBack,
                actions = {
                    ToolbarTextAction(
                        text = stringResource(R.string.delete),
                        onClick = onDelete,
                        enabled = uiState.message != null && !uiState.isDeleting,
                    )
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val message = uiState.message
        if (message == null) {
            if (uiState.isLoading) {
                InPlaceLoading(stringResource(R.string.loading_phone_message), modifier = Modifier.padding(innerPadding))
            }
            return@Scaffold
        }
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
        ) {
            MessageField(
                label = stringResource(R.string.from),
                value = message.remotePhoneNumber?.let(::formatPhoneNumber) ?: stringResource(R.string.unknown_sender),
            )
            MessageField(label = stringResource(R.string.to), value = formatPhoneNumber(message.localPhoneNumber))
            MessageField(
                label = stringResource(R.string.received),
                value = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(message.receivedAt),
            )
            Row(modifier = Modifier.padding(top = 16.dp)) {
                SelectionContainer(modifier = Modifier.weight(1f)) {
                    Text(
                        message.body.ifEmpty { stringResource(R.string.no_text) },
                        color = if (message.body.isEmpty()) SecondaryText else PrimaryText,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                if (message.body.isNotEmpty()) {
                    CopyButton(
                        contentDescription = stringResource(R.string.copy_value, stringResource(R.string.message)),
                        onClick = { onCopy(message) },
                        modifier = Modifier.offset(y = (-12).dp),
                    )
                }
            }
            if (message.mediaOmitted) {
                Text(
                    stringResource(R.string.media_omitted_detail),
                    color = SecondaryText,
                    fontStyle = FontStyle.Italic,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun MessageField(
    label: String,
    value: String,
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(label, color = SecondaryText, style = MaterialTheme.typography.bodySmall)
        Text(value, color = PrimaryText, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showSystemUi = true)
@Composable
private fun PhoneMessageContentPreview() {
    TelephonyExampleTheme {
        PhoneMessageContent(
            uiState =
                PhoneMessageUiState(
                    isLoading = false,
                    message = previewMessage("1", "+16505550199", "Running late, see you at six.", seen = true),
                ),
            onCopy = {},
            onDelete = {},
            onNavigateBack = {},
        )
    }
}
