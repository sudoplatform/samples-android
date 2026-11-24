/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.ui.TestTags
import com.sudoplatform.emailexample.ui.components.InPlaceLoading
import com.sudoplatform.emailexample.ui.components.LoadingDialog
import com.sudoplatform.emailexample.ui.components.MessageDialog
import com.sudoplatform.emailexample.ui.components.RetryDialog
import com.sudoplatform.emailexample.ui.components.SampleTopAppBar
import com.sudoplatform.emailexample.ui.components.SectionHeader
import com.sudoplatform.emailexample.ui.components.ToolbarIconAction
import com.sudoplatform.emailexample.ui.theme.EmailExampleTheme
import com.sudoplatform.emailexample.ui.theme.PrimaryText
import com.sudoplatform.emailexample.ui.theme.SecondaryText
import com.sudoplatform.sudoemail.types.Direction
import com.sudoplatform.sudoemail.types.EmailAttachment
import com.sudoplatform.sudoemail.types.EmailMessage
import com.sudoplatform.sudoemail.types.EmailMessageWithBody
import com.sudoplatform.sudoemail.types.EncryptionStatus
import com.sudoplatform.sudoemail.types.State
import java.text.DateFormat
import java.util.Date

/** Attachment names longer than this are shortened in the list. */
private const val MAX_FILE_NAME_LENGTH = 30

/**
 * Shows an email message: its date, sender, recipients, subject, body and attachments.
 *
 * Links From:
 *  - Email Messages: the user taps a message.
 *  - A tapped notification for the message.
 *
 * Links To:
 *  - Compose: the user taps Reply or Forward.
 *  - Email Messages: once the sender is blocked, or if the message couldn't be loaded.
 *  - A viewer app: the user taps an attachment.
 */
@Composable
fun ReadEmailMessageScreen(
    onNavigateBack: () -> Unit,
    onReply: (messageId: String) -> Unit,
    onForward: (messageId: String) -> Unit,
    viewModel: ReadEmailMessageViewModel = viewModel(factory = ReadEmailMessageViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val noViewerMessage = stringResource(R.string.open_attachment_failure)

    LaunchedEffect(viewModel) {
        viewModel.attachments.collect { file ->
            val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(file.uri, file.mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                viewModel.reportAttachmentError(noViewerMessage)
            }
        }
    }

    ReadEmailMessageContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onReply = { uiState.message?.let { onReply(it.id) } },
        onForward = { uiState.message?.let { onForward(it.id) } },
        onBlock = viewModel::blockSender,
        onOpenAttachment = viewModel::openAttachment,
    )

    if (uiState.isBlocking) {
        LoadingDialog(stringResource(R.string.blocking_address))
    }
    if (uiState.senderBlocked) {
        MessageDialog(
            title = stringResource(R.string.success),
            confirmText = stringResource(R.string.ok),
            onConfirm = onNavigateBack,
            onDismissRequest = onNavigateBack,
        )
    }
    uiState.blockError?.let { message ->
        RetryDialog(
            title = stringResource(R.string.block_address_failure),
            message = message,
            onRetry = viewModel::blockSender,
            onDismiss = viewModel::dismissBlockError,
        )
    }
    uiState.loadError?.let { message ->
        RetryDialog(
            title = stringResource(R.string.read_email_message_failure),
            message = message,
            onRetry = viewModel::load,
            onDismiss = {
                viewModel.dismissLoadError()
                if (uiState.message == null) onNavigateBack()
            },
        )
    }
    uiState.attachmentError?.let { message ->
        MessageDialog(
            title = stringResource(R.string.open_attachment_failure_title),
            message = message,
            confirmText = stringResource(R.string.ok),
            onConfirm = viewModel::dismissAttachmentError,
            onDismissRequest = viewModel::dismissAttachmentError,
        )
    }
}

