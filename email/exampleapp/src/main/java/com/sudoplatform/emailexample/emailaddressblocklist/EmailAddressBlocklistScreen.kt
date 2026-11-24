/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaddressblocklist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.ui.TestTags
import com.sudoplatform.emailexample.ui.components.LoadingDialog
import com.sudoplatform.emailexample.ui.components.RetryDialog
import com.sudoplatform.emailexample.ui.components.SampleTopAppBar
import com.sudoplatform.emailexample.ui.components.ToolbarIconAction
import com.sudoplatform.emailexample.ui.theme.EmailExampleTheme
import com.sudoplatform.emailexample.ui.theme.PrimaryText
import com.sudoplatform.emailexample.ui.theme.SecondaryText
import com.sudoplatform.sudoemail.types.BlockedEmailAddressAction
import com.sudoplatform.sudoemail.types.UnsealedBlockedAddress
import com.sudoplatform.sudoemail.types.UnsealedBlockedAddressStatus

/**
 * Lists the addresses the user has blocked. The user ticks addresses and taps the bin to unblock
 * them.
 *
 * Links From:
 *  - Email Messages: the user chooses "Blocked Addresses" from the menu.
 */
@Composable
fun EmailAddressBlocklistScreen(
    onNavigateBack: () -> Unit,
    viewModel: EmailAddressBlocklistViewModel = viewModel(factory = EmailAddressBlocklistViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EmailAddressBlocklistContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onRefresh = viewModel::load,
        onToggle = viewModel::toggleSelection,
        onUnblock = viewModel::unblockSelected,
    )

    if (uiState.isUnblocking) {
        LoadingDialog(stringResource(R.string.unblocking_addresses))
    }
    uiState.listError?.let { message ->
        RetryDialog(
            title = stringResource(R.string.list_blocked_addresses_failure),
            message = message,
            onRetry = viewModel::load,
            onDismiss = viewModel::dismissListError,
        )
    }
    uiState.unblockError?.let { message ->
        RetryDialog(
            title = stringResource(R.string.unblock_addresses_failure),
            message = message,
            onRetry = viewModel::unblockSelected,
            onDismiss = viewModel::dismissUnblockError,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmailAddressBlocklistContent(
    uiState: EmailAddressBlocklistUiState,
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit,
    onToggle: (hashedBlockedValue: String) -> Unit,
    onUnblock: () -> Unit,
) {
    Scaffold(
        topBar = {
            SampleTopAppBar(
                title = stringResource(R.string.blocked_addresses),
                onNavigateBack = onNavigateBack,
                actions = {
                    ToolbarIconAction(
                        iconResId = R.drawable.ic_baseline_delete_24px,
                        contentDescription = stringResource(R.string.unblock),
                        onClick = onUnblock,
                        testTag = TestTags.UNBLOCK_ACTION,
                        enabled = uiState.selected.isNotEmpty() && !uiState.isUnblocking,
                    )
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading && uiState.blockedAddresses.isEmpty(),
            onRefresh = onRefresh,
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(24.dp).testTag(TestTags.BLOCKED_ADDRESSES_LIST)) {
                items(uiState.blockedAddresses, key = { it.hashedBlockedValue }) { blockedAddress ->
                    BlockedAddressRow(
                        address = blockedAddress.address,
                        checked = blockedAddress.hashedBlockedValue in uiState.selected,
                        onToggle = { onToggle(blockedAddress.hashedBlockedValue) },
                    )
                }
                if (uiState.hasLoaded && !uiState.isLoading && uiState.blockedAddresses.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.no_blocked_addresses),
                            color = SecondaryText,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                        )
                    }
                }
            }
        }
    }
}

/** A blocked address with a checkbox, ticked by tapping the row or the box. */
@Composable
private fun BlockedAddressRow(
    address: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .padding(bottom = 2.dp)
                .fillMaxWidth()
                .height(60.dp)
                .background(Color.White)
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp)
                .testTag(TestTags.BLOCKED_ADDRESS_ROW),
    ) {
        Text(
            address,
            color = PrimaryText,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            modifier = Modifier.testTag(TestTags.BLOCKED_ADDRESS_CHECKBOX),
        )
    }
}

private fun previewBlockedAddress(address: String) =
    UnsealedBlockedAddress(
        hashedBlockedValue = address,
        address = address,
        status = UnsealedBlockedAddressStatus.Completed,
        action = BlockedEmailAddressAction.DROP,
        emailAddressId = null,
    )

@Preview(showSystemUi = true)
@Composable
private fun EmailAddressBlocklistContentPreview() {
    EmailExampleTheme {
        EmailAddressBlocklistContent(
            uiState =
                EmailAddressBlocklistUiState(
                    hasLoaded = true,
                    blockedAddresses = listOf(previewBlockedAddress("spam@example.com"), previewBlockedAddress("ads@example.org")),
                    selected = setOf("spam@example.com"),
                ),
            onNavigateBack = {},
            onRefresh = {},
            onToggle = {},
            onUnblock = {},
        )
    }
}
