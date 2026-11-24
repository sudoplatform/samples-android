/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaliases

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.emailaddresses.AddressAvailability
import com.sudoplatform.emailexample.ui.TestTags
import com.sudoplatform.emailexample.ui.components.DropdownField
import com.sudoplatform.emailexample.ui.components.InPlaceLoading
import com.sudoplatform.emailexample.ui.components.ItemCell
import com.sudoplatform.emailexample.ui.components.LoadingDialog
import com.sudoplatform.emailexample.ui.components.MessageDialog
import com.sudoplatform.emailexample.ui.components.RetryDialog
import com.sudoplatform.emailexample.ui.components.SampleTopAppBar
import com.sudoplatform.emailexample.ui.components.ToolbarTextAction
import com.sudoplatform.emailexample.ui.components.whiteOutlinedTextFieldColors
import com.sudoplatform.emailexample.ui.theme.EmailExampleTheme
import com.sudoplatform.emailexample.ui.theme.NegativeMessage
import com.sudoplatform.emailexample.ui.theme.PositiveMessage
import com.sudoplatform.emailexample.ui.theme.PrimaryText
import com.sudoplatform.sudoemail.types.EmailAddress
import java.util.Date

/**
 * Presents a form so that a user can create an email alias for a Sudo. An internal alias forwards
 * to one of the Sudo's addresses; an external one forwards to an address outside the platform,
 * which the user verifies afterwards from the aliases list.
 *
 * Links From:
 *  - Email Addresses: the user taps "+ Create Email Alias" or "+ Create External Email Alias".
 *
 * Links To:
 *  - Email Addresses: once the alias is provisioned.
 */
@Composable
fun ProvisionEmailAliasScreen(
    onEmailAliasProvisioned: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ProvisionEmailAliasViewModel = viewModel(factory = ProvisionEmailAliasViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.provisioned.collect { onEmailAliasProvisioned() }
    }

    ProvisionEmailAliasContent(
        uiState = uiState,
        onAddressSelected = viewModel::selectAddress,
        onExternalAddressChange = viewModel::updateExternalAddress,
        onLocalPartChange = viewModel::updateLocalPart,
        onDomainSelected = viewModel::selectDomain,
        onProvision = viewModel::provision,
        onNavigateBack = onNavigateBack,
    )

    uiState.notice?.let { notice ->
        MessageDialog(
            title = stringResource(notice.titleResId()),
            confirmText = stringResource(R.string.ok),
            onConfirm = viewModel::dismissNotice,
            onDismissRequest = viewModel::dismissNotice,
        )
    }
    uiState.loadAddressesError?.let { message ->
        RetryDialog(
            title = stringResource(R.string.load_email_addresses_failure),
            message = message,
            onRetry = viewModel::loadEmailAddresses,
            onDismiss = viewModel::dismissLoadAddressesError,
        )
    }
    uiState.loadDomainsError?.let { message ->
        RetryDialog(
            title = stringResource(R.string.load_alias_domains_failure),
            message = message,
            onRetry = viewModel::loadAliasDomains,
            onDismiss = viewModel::dismissLoadDomainsError,
        )
    }
    uiState.provisionError?.let { message ->
        RetryDialog(
            title = stringResource(R.string.provision_email_alias_failure),
            message = message,
            onRetry = viewModel::provision,
            onDismiss = viewModel::dismissProvisionError,
        )
    }
    if (uiState.isProvisioning) {
        LoadingDialog(stringResource(R.string.provisioning_email_alias))
    }
}

