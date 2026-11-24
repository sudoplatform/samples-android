/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.ui.TestTags
import com.sudoplatform.emailexample.ui.components.DialogTextButton
import com.sudoplatform.emailexample.ui.components.DropdownField
import com.sudoplatform.emailexample.ui.components.FormDialog
import com.sudoplatform.emailexample.ui.components.InPlaceLoading
import com.sudoplatform.emailexample.ui.components.LoadingDialog
import com.sudoplatform.emailexample.ui.components.MessageDialog
import com.sudoplatform.emailexample.ui.components.RetryDialog
import com.sudoplatform.emailexample.ui.components.SampleTopAppBar
import com.sudoplatform.emailexample.ui.components.SectionHeader
import com.sudoplatform.emailexample.ui.components.SwipeToDeleteRow
import com.sudoplatform.emailexample.ui.components.ToolbarIconAction
import com.sudoplatform.emailexample.ui.components.whiteOutlinedTextFieldColors
import com.sudoplatform.emailexample.ui.theme.EmailExampleTheme
import com.sudoplatform.sudoemail.types.EmailAttachment
import com.sudoplatform.sudoemail.types.EmailMessage
import java.util.Calendar
import java.util.Date

/**
 * Composes and sends an email message: a new message, a reply, a forward, or an edit of a draft.
 * A draft can also be scheduled to send later, or have its schedule cancelled.
 *
 * Links From:
 *  - Email Messages: the user taps the compose button, or a draft.
 *  - Read Email Message: the user taps Reply or Forward.
 *
 * Links To:
 *  - Back to the previous screen once the message is sent, saved, scheduled or unscheduled.
 *  - A file picker: the user taps Attach.
 *  - A viewer app: the user taps an attachment.
 */
