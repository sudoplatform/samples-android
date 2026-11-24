/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.userdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.ui.TestTags
import com.sudoplatform.emailexample.ui.components.CopyButton
import com.sudoplatform.emailexample.ui.components.RetryDialog
import com.sudoplatform.emailexample.ui.components.SampleTopAppBar
import com.sudoplatform.emailexample.ui.components.SectionFooter
import com.sudoplatform.emailexample.ui.components.SectionHeader
import com.sudoplatform.emailexample.ui.copyToClipboard
import com.sudoplatform.emailexample.ui.theme.EmailExampleTheme
import com.sudoplatform.emailexample.ui.theme.PrimaryText
import com.sudoplatform.emailexample.ui.theme.SecondaryText
import java.text.DateFormat
import java.util.Date

/**
 * The signed in user's account identifiers, each copyable, e.g. for a support request, then how
 * they signed in, the email key options chosen at sign in, and when their session started and
 * expires.
 *
 * Links From:
 *  - Sudos: the user taps "Signed in with".
 */
@Composable
fun UserDetailsScreen(
    onNavigateBack: () -> Unit,
    viewModel: UserDetailsViewModel = viewModel(factory = UserDetailsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    UserDetailsContent(
        uiState = uiState,
        onCopy = { name, value -> context.copyToClipboard(name, value, context.getString(R.string.value_copied, name)) },
        onNavigateBack = onNavigateBack,
    )

    uiState.loadError?.let { message ->
        RetryDialog(
            title = stringResource(R.string.load_user_details_failure),
            message = message,
            onRetry = viewModel::load,
            onDismiss = viewModel::dismissLoadError,
        )
    }
}

@Composable
private fun UserDetailsContent(
    uiState: UserDetailsUiState,
    onCopy: (name: String, value: String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = { SampleTopAppBar(title = stringResource(R.string.user_details), onNavigateBack = onNavigateBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val notAvailable = stringResource(R.string.not_available)
        val dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

        fun Date?.formatted() = this?.let(dateFormat::format) ?: notAvailable

        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        ) {
            SectionHeader(stringResource(R.string.account))
            DetailCell(
                name = stringResource(R.string.user_id),
                value = uiState.userId,
                onCopy = onCopy,
                valueModifier = Modifier.testTag(TestTags.USER_ID_VALUE),
            )
            DetailCell(name = stringResource(R.string.subject), value = uiState.subject, onCopy = onCopy)
            SectionFooter(stringResource(R.string.user_details_copy_explanation))

            SectionHeader(stringResource(R.string.session), modifier = Modifier.padding(top = 16.dp))
            DetailCell(
                name = stringResource(R.string.signed_in_with),
                value = stringResource(if (uiState.usedFsso) R.string.federated_signin else R.string.test_registration),
            )
            DetailCell(
                name = stringResource(R.string.key_config),
                value =
                    stringResource(
                        if (uiState.enforceSingletonKeyPair) R.string.single_key_for_email else R.string.key_per_email_address,
                    ),
            )
            DetailCell(
                name = stringResource(R.string.key_removal),
                value =
                    stringResource(
                        when {
                            uiState.enforceSingletonKeyPair -> R.string.key_removal_never
                            uiState.allowCallerManagedKeys -> R.string.key_removal_caller_managed
                            else -> R.string.key_removal_on_deprovision
                        },
                    ),
            )
            DetailCell(name = stringResource(R.string.signed_in), value = uiState.signedInAt.formatted())
            DetailCell(name = stringResource(R.string.id_token_expires), value = uiState.idTokenExpiresAt.formatted())
            DetailCell(name = stringResource(R.string.refresh_token_expires), value = uiState.refreshTokenExpiresAt.formatted())
            SectionFooter(stringResource(R.string.session_explanation))
        }
    }
}

/**
 * A white list row with a label and its value underneath. With [onCopy] the value is shown in
 * monospace with a button to copy it.
 */
@Composable
private fun DetailCell(
    name: String,
    value: String?,
    onCopy: ((name: String, value: String) -> Unit)? = null,
    valueModifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .padding(bottom = 2.dp)
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = SecondaryText, style = MaterialTheme.typography.bodyMedium)
            Text(
                value ?: stringResource(R.string.not_available),
                color = PrimaryText,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = if (onCopy != null) FontFamily.Monospace else null,
                modifier = valueModifier,
            )
        }
        if (onCopy != null && value != null) {
            CopyButton(contentDescription = stringResource(R.string.copy_value, name), onClick = { onCopy(name, value) })
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun UserDetailsContentPreview() {
    EmailExampleTheme {
        UserDetailsContent(
            uiState =
                UserDetailsUiState(
                    userId = "5fbc915ab340c40076859bc1",
                    subject = "2b6e7f4c-8d1a-4c3e-9f50-1a2b3c4d5e6f",
                    usedFsso = true,
                    enforceSingletonKeyPair = true,
                    signedInAt = Date(1_793_746_224_000),
                    idTokenExpiresAt = Date(1_793_749_824_000),
                    refreshTokenExpiresAt = Date(1_793_832_624_000),
                ),
            onCopy = { _, _ -> },
            onNavigateBack = {},
        )
    }
}