@Composable
private fun ReadEmailMessageContent(
    uiState: ReadEmailMessageUiState,
    onNavigateBack: () -> Unit,
    onReply: () -> Unit,
    onForward: () -> Unit,
    onBlock: () -> Unit,
    onOpenAttachment: (EmailAttachment) -> Unit,
) {
    val message = uiState.message
    val actionsEnabled = message != null && !uiState.isBlocking
    Scaffold(
        topBar = {
            SampleTopAppBar(
                title = stringResource(R.string.read_email_message),
                onNavigateBack = onNavigateBack,
                actions = {
                    ToolbarIconAction(
                        iconResId = R.drawable.ic_baseline_block_24px,
                        contentDescription = stringResource(R.string.block_sender),
                        onClick = onBlock,
                        testTag = TestTags.BLOCK_ACTION,
                        enabled = actionsEnabled && message?.from?.isNotEmpty() == true,
                    )
                    ToolbarIconAction(
                        iconResId = R.drawable.ic_baseline_reply_24px,
                        contentDescription = stringResource(R.string.reply),
                        onClick = onReply,
                        testTag = TestTags.REPLY_ACTION,
                        enabled = actionsEnabled,
                    )
                    ToolbarIconAction(
                        iconResId = R.drawable.ic_baseline_forward_24px,
                        contentDescription = stringResource(R.string.forward),
                        onClick = onForward,
                        testTag = TestTags.FORWARD_ACTION,
                        enabled = actionsEnabled,
                    )
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        if (message == null) {
            if (uiState.isLoading) {
                InPlaceLoading(stringResource(R.string.reading), modifier = Modifier.padding(innerPadding))
            }
            return@Scaffold
        }
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                Text(
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(message.createdAt),
                    color = SecondaryText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.End).testTag(TestTags.MESSAGE_DATE_TEXT),
                )
                AddressField(
                    label = stringResource(R.string.from),
                    addresses = message.from.take(1),
                    testTag = TestTags.MESSAGE_FROM_TEXT,
                )
                AddressField(label = stringResource(R.string.to), addresses = message.to, testTag = TestTags.MESSAGE_TO_TEXT)
                AddressField(label = stringResource(R.string.cc), addresses = message.cc, testTag = TestTags.MESSAGE_CC_TEXT)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    message.subject ?: stringResource(R.string.no_subject),
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.testTag(TestTags.MESSAGE_SUBJECT_TEXT),
                )
                uiState.body?.let { body ->
                    val text = remember(body) { if (body.isHtml) AnnotatedString.fromHtml(body.body) else AnnotatedString(body.body) }
                    SelectionContainer(modifier = Modifier.padding(top = 16.dp)) {
                        Text(
                            text,
                            color = PrimaryText,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.testTag(TestTags.MESSAGE_BODY_TEXT),
                        )
                    }
                }
            }
            val attachments = uiState.body?.attachments.orEmpty()
            if (attachments.isNotEmpty()) {
                SectionHeader(stringResource(R.string.attachments))
                attachments.forEach { attachment ->
                    AttachmentRow(attachment = attachment, onClick = { onOpenAttachment(attachment) })
                }
            }
        }
    }
}

/** A label and its addresses, one per line, or the label alone when there are none. */
@Composable
private fun AddressField(
    label: String,
    addresses: List<EmailMessage.EmailAddress>,
    testTag: String,
) {
    Row(modifier = Modifier.padding(top = 4.dp)) {
        Text(label, color = SecondaryText, style = MaterialTheme.typography.bodyMedium)
        Text(
            addresses.joinToString("\n", transform = ::formatAddressForDisplay),
            color = PrimaryText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).testTag(testTag),
        )
    }
}

/** An attachment's type icon, name and size, which opens it when tapped. */
@Composable
internal fun AttachmentRow(
    attachment: EmailAttachment,
    onClick: () -> Unit,
) {
    val icon =
        when {
            attachment.mimeType.contains("image/") -> R.drawable.ic_baseline_attachment_image_24px
            attachment.mimeType.contains("video/") -> R.drawable.ic_baseline_attachment_media_24px
            attachment.mimeType.contains("application/pdf") -> R.drawable.ic_baseline_attachment_pdf_24px
            else -> R.drawable.ic_baseline_attachment_file_24px
        }
    val name =
        if (attachment.fileName.length > MAX_FILE_NAME_LENGTH) {
            attachment.fileName.take(MAX_FILE_NAME_LENGTH) + "…"
        } else {
            attachment.fileName
        }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .padding(bottom = 2.dp)
                .fillMaxWidth()
                .background(Color.White)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .testTag(TestTags.ATTACHMENT_ROW),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = SecondaryText)
        Text(
            name,
            color = PrimaryText,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )
        Text(
            stringResource(R.string.file_size, (attachment.data.size / 1000).toString()),
            color = SecondaryText,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun ReadEmailMessageContentPreview() {
    val message =
        EmailMessage(
            id = "1",
            owner = "owner",
            owners = emptyList(),
            emailAddressId = "address",
            folderId = "inbox",
            direction = Direction.INBOUND,
            state = State.RECEIVED,
            version = 1,
            sortDate = Date(),
            createdAt = Date(),
            updatedAt = Date(),
            size = 0.0,
            from = listOf(EmailMessage.EmailAddress("orders@store.example.com", "Store")),
            to = listOf(EmailMessage.EmailAddress("shopping@example.com")),
            cc = listOf(EmailMessage.EmailAddress("friend@example.com")),
            subject = "Your order has shipped",
            hasAttachments = true,
            encryptionStatus = EncryptionStatus.UNENCRYPTED,
        )
    val body =
        EmailMessageWithBody(
            id = "1",
            body = "<p>Your order is on its way.</p><p><b>Thanks</b> for shopping with us.</p>",
            isHtml = true,
            attachments = listOf(EmailAttachment("receipt.pdf", "receipt", "application/pdf", false, ByteArray(24_000))),
            inlineAttachments = emptyList(),
        )
    EmailExampleTheme {
        ReadEmailMessageContent(
            uiState = ReadEmailMessageUiState(message = message, body = body),
            onNavigateBack = {},
            onReply = {},
            onForward = {},
            onBlock = {},
            onOpenAttachment = {},
        )
    }
}