@Composable
fun ComposeEmailMessageScreen(
    onNavigateBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: ComposeEmailMessageViewModel = viewModel(factory = ComposeEmailMessageViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val noViewerMessage = stringResource(R.string.open_attachment_failure)
    var showSchedulePicker by rememberSaveable { mutableStateOf(false) }

    val filePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let(viewModel::addAttachment)
        }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ComposeEvent.Done -> {
                    Toast.makeText(context, event.messageResId, Toast.LENGTH_SHORT).show()
                    onDone()
                }
                is ComposeEvent.OpenAttachment -> {
                    val intent =
                        Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(event.file.uri, event.file.mimeType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    try {
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        viewModel.reportAttachmentError(noViewerMessage)
                    }
                }
            }
        }
    }

    ComposeEmailMessageContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onAction = { action ->
            when (action) {
                ComposeAction.CANCEL_SCHEDULE -> viewModel.cancelSchedule()
                ComposeAction.SCHEDULE -> showSchedulePicker = true
                ComposeAction.ATTACH -> filePicker.launch("*/*")
                ComposeAction.SAVE_DRAFT -> viewModel.saveDraft()
                ComposeAction.SEND -> viewModel.send()
            }
        },
        onSendFromSelected = viewModel::selectSendFrom,
        onToChange = viewModel::updateTo,
        onCcChange = viewModel::updateCc,
        onBccChange = viewModel::updateBcc,
        onSubjectChange = viewModel::updateSubject,
        onBodyChange = viewModel::updateBody,
        onOpenAttachment = viewModel::openAttachment,
        onRemoveAttachment = viewModel::removeAttachment,
    )

    if (showSchedulePicker) {
        ScheduleSendPicker(
            onPicked = { sendAt ->
                showSchedulePicker = false
                viewModel.schedule(sendAt)
            },
            onDismiss = { showSchedulePicker = false },
        )
    }
    uiState.busyResId?.let { LoadingDialog(stringResource(it)) }

    if (uiState.showToRequired) {
        MessageDialog(
            title = stringResource(R.string.validate_to_address),
            confirmText = stringResource(R.string.ok),
            onConfirm = viewModel::dismissToRequired,
            onDismissRequest = viewModel::dismissToRequired,
        )
    }
    if (uiState.showScheduleInPast) {
        MessageDialog(
            title = stringResource(R.string.schedule_email_message_failure),
            message = stringResource(R.string.schedule_in_past),
            confirmText = stringResource(R.string.ok),
            onConfirm = viewModel::dismissScheduleInPast,
            onDismissRequest = viewModel::dismissScheduleInPast,
        )
    }
    uiState.loadError?.let { error ->
        RetryDialog(
            title = stringResource(error.titleResId),
            message = error.message,
            onRetry = viewModel::load,
            onDismiss = {
                viewModel.dismissLoadError()
                if (uiState.sender == null || !uiState.isMessageLoaded) onNavigateBack()
            },
        )
    }
    uiState.listAliasesError?.let { message ->
        RetryDialog(
            title = stringResource(R.string.list_email_aliases_failure),
            message = message,
            onRetry = viewModel::loadAliases,
            onDismiss = viewModel::dismissListAliasesError,
        )
    }
    uiState.encryptionLookupError?.let { message ->
        MessageDialog(
            title = stringResource(R.string.encrypted_email_address_lookup_failure),
            message = message,
            confirmText = stringResource(R.string.cancel),
            onConfirm = viewModel::dismissEncryptionLookupError,
            onDismissRequest = viewModel::dismissEncryptionLookupError,
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

@Composable
private fun ComposeEmailMessageContent(
    uiState: ComposeUiState,
    onNavigateBack: () -> Unit,
    onAction: (ComposeAction) -> Unit,
    onSendFromSelected: (SendFromOption) -> Unit,
    onToChange: (String) -> Unit,
    onCcChange: (String) -> Unit,
    onBccChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onOpenAttachment: (EmailAttachment) -> Unit,
    onRemoveAttachment: (EmailAttachment) -> Unit,
) {
    Scaffold(
        topBar = {
            SampleTopAppBar(
                title = stringResource(R.string.compose_email_message),
                onNavigateBack = onNavigateBack,
                actions = {
                    uiState.actions.forEach { action ->
                        ComposeActionButton(action, enabled = uiState.canAct, onClick = { onAction(action) })
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        if (!uiState.isMessageLoaded) {
            if (uiState.isLoading) {
                InPlaceLoading(stringResource(R.string.reading), modifier = Modifier.padding(innerPadding))
            }
            return@Scaffold
        }
        val enabled = !uiState.isBusy
        val colors = whiteOutlinedTextFieldColors()
        val emailKeyboard = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
        ) {
            if (uiState.isEncrypted) {
                Text(
                    stringResource(R.string.encrypted_indicator_label),
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag(TestTags.ENCRYPTED_INDICATOR),
                )
            }
            if (uiState.aliasesEnabled) {
                val emailAddressLabel = stringResource(R.string.send_aliased_email_none_option)
                DropdownField(
                    label = stringResource(R.string.email_alias),
                    options = uiState.sendFromOptions,
                    selected = uiState.selectedSendFromOption,
                    optionText = { sendFromOptionLabel(it, emailAddressLabel) },
                    onSelected = onSendFromSelected,
                    enabled = enabled,
                    testTag = TestTags.SEND_FROM_FIELD,
                    colors = colors,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedTextField(
                value = uiState.content.to,
                onValueChange = onToChange,
                enabled = enabled,
                singleLine = true,
                label = { Text(stringResource(R.string.to).trim()) },
                keyboardOptions = emailKeyboard,
                colors = colors,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag(TestTags.TO_FIELD),
            )
            OutlinedTextField(
                value = uiState.content.cc,
                onValueChange = onCcChange,
                enabled = enabled,
                singleLine = true,
                label = { Text(stringResource(R.string.cc).trim()) },
                keyboardOptions = emailKeyboard,
                colors = colors,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag(TestTags.CC_FIELD),
            )
            OutlinedTextField(
                value = uiState.content.bcc,
                onValueChange = onBccChange,
                enabled = enabled,
                singleLine = true,
                label = { Text(stringResource(R.string.bcc).trim()) },
                keyboardOptions = emailKeyboard,
                colors = colors,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag(TestTags.BCC_FIELD),
            )
            OutlinedTextField(
                value = uiState.content.subject,
                onValueChange = onSubjectChange,
                enabled = enabled,
                singleLine = true,
                label = { Text(stringResource(R.string.subject).trim()) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                colors = colors,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag(TestTags.SUBJECT_FIELD),
            )
            OutlinedTextField(
                value = uiState.content.body,
                onValueChange = onBodyChange,
                enabled = enabled,
                minLines = 6,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = colors,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).testTag(TestTags.BODY_FIELD),
            )
            if (uiState.attachments.isNotEmpty()) {
                SectionHeader(stringResource(R.string.attachments))
                uiState.attachments.forEach { attachment ->
                    key(attachment.contentId) {
                        SwipeToDeleteRow(onDelete = { onRemoveAttachment(attachment) }, enabled = enabled) {
                            AttachmentRow(attachment = attachment, onClick = { onOpenAttachment(attachment) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComposeActionButton(
    action: ComposeAction,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    when (action) {
        ComposeAction.CANCEL_SCHEDULE ->
            ToolbarIconAction(
                iconResId = R.drawable.ic_baseline_block_24px,
                contentDescription = stringResource(R.string.cancel_scheduled_send),
                onClick = onClick,
                testTag = TestTags.CANCEL_SCHEDULE_ACTION,
                enabled = enabled,
            )
        ComposeAction.SCHEDULE ->
            ToolbarIconAction(
                iconResId = R.drawable.ic_baseline_access_time_24px,
                contentDescription = stringResource(R.string.schedule_send),
                onClick = onClick,
                testTag = TestTags.SCHEDULE_SEND_ACTION,
                enabled = enabled,
            )
        ComposeAction.ATTACH ->
            ToolbarIconAction(
                iconResId = R.drawable.ic_baseline_attachment_24px,
                contentDescription = stringResource(R.string.attach_file),
                onClick = onClick,
                testTag = TestTags.ATTACH_ACTION,
                enabled = enabled,
            )
        ComposeAction.SAVE_DRAFT ->
            ToolbarIconAction(
                iconResId = R.drawable.ic_baseline_save_24px,
                contentDescription = stringResource(R.string.save_draft),
                onClick = onClick,
                testTag = TestTags.SAVE_DRAFT_ACTION,
                enabled = enabled,
            )
        ComposeAction.SEND ->
            ToolbarIconAction(
                iconResId = R.drawable.ic_baseline_send_24px,
                contentDescription = stringResource(R.string.send),
                onClick = onClick,
                testTag = TestTags.SEND_ACTION,
                enabled = enabled,
            )
    }
}

/** Asks for the day, then the time (24 hour, to the minute), to send a scheduled draft. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleSendPicker(
    onPicked: (Date) -> Unit,
    onDismiss: () -> Unit,
) {
    var pickedDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    val dateMillis = pickedDateMillis
    if (dateMillis == null) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = remember { System.currentTimeMillis() })
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                DialogTextButton(
                    text = stringResource(R.string.ok),
                    onClick = { datePickerState.selectedDateMillis?.let { pickedDateMillis = it } },
                    testTag = TestTags.DATE_PICKER_CONFIRM,
                )
            },
            dismissButton = { DialogTextButton(stringResource(R.string.cancel), onDismiss, TestTags.DATE_PICKER_DISMISS) },
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        stringResource(R.string.schedule_send_date_title),
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                    )
                },
            )
        }
    } else {
        val now = remember { Calendar.getInstance() }
        val timePickerState =
            rememberTimePickerState(
                initialHour = now.get(Calendar.HOUR_OF_DAY),
                initialMinute = now.get(Calendar.MINUTE),
                is24Hour = true,
            )
        FormDialog(
            title = stringResource(R.string.schedule_send_time_title),
            confirmText = stringResource(R.string.ok),
            onConfirm = { onPicked(scheduledSendTime(dateMillis, timePickerState.hour, timePickerState.minute)) },
            onDismiss = onDismiss,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                TimePicker(state = timePickerState, modifier = Modifier.testTag(TestTags.TIME_PICKER))
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun ComposeEmailMessageContentPreview() {
    EmailExampleTheme {
        ComposeEmailMessageContent(
            uiState =
                ComposeUiState(
                    mode = ComposeMode.REPLY,
                    sender = EmailMessage.EmailAddress("shopping@example.com", "Shopping"),
                    isMessageLoaded = true,
                    aliasesEnabled = true,
                    content =
                        MessageContent(
                            to = "orders@store.example.com",
                            subject = "Re: Your order has shipped",
                            body = "Thanks!\n\n---------------\n\nYour order is on its way.",
                        ),
                    attachments = listOf(EmailAttachment("receipt.pdf", "receipt", "application/pdf", false, ByteArray(24_000))),
                    isEncrypted = true,
                ),
            onNavigateBack = {},
            onAction = {},
            onSendFromSelected = {},
            onToChange = {},
            onCcChange = {},
            onBccChange = {},
            onSubjectChange = {},
            onBodyChange = {},
            onOpenAttachment = {},
            onRemoveAttachment = {},
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun ComposeScheduledDraftPreview() {
    EmailExampleTheme {
        ComposeEmailMessageContent(
            uiState =
                ComposeUiState(
                    mode = ComposeMode.DRAFT,
                    sender = EmailMessage.EmailAddress("shopping@example.com"),
                    isMessageLoaded = true,
                    isScheduled = true,
                    content = MessageContent(to = "friend@example.com, other@example.com", subject = "Plans", body = "See you then."),
                ),
            onNavigateBack = {},
            onAction = {},
            onSendFromSelected = {},
            onToChange = {},
            onCcChange = {},
            onBccChange = {},
            onSubjectChange = {},
            onBodyChange = {},
            onOpenAttachment = {},
            onRemoveAttachment = {},
        )
    }
}
