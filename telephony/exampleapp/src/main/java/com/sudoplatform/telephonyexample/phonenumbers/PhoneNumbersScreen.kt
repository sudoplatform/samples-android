/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.phonenumbers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sudoplatform.sudotelephony.types.PhoneNumber
import com.sudoplatform.sudotelephony.types.PhoneNumberFailureReason
import com.sudoplatform.sudotelephony.types.PhoneNumberState
import com.sudoplatform.sudotelephony.types.PhoneNumberType
import com.sudoplatform.telephonyexample.R
import com.sudoplatform.telephonyexample.ui.TestTags
import com.sudoplatform.telephonyexample.ui.components.ItemCell
import com.sudoplatform.telephonyexample.ui.components.LoadingDialog
import com.sudoplatform.telephonyexample.ui.components.MenuButton
import com.sudoplatform.telephonyexample.ui.components.MessageDialog
import com.sudoplatform.telephonyexample.ui.components.RetryDialog
import com.sudoplatform.telephonyexample.ui.components.SampleTopAppBar
import com.sudoplatform.telephonyexample.ui.components.SwipeToDeleteRow
import com.sudoplatform.telephonyexample.ui.components.ToolbarIconAction
import com.sudoplatform.telephonyexample.ui.components.UnreadBadge
import com.sudoplatform.telephonyexample.ui.formatPhoneNumber
import com.sudoplatform.telephonyexample.ui.theme.SecondaryText
import com.sudoplatform.telephonyexample.ui.theme.TelephonyExampleTheme
import java.text.DateFormat
import java.util.Date

/**
 * Lists the phone numbers that belong to a Sudo.
 *
 * Links From:
 *  - Sudos: the user taps a Sudo.
 *  - Create Sudo: once the Sudo is created.
 *
 * Links To:
 *  - Get a Number: the user taps "+ Get a Number".
 *  - Messages: the user taps an active or suspended number.
 */
