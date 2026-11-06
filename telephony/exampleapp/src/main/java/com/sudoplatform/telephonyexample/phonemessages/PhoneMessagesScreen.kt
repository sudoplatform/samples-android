/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.phonemessages

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sudoplatform.sudotelephony.types.PhoneMessage
import com.sudoplatform.sudotelephony.types.PhoneMessageDirection
import com.sudoplatform.sudotelephony.types.PhoneMessageType
import com.sudoplatform.telephonyexample.R
import com.sudoplatform.telephonyexample.ui.components.ItemCell
import com.sudoplatform.telephonyexample.ui.components.LoadingDialog
import com.sudoplatform.telephonyexample.ui.components.MessageDialog
import com.sudoplatform.telephonyexample.ui.components.RetryDialog
import com.sudoplatform.telephonyexample.ui.components.SampleTopAppBar
import com.sudoplatform.telephonyexample.ui.components.SwipeToDeleteRow
import com.sudoplatform.telephonyexample.ui.components.ToolbarTextAction
import com.sudoplatform.telephonyexample.ui.components.UnreadBadge
import com.sudoplatform.telephonyexample.ui.copyMessageText
import com.sudoplatform.telephonyexample.ui.formatPhoneNumber
import com.sudoplatform.telephonyexample.ui.theme.SecondaryText
import com.sudoplatform.telephonyexample.ui.theme.TelephonyExampleTheme
import java.text.DateFormat
import java.util.Date

/**
 * Lists the messages received on a phone number, newest first.
 *
 * Links From:
 *  - Phone Numbers: the user taps an active or suspended number.
 *
 * Links To:
 *  - Message: the user taps a message.
 *  - Phone Numbers: the user releases the number.
 */