@Composable
private fun ProvisionEmailAliasContent(
    uiState: ProvisionEmailAliasUiState,
    onAddressSelected: (EmailAddress) -> Unit,
    onExternalAddressChange: (String) -> Unit,
    onLocalPartChange: (String) -> Unit,
    onDomainSelected: (String) -> Unit,
    onProvision: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            SampleTopAppBar(
                title = stringResource(if (uiState.external) R.string.provision_external_email_alias else R.string.provision_email_alias),
                onNavigateBack = onNavigateBack,
                actions = {
                    ToolbarTextAction(
                        text = stringResource(R.string.create),
                        onClick = onProvision,
                        enabled = uiState.canProvision,
                        testTag = TestTags.PROVISION_EMAIL_ALIAS_ACTION,
                    )
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        if (uiState.isLoadingAddresses) {
            InPlaceLoading(stringResource(R.string.loading_email_addresses), modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }
        val enabled = uiState.isFormEnabled
        Column(modifier = Modifier.padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (uiState.external) {
                OutlinedTextField(
                    value = uiState.externalAddress,
                    onValueChange = onExternalAddressChange,
                    enabled = enabled,
                    singleLine = true,
                    label = { Text(stringResource(R.string.external_email_address_label)) },
                    placeholder = { Text(stringResource(R.string.enter_external_email_address)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    colors = whiteOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag(TestTags.EXTERNAL_ADDRESS_FIELD),
                )
            } else {
                DropdownField(
                    label = stringResource(R.string.real_email_address_label),
                    options = uiState.emailAddresses,
                    selected = uiState.selectedAddress,
                    optionText = { it.emailAddress },
                    onSelected = onAddressSelected,
                    enabled = enabled,
                    testTag = TestTags.REAL_ADDRESS_FIELD,
                    colors = whiteOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedTextField(
                value = uiState.localPart,
                onValueChange = onLocalPartChange,
                enabled = enabled,
                singleLine = true,
                label = { Text(stringResource(R.string.alias_local_part_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                colors = whiteOutlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag(TestTags.ALIAS_LOCAL_PART_FIELD),
            )
            DropdownField(
                label = stringResource(R.string.alias_domain_label),
                options = uiState.aliasDomains,
                selected = uiState.selectedDomain,
                optionText = { it },
                onSelected = onDomainSelected,
                enabled = enabled,
                testTag = TestTags.ALIAS_DOMAIN_FIELD,
                colors = whiteOutlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            uiState.availability?.let { availability ->
                Text(
                    when (availability) {
                        is AddressAvailability.Available -> stringResource(R.string.email_address_available)
                        AddressAvailability.Unavailable -> stringResource(R.string.email_address_unavailable)
                        is AddressAvailability.Failed -> availability.message
                    },
                    color = if (availability is AddressAvailability.Available) PositiveMessage else NegativeMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp).testTag(TestTags.AVAILABILITY_TEXT),
                )
            }
            ItemCell(text = stringResource(R.string.sudo), modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    uiState.sudoLabel,
                    color = PrimaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag(TestTags.SUDO_LABEL_TEXT),
                )
            }
        }
    }
}

private fun AliasProvisionNotice.titleResId(): Int =
    when (this) {
        AliasProvisionNotice.NO_EMAIL_ADDRESSES -> R.string.no_email_addresses_for_alias
        AliasProvisionNotice.NO_ALIAS_DOMAINS -> R.string.no_alias_domains_available
        AliasProvisionNotice.ENTER_EXTERNAL_ADDRESS -> R.string.enter_external_email_address
        AliasProvisionNotice.INVALID_EXTERNAL_ADDRESS -> R.string.invalid_email_format
        AliasProvisionNotice.ENTER_LOCAL_PART -> R.string.enter_alias_local_part
    }

@Composable
private fun ProvisionEmailAliasContentPreview(uiState: ProvisionEmailAliasUiState) {
    EmailExampleTheme {
        ProvisionEmailAliasContent(
            uiState = uiState,
            onAddressSelected = {},
            onExternalAddressChange = {},
            onLocalPartChange = {},
            onDomainSelected = {},
            onProvision = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun ProvisionEmailAliasContentInternalPreview() {
    val address =
        EmailAddress(
            id = "1",
            owner = "owner",
            owners = emptyList(),
            emailAddress = "shopping@example.com",
            size = 0.0,
            numberOfEmailMessages = 0,
            version = 1,
            createdAt = Date(),
            updatedAt = Date(),
            folders = emptyList(),
        )
    ProvisionEmailAliasContentPreview(
        ProvisionEmailAliasUiState(
            sudoLabel = "Shopping",
            emailAddresses = listOf(address),
            selectedAddress = address,
            localPart = "deals",
            aliasDomains = listOf("aliases.example.com"),
            selectedDomain = "aliases.example.com",
            availability = AddressAvailability.Available("deals@aliases.example.com"),
        ),
    )
}

@Preview(showSystemUi = true)
@Composable
private fun ProvisionEmailAliasContentExternalPreview() {
    ProvisionEmailAliasContentPreview(
        ProvisionEmailAliasUiState(
            external = true,
            sudoLabel = "Shopping",
            externalAddress = "me@example.org",
            localPart = "news",
            aliasDomains = listOf("aliases.example.com"),
            selectedDomain = "aliases.example.com",
            availability = AddressAvailability.Unavailable,
        ),
    )
}