@Composable
fun PhoneNumbersScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProvision: () -> Unit,
    onNavigateToMessages: (phoneNumberId: String) -> Unit,
    viewModel: PhoneNumbersViewModel = viewModel(factory = PhoneNumbersViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showInfo by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val docUrl = stringResource(R.string.telephony_doc_url)

    // Refresh on every return to the screen, to pick up anything the subscription missed.
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    PhoneNumbersContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onInfo = { showInfo = true },
        onRefresh = viewModel::refresh,
        onProvision = onNavigateToProvision,
        onOpenPhoneNumber = onNavigateToMessages,
        onReleasePhoneNumber = viewModel::requestRelease,
    )

    uiState.pendingRelease?.let { phoneNumber ->
        MessageDialog(
            title = stringResource(R.string.release_phone_number_title),
            message = stringResource(R.string.release_phone_number_confirmation, formatPhoneNumber(phoneNumber)),
            confirmText = stringResource(R.string.release),
            onConfirm = viewModel::confirmRelease,
            dismissText = stringResource(R.string.cancel),
            onDismissRequest = viewModel::cancelRelease,
        )
    }
    uiState.listError?.let { message ->
        RetryDialog(
            title = stringResource(R.string.list_phone_numbers_failure),
            message = message,
            onRetry = viewModel::refresh,
            onDismiss = viewModel::dismissListError,
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
    if (showInfo) {
        MessageDialog(
            title = stringResource(R.string.what_is_a_phone_number),
            message = stringResource(R.string.phone_number_explanation),
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
private fun PhoneNumbersContent(
    uiState: PhoneNumbersUiState,
    onNavigateBack: () -> Unit,
    onInfo: () -> Unit,
    onRefresh: () -> Unit,
    onProvision: () -> Unit,
    onOpenPhoneNumber: (phoneNumberId: String) -> Unit,
    onReleasePhoneNumber: (phoneNumberId: String) -> Unit,
) {
    Scaffold(
        topBar = {
            SampleTopAppBar(
                title = stringResource(R.string.phone_numbers),
                onNavigateBack = onNavigateBack,
                actions = {
                    ToolbarIconAction(
                        iconResId = R.drawable.ic_info_24px,
                        contentDescription = stringResource(R.string.what_is_a_phone_number),
                        onClick = onInfo,
                        testTag = TestTags.INFO_ACTION,
                    )
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading && uiState.phoneNumbers.isEmpty(),
            onRefresh = onRefresh,
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                if (uiState.sudoLabel.isNotEmpty()) {
                    Text(
                        stringResource(R.string.sudo_heading, uiState.sudoLabel),
                        color = SecondaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(uiState.phoneNumbers, key = { it.id }) { phoneNumber ->
                        PhoneNumberRow(
                            phoneNumber = phoneNumber,
                            unreadCount = uiState.unreadCounts[phoneNumber.id] ?: 0,
                            onOpen = { onOpenPhoneNumber(phoneNumber.id) },
                            onRelease = { onReleasePhoneNumber(phoneNumber.id) },
                        )
                    }
                    // Inside the list so that an empty list can still be pulled to refresh.
                    if (uiState.hasLoaded && uiState.phoneNumbers.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.no_phone_numbers),
                                color = SecondaryText,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            )
                        }
                    }
                }
                if (uiState.includesUnownedNumbers) {
                    Text(
                        stringResource(R.string.unowned_phone_numbers_note),
                        color = SecondaryText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                MenuButton(
                    text = stringResource(R.string.provision_phone_number_button),
                    onClick = onProvision,
                    enabled = !uiState.isReleasing,
                    modifier = Modifier.testTag(TestTags.PROVISION_PHONE_NUMBER_BUTTON),
                )
            }
        }
    }
}

/**
 * A phone number, its state and how many unread messages it has. Messages can be read on an
 * active or suspended number, and only those can be released; a number that is provisioning or
 * failed can do neither.
 */
@Composable
private fun PhoneNumberRow(
    phoneNumber: PhoneNumber,
    unreadCount: Int,
    onOpen: () -> Unit,
    onRelease: () -> Unit,
) {
    val usable = phoneNumber.state == PhoneNumberState.Active || phoneNumber.state == PhoneNumberState.Suspended
    SwipeToDeleteRow(
        onDelete = onRelease,
        enabled = usable,
    ) {
        ItemCell(
            text = formatPhoneNumber(phoneNumber),
            subtitle = phoneNumber.stateText(),
            subtitleColor = if (phoneNumber.state == PhoneNumberState.Failed) MaterialTheme.colorScheme.error else SecondaryText,
            onClick = if (usable) onOpen else null,
            textLeadingContent = {
                if (unreadCount > 0) UnreadBadge(modifier = Modifier.padding(end = 6.dp), count = unreadCount)
            },
        ) {
            if (phoneNumber.state == PhoneNumberState.Provisioning) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun PhoneNumber.stateText(): String? =
    when (val state = state) {
        PhoneNumberState.Provisioning -> stringResource(R.string.provisioning)
        PhoneNumberState.Suspended ->
            gracePeriodEndsAt?.let { stringResource(R.string.suspended_until, DateFormat.getDateInstance().format(it)) }
                ?: stringResource(R.string.suspended)
        PhoneNumberState.Failed ->
            when (val reason = failureReason) {
                PhoneNumberFailureReason.NumberUnavailable -> stringResource(R.string.failed_number_unavailable)
                PhoneNumberFailureReason.CarrierError -> stringResource(R.string.failed_carrier_error)
                PhoneNumberFailureReason.OwnerDeleted -> stringResource(R.string.failed_owner_deleted)
                is PhoneNumberFailureReason.Unknown -> stringResource(R.string.failed_with_reason, reason.value)
                null -> stringResource(R.string.failed)
            }
        is PhoneNumberState.Unknown -> state.value
        PhoneNumberState.Active, PhoneNumberState.Released -> null
    }

/** Shows a number in its own country's national format. */
private fun formatPhoneNumber(phoneNumber: PhoneNumber): String = formatPhoneNumber(phoneNumber.phoneNumber, phoneNumber.countryCode)

@Preview(showSystemUi = true)
@Composable
private fun PhoneNumbersContentPreview() {
    TelephonyExampleTheme {
        PhoneNumbersContent(
            uiState =
                PhoneNumbersUiState(
                    sudoLabel = "Shopping",
                    hasLoaded = true,
                    phoneNumbers =
                        listOf(PhoneNumberState.Active, PhoneNumberState.Provisioning, PhoneNumberState.Suspended, PhoneNumberState.Failed)
                            .mapIndexed { index, state -> previewPhoneNumber("$index", "+1415555012$index", state) },
                    includesUnownedNumbers = true,
                    unreadCounts = mapOf("0" to 3),
                ),
            onNavigateBack = {},
            onInfo = {},
            onRefresh = {},
            onProvision = {},
            onOpenPhoneNumber = {},
            onReleasePhoneNumber = {},
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun PhoneNumbersContentEmptyPreview() {
    TelephonyExampleTheme {
        PhoneNumbersContent(
            uiState = PhoneNumbersUiState(sudoLabel = "Shopping", hasLoaded = true),
            onNavigateBack = {},
            onInfo = {},
            onRefresh = {},
            onProvision = {},
            onOpenPhoneNumber = {},
            onReleasePhoneNumber = {},
        )
    }
}

private fun previewPhoneNumber(
    id: String,
    number: String,
    state: PhoneNumberState,
) = PhoneNumber(
    id = id,
    owner = "owner",
    owners = emptyList(),
    identityId = "identity",
    keyRingId = "keyRing",
    keyIds = emptyList(),
    phoneNumber = number,
    countryCode = "US",
    numberType = PhoneNumberType.Local,
    areaCode = "415",
    state = state,
    gracePeriodEndsAt = if (state == PhoneNumberState.Suspended) Date() else null,
    version = 1,
    createdAt = Date(),
    updatedAt = Date(),
)
