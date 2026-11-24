/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.entitlements

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.ui.TestTags
import com.sudoplatform.emailexample.ui.components.InPlaceLoading
import com.sudoplatform.emailexample.ui.components.ItemCell
import com.sudoplatform.emailexample.ui.components.RetryDialog
import com.sudoplatform.emailexample.ui.components.SampleTopAppBar
import com.sudoplatform.emailexample.ui.components.SectionFooter
import com.sudoplatform.emailexample.ui.components.SectionHeader
import com.sudoplatform.emailexample.ui.theme.EmailExampleTheme
import com.sudoplatform.emailexample.ui.theme.PrimaryText
import com.sudoplatform.emailexample.ui.theme.SecondaryText
import com.sudoplatform.sudoentitlements.types.Entitlement

/**
 * Shows the user's entitlement set, how much of each entitlement they and their Sudos have used,
 * and every entitlement's limit.
 *
 * Links From:
 *  - Sudos: the user taps "Entitlements".
 */
@Composable
fun EntitlementsScreen(
    onNavigateBack: () -> Unit,
    viewModel: EntitlementsViewModel = viewModel(factory = EntitlementsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EntitlementsContent(uiState = uiState, onRefresh = viewModel::load, onNavigateBack = onNavigateBack)

    uiState.loadError?.let { message ->
        RetryDialog(
            title = stringResource(R.string.load_entitlements_failure),
            message = message,
            onRetry = viewModel::load,
            onDismiss = viewModel::dismissLoadError,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntitlementsContent(
    uiState: EntitlementsUiState,
    onRefresh: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = { SampleTopAppBar(title = stringResource(R.string.entitlements), onNavigateBack = onNavigateBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        if (uiState.isLoading && !uiState.hasLoaded) {
            InPlaceLoading(stringResource(R.string.loading_entitlements), Modifier.padding(innerPadding))
            return@Scaffold
        }
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .testTag(TestTags.ENTITLEMENTS_LIST),
            ) {
                uiState.entitlementsSetName?.let { setName ->
                    item {
                        SectionHeader(stringResource(R.string.entitlement_set))
                        ItemCell(text = setName)
                    }
                }
                item { SectionHeader(stringResource(R.string.usage)) }
                if (uiState.hasLoaded && uiState.usage.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.nothing_used_yet),
                            color = SecondaryText,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                        )
                    }
                }
                items(uiState.usage, key = { "${it.name}|${it.sudo}" }) { usage ->
                    ItemCell(
                        text = usage.name,
                        subtitle = usage.sudo ?: stringResource(R.string.account),
                    ) {
                        Value(stringResource(R.string.usage_value, usage.consumed, usage.value))
                    }
                }
                item { SectionFooter(stringResource(R.string.usage_explanation)) }
                item { SectionHeader(stringResource(R.string.entitlements), modifier = Modifier.padding(top = 16.dp)) }
                items(uiState.entitlements, key = Entitlement::name) { entitlement ->
                    ItemCell(
                        text = entitlement.name,
                        subtitle = entitlement.description,
                        modifier = Modifier.testTag(TestTags.ENTITLEMENT_ROW),
                    ) {
                        Value(entitlement.value.toString())
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun Value(text: String) {
    Text(text, color = PrimaryText, style = MaterialTheme.typography.bodyMedium)
}

@Preview(showSystemUi = true)
@Composable
private fun EntitlementsContentPreview() {
    EmailExampleTheme {
        EntitlementsContent(
            uiState =
                EntitlementsUiState(
                    hasLoaded = true,
                    entitlementsSetName = "TEST",
                    entitlements =
                        listOf(
                            Entitlement("sudoplatform.sudo.max", "Maximum number of Sudos", 3),
                            Entitlement("sudoplatform.email.emailAddressUserEntitled", "Email addresses per user", 2),
                        ),
                    usage = listOf(EntitlementUsage("sudoplatform.sudo.max", sudo = null, consumed = 1, value = 3)),
                ),
            onRefresh = {},
            onNavigateBack = {},
        )
    }
}