@Composable
fun PhoneMessagesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMessage: (messageId: String) -> Unit,
    viewModel: PhoneMessagesViewModel = viewModel(factory = PhoneMessagesViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.released.collect { onNavigateBack() }
    }

    // Refresh on every return to the screen, to pick up anything the subscription missed.
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    PhoneMessagesContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onOpenMessage = onNavigateToMessage,
        onCopyMessage = { message -> context.copyMessageText(message.body) },
        onDeleteMessage = viewModel::requestDelete,
        onRelease = viewModel::requestRelease,
    )

    val phoneNumber = uiState.phoneNumber
    if (uiState.confirmRelease && phoneNumber != null) {
        MessageDialog(
            title = stringResource(R.string.release_phone_number_title),
            message =
                stringResource(
                    R.string.release_phone_number_confirmation,
                    formatPhoneNumber(phoneNumber.phoneNumber, phoneNumber.countryCode),
                ),
            confirmText = stringResource(R.string.release),
            onConfirm = viewModel::confirmRelease,
            dismissText = stringResource(R.string.cancel),
            onDismissRequest = viewModel::cancelRelease,
        )
    }
    uiState.releaseError?.let { message ->
        MessageDialog(
            title = stringResource(R.string.release_phone_number_failure),
            message = message,
            confirmText = stringResource(R.string.ok),
            onConfirm = viewModel::dismissReleaseError,
            onDismissRequest = viewModel::dismissReleaseError,
        )
    }
    if (uiState.isReleasing) {
        LoadingDialog(stringResource(R.string.releasing_phone_number))
    }

    uiState.listError?.let { message ->
        RetryDialog(
            title = stringResource(R.string.list_phone_messages_failure),
            message = message,
            onRetry = viewModel::refresh,
            onDismiss = viewModel::dismissListError,
        )
    }
    if (uiState.pendingDelete != null) {
        MessageDialog(
            title = stringResource(R.string.delete_phone_message_title),
            message = stringResource(R.string.delete_phone_message_confirmation),
            confirmText = stringResource(R.string.delete),
            onConfirm = viewModel::confirmDelete,
            dismissText = stringResource(R.string.cancel),
            onDismissRequest = viewModel::cancelDelete,
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneMessagesContent(
    uiState: PhoneMessagesUiState,
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenMessage: (messageId: String) -> Unit,
    onCopyMessage: (PhoneMessage) -> Unit,
    onDeleteMessage: (messageId: String) -> Unit,
    onRelease: () -> Unit,
) {
    val listState = rememberLazyListState()

    // Load the next page as the user nears the end of the list.
    LaunchedEffect(listState, uiState.hasMore) {
        if (!uiState.hasMore) return@LaunchedEffect
        // Keyed on the item count too, so a page that leaves the user at the end loads the next.
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) to layoutInfo.totalItemsCount
        }.collect { (lastVisible, total) ->
            if (lastVisible >= total - 3) onLoadMore()
        }
    }

    Scaffold(
        topBar = {
            SampleTopAppBar(
                title = uiState.phoneNumber?.let { formatPhoneNumber(it.phoneNumber, it.countryCode) } ?: stringResource(R.string.messages),
                onNavigateBack = onNavigateBack,
                actions = {
                    ToolbarTextAction(
                        text = stringResource(R.string.release),
                        onClick = onRelease,
                        enabled = uiState.phoneNumber != null && !uiState.isReleasing,
                    )
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when {
                uiState.keyMissing -> EmptyMessage(stringResource(R.string.phone_messages_key_missing), Modifier.padding(24.dp))
                else ->
                    PullToRefreshBox(
                        isRefreshing = uiState.isLoading && uiState.messages.isEmpty(),
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                        ) {
                            items(uiState.messages, key = { it.id }) { message ->
                                PhoneMessageRow(
                                    message = message,
                                    onOpen = { onOpenMessage(message.id) },
                                    onCopy = { onCopyMessage(message) },
                                    onDelete = { onDeleteMessage(message.id) },
                                )
                            }
                            if (uiState.hasLoaded && uiState.messages.isEmpty()) {
                                item { EmptyMessage(stringResource(R.string.no_phone_messages), Modifier.padding(vertical = 24.dp)) }
                            }
                            if (uiState.isLoadingMore) {
                                item {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }
}

@Composable
private fun EmptyMessage(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        color = SecondaryText,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * A message's sender, a preview of its text, and when it arrived. Unread messages are bold with
 * an unread indicator. A long press copies the text, and swiping it to the left asks to delete it.
 */
@Composable
private fun PhoneMessageRow(
    message: PhoneMessage,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    SwipeToDeleteRow(onDelete = onDelete) {
        ItemCell(
            text = message.remotePhoneNumber?.let(::formatPhoneNumber) ?: stringResource(R.string.unknown_sender),
            subtitle = message.preview(),
            emphasised = !message.seen,
            onClick = onOpen,
            onLongClick = if (message.body.isNotEmpty()) onCopy else null,
            textLeadingContent = {
                if (!message.seen) UnreadBadge(modifier = Modifier.padding(end = 6.dp))
            },
        ) {
            Text(
                DateUtils
                    .formatSameDayTime(
                        message.receivedAt.time,
                        System.currentTimeMillis(),
                        DateFormat.MEDIUM,
                        DateFormat.SHORT,
                    ).toString(),
                color = SecondaryText,
                fontWeight = if (message.seen) null else FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun PhoneMessage.preview(): String =
    when {
        body.isNotEmpty() -> body.replace('\n', ' ')
        mediaOmitted -> stringResource(R.string.media_omitted)
        else -> stringResource(R.string.no_text)
    }

@Preview(showSystemUi = true)
@Composable
private fun PhoneMessagesContentPreview() {
    TelephonyExampleTheme {
        PhoneMessagesContent(
            uiState =
                PhoneMessagesUiState(
                    hasLoaded = true,
                    messages =
                        listOf(
                            previewMessage("1", "+16505550199", "Your code is 123456", seen = false),
                            previewMessage("2", "+16505550142", "Running late, see you at six", seen = true),
                            previewMessage("3", null, "", seen = true, mediaOmitted = true),
                        ),
                ),
            onNavigateBack = {},
            onRefresh = {},
            onLoadMore = {},
            onOpenMessage = {},
            onCopyMessage = {},
            onDeleteMessage = {},
            onRelease = {},
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun PhoneMessagesContentKeyMissingPreview() {
    TelephonyExampleTheme {
        PhoneMessagesContent(
            uiState = PhoneMessagesUiState(keyMissing = true),
            onNavigateBack = {},
            onRefresh = {},
            onLoadMore = {},
            onOpenMessage = {},
            onCopyMessage = {},
            onDeleteMessage = {},
            onRelease = {},
        )
    }
}

internal fun previewMessage(
    id: String,
    from: String?,
    body: String,
    seen: Boolean,
    mediaOmitted: Boolean = false,
) = PhoneMessage(
    id = id,
    owner = "owner",
    owners = emptyList(),
    phoneNumberId = "phoneNumber",
    conversationId = "conversation-$from",
    type = PhoneMessageType.SMS,
    direction = PhoneMessageDirection.INBOUND,
    localPhoneNumber = "+14155550123",
    remotePhoneNumber = from,
    body = body,
    segmentCount = 1,
    mediaOmitted = mediaOmitted,
    seen = seen,
    receivedAt = Date(),
    version = 1,
    createdAt = Date(),
    updatedAt = Date(),
)
