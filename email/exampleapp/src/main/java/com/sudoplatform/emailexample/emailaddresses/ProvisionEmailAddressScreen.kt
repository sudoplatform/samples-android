/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaddresses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.ui.TestTags
import com.sudoplatform.emailexample.ui.components.ItemCell
import com.sudoplatform.emailexample.ui.components.LoadingDialog
import com.sudoplatform.emailexample.ui.components.RetryDialog
import com.sudoplatform.emailexample.ui.components.SampleTopAppBar
import com.sudoplatform.emailexample.ui.components.ToolbarTextAction
import com.sudoplatform.emailexample.ui.components.whiteOutlinedTextFieldColors
import com.sudoplatform.emailexample.ui.theme.EmailExampleTheme
import com.sudoplatform.emailexample.ui.theme.NegativeMessage
import com.sudoplatform.emailexample.ui.theme.PositiveMessage
import com.sudoplatform.emailexample.ui.theme.PrimaryText
import com.sudoplatform.emailexample.ui.theme.SecondaryText

/**
 * Presents a form so that a user can provision an email address for a Sudo. The address is
 * checked for availability as the user types its local part.
 *
 * Links From:
 *  - Email Addresses: the user taps "+ Create Email Address".
 *
 * Links To:
 *  - Email Addresses: once the address is provisioned.
 */
@Composable
fun ProvisionEmailAddressScreen(
    onEmailAddressProvisioned: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ProvisionEmailAddressViewModel = viewModel(factory = ProvisionEmailAddressViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.provisioned.collect { onEmailAddressProvisioned() }
    }

    ProvisionEmailAddressContent(
        uiState = uiState,
        onLocalPartChange = viewModel::updateLocalPart,
        onDisplayNameChange = viewModel::updateDisplayName,
        onProvision = viewModel::provision,
        onNavigateBack = onNavigateBack,
    )

    if (uiState.isProvisioning) {
        LoadingDialog(stringResource(R.string.provisioning_email_address))
    }
    uiState.provisionError?.let { message ->
        RetryDialog(
            title = stringResource(R.string.provision_email_address_failure),
            message = message,
            onRetry = viewModel::provision,
            onDismiss = viewModel::dismissProvisionError,
        )
    }
}

@Composable
private fun ProvisionEmailAddressContent(
    uiState: ProvisionEmailAddressUiState,
    onLocalPartChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onProvision: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val docUrl = stringResource(R.string.provision_email_address_doc_url)
    Scaffold(
        topBar = {
            SampleTopAppBar(
                title = stringResource(R.string.provision_email_address),
                onNavigateBack = onNavigateBack,
                actions = {
                    ToolbarTextAction(
                        text = stringResource(R.string.create),
                        onClick = onProvision,
                        enabled = uiState.availability is AddressAvailability.Available && !uiState.isProvisioning,
                        testTag = TestTags.PROVISION_EMAIL_ADDRESS_ACTION,
                    )
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            OutlinedTextField(
                value = uiState.localPart,
                onValueChange = onLocalPartChange,
                enabled = !uiState.isProvisioning,
                singleLine = true,
                label = { Text(stringResource(R.string.enter_address_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                colors = whiteOutlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth().testTag(TestTags.LOCAL_PART_FIELD),
            )
            OutlinedTextField(
                value = uiState.displayName,
                onValueChange = onDisplayNameChange,
                enabled = !uiState.isProvisioning,
                singleLine = true,
                label = { Text(stringResource(R.string.optional_enter_display_name_label)) },
                colors = whiteOutlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag(TestTags.DISPLAY_NAME_FIELD),
            )
            uiState.availability?.let { AvailabilityText(it) }
            ItemCell(text = stringResource(R.string.sudo), modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    uiState.sudoLabel,
                    color = PrimaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag(TestTags.SUDO_LABEL_TEXT),
                )
            }
            Text(
                stringResource(R.string.provision_email_address_learn_more),
                color = SecondaryText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 24.dp),
            )
            TextButton(
                onClick = { uriHandler.openUri(docUrl) },
                modifier = Modifier.fillMaxWidth().height(55.dp).testTag(TestTags.LEARN_MORE_BUTTON),
            ) {
                Text(stringResource(R.string.learn_more).uppercase(), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** The address that will be provisioned, or why it can't be. */
@Composable
private fun AvailabilityText(availability: AddressAvailability) {
    Column(modifier = Modifier.padding(top = 8.dp, start = 4.dp)) {
        if (availability is AddressAvailability.Available) {
            Text(
                availability.emailAddress,
                color = PrimaryText,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag(TestTags.AVAILABLE_ADDRESS_TEXT),
            )
        }
        Text(
            when (availability) {
                is AddressAvailability.Available -> stringResource(R.string.email_address_available)
                AddressAvailability.Unavailable -> stringResource(R.string.email_address_unavailable)
                is AddressAvailability.Failed -> availability.message
            },
            color = if (availability is AddressAvailability.Available) PositiveMessage else NegativeMessage,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag(TestTags.AVAILABILITY_TEXT),
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun ProvisionEmailAddressContentPreview() {
    EmailExampleTheme {
        ProvisionEmailAddressContent(
            uiState =
                ProvisionEmailAddressUiState(
                    sudoLabel = "Shopping",
                    localPart = "shopping",
                    availability = AddressAvailability.Available("shopping@example.com"),
                ),
            onLocalPartChange = {},
            onDisplayNameChange = {},
            onProvision = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun ProvisionEmailAddressContentUnavailablePreview() {
    EmailExampleTheme {
        ProvisionEmailAddressContent(
            uiState =
                ProvisionEmailAddressUiState(
                    sudoLabel = "Shopping",
                    localPart = "taken",
                    availability = AddressAvailability.Unavailable,
                ),
            onLocalPartChange = {},
            onDisplayNameChange = {},
            onProvision = {},
            onNavigateBack = {},
        )
    }
}
