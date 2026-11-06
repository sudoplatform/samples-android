/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.sudos

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
import com.sudoplatform.telephonyexample.R
import com.sudoplatform.telephonyexample.ui.TestTags
import com.sudoplatform.telephonyexample.ui.components.LoadingDialog
import com.sudoplatform.telephonyexample.ui.components.MessageDialog
import com.sudoplatform.telephonyexample.ui.components.RetryDialog
import com.sudoplatform.telephonyexample.ui.components.SampleTopAppBar
import com.sudoplatform.telephonyexample.ui.components.ToolbarTextAction
import com.sudoplatform.telephonyexample.ui.theme.TelephonyExampleTheme

/**
 * Presents a form so that a user can change a Sudo's label.
 *
 * Links From:
 *  - Sudos: the user taps the edit icon on a Sudo.
 *
 * Links To:
 *  - Sudos: once the Sudo is saved, or if it couldn't be loaded.
 */
@Composable
fun EditSudoScreen(
    onDone: () -> Unit,
    viewModel: EditSudoViewModel = viewModel(factory = EditSudoViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.saved.collect { onDone() }
    }

    EditSudoContent(
        uiState = uiState,
        onLabelChange = viewModel::updateLabel,
        onSave = viewModel::save,
        onNavigateBack = onDone,
    )

    if (uiState.isLoading) {
        LoadingDialog(stringResource(R.string.loading_sudo))
    }
    uiState.loadError?.let { message ->
        MessageDialog(
            title = stringResource(R.string.load_sudo_failure),
            message = message,
            confirmText = stringResource(R.string.ok),
            onConfirm = onDone,
            onDismissRequest = onDone,
        )
    }
    if (uiState.isBusy) {
        LoadingDialog(stringResource(R.string.saving_sudo))
    }
    uiState.saveError?.let { message ->
        RetryDialog(
            title = stringResource(R.string.something_wrong),
            message = message,
            onRetry = viewModel::save,
            onDismiss = viewModel::dismissSaveError,
        )
    }
}

@Composable
private fun EditSudoContent(
    uiState: EditSudoUiState,
    onLabelChange: (String) -> Unit,
    onSave: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            SampleTopAppBar(
                title = stringResource(R.string.edit_sudo),
                onNavigateBack = onNavigateBack,
                actions = {
                    ToolbarTextAction(
                        text = stringResource(R.string.save),
                        onClick = onSave,
                        enabled = uiState.isEditable,
                        testTag = TestTags.SAVE_SUDO_ACTION,
                    )
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        SudoLabelForm(
            label = uiState.label,
            onLabelChange = onLabelChange,
            enabled = uiState.isEditable,
            showLabelRequired = uiState.showLabelRequired,
            onSubmit = onSave,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

private val EditSudoUiState.isEditable get() = !isLoading && !isBusy && loadError == null

@Preview(showSystemUi = true)
@Composable
private fun EditSudoContentPreview() {
    TelephonyExampleTheme {
        EditSudoContent(
            uiState = EditSudoUiState(label = "Shopping", isLoading = false),
            onLabelChange = {},
            onSave = {},
            onNavigateBack = {},
        )
    }
}
