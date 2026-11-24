/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.sudos

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.ui.TestTags
import com.sudoplatform.emailexample.ui.components.LoadingDialog
import com.sudoplatform.emailexample.ui.components.RetryDialog
import com.sudoplatform.emailexample.ui.components.SampleTopAppBar
import com.sudoplatform.emailexample.ui.components.ToolbarTextAction
import com.sudoplatform.emailexample.ui.theme.EmailExampleTheme

/**
 * Presents a form so that a user can create a Sudo.
 *
 * Links From:
 *  - Sudos: the user taps "+ Create Sudo".
 *
 * Links To:
 *  - Email Addresses: once the Sudo is created, to provision its first address.
 */
@Composable
fun CreateSudoScreen(
    onSudoCreated: (sudoId: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: CreateSudoViewModel = viewModel(factory = CreateSudoViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.created.collect { onSudoCreated(it.sudoId) }
    }

    CreateSudoContent(
        uiState = uiState,
        onLabelChange = viewModel::updateLabel,
        onCreate = viewModel::createSudo,
        onNavigateBack = onNavigateBack,
    )

    if (uiState.isBusy) {
        LoadingDialog(stringResource(R.string.creating_sudo))
    }
    uiState.createError?.let { message ->
        RetryDialog(
            title = stringResource(R.string.something_wrong),
            message = message,
            onRetry = viewModel::createSudo,
            onDismiss = viewModel::dismissCreateError,
        )
    }
}

@Composable
private fun CreateSudoContent(
    uiState: CreateSudoUiState,
    onLabelChange: (String) -> Unit,
    onCreate: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            SampleTopAppBar(
                title = stringResource(R.string.create_sudo),
                onNavigateBack = onNavigateBack,
                actions = {
                    ToolbarTextAction(
                        text = stringResource(R.string.create),
                        onClick = onCreate,
                        enabled = !uiState.isBusy,
                        testTag = TestTags.CREATE_SUDO_ACTION,
                    )
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        SudoLabelForm(
            label = uiState.label,
            onLabelChange = onLabelChange,
            enabled = !uiState.isBusy,
            showLabelRequired = uiState.showLabelRequired,
            onSubmit = onCreate,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun CreateSudoContentPreview() {
    EmailExampleTheme {
        CreateSudoContent(uiState = CreateSudoUiState(), onLabelChange = {}, onCreate = {}, onNavigateBack = {})
    }
}
