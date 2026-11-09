/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.phonenumbers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sudoplatform.sudotelephony.types.AvailablePhoneNumber
import com.sudoplatform.sudotelephony.types.PhoneNumberSearch
import com.sudoplatform.sudotelephony.types.PhoneNumberType
import com.sudoplatform.telephonyexample.R
import com.sudoplatform.telephonyexample.ui.TestTags
import com.sudoplatform.telephonyexample.ui.components.DropdownField
import com.sudoplatform.telephonyexample.ui.components.InPlaceLoading
import com.sudoplatform.telephonyexample.ui.components.ItemCell
import com.sudoplatform.telephonyexample.ui.components.LoadingDialog
import com.sudoplatform.telephonyexample.ui.components.MessageDialog
import com.sudoplatform.telephonyexample.ui.components.RetryDialog
import com.sudoplatform.telephonyexample.ui.components.SampleTopAppBar
import com.sudoplatform.telephonyexample.ui.components.ToolbarTextAction
import com.sudoplatform.telephonyexample.ui.components.whiteOutlinedTextFieldColors
import com.sudoplatform.telephonyexample.ui.countryName
import com.sudoplatform.telephonyexample.ui.formatPhoneNumber
import com.sudoplatform.telephonyexample.ui.theme.SecondaryText
import com.sudoplatform.telephonyexample.ui.theme.TelephonyExampleTheme
import java.text.DateFormat
import java.util.Date

/**
 * Presents a search for available phone numbers so that a user can provision one for a Sudo.
 *
 * Links From:
 *  - Phone Numbers: the user taps "+ Get a Number".
 *
 * Links To:
 *  - Phone Numbers: once the number is provisioned.
 */
@Composable
fun ProvisionPhoneNumberScreen(
    onPhoneNumberProvisioned: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ProvisionPhoneNumberViewModel = viewModel(factory = ProvisionPhoneNumberViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.provisioned.collect { onPhoneNumberProvisioned() }
    }

    ProvisionPhoneNumberContent(
        uiState = uiState,
        onCountrySelected = viewModel::selectCountry,
        onAreaCodeChange = viewModel::updateAreaCode,
        onSearch = viewModel::search,
        onSelectPhoneNumber = viewModel::requestProvision,
        onNavigateBack = onNavigateBack,
    )

    uiState.pendingProvision?.let { phoneNumber ->
        MessageDialog(
            title = stringResource(R.string.provision_phone_number_title, formatPhoneNumber(phoneNumber)),
            message =
                uiState.sudoLabel.takeIf { it.isNotEmpty() }?.let {
                    stringResource(
                        R.string.provision_phone_number_confirmation,
                        it,
                    )
                },
            confirmText = stringResource(R.string.provision),
            onConfirm = viewModel::confirmProvision,
            dismissText = stringResource(R.string.cancel),
            onDismissRequest = viewModel::cancelProvision,
        )
    }
    uiState.notice?.let { notice ->
        MessageDialog(
            title = stringResource(notice.titleResId()),
            message = stringResource(notice.messageResId()),
            confirmText = stringResource(R.string.ok),
            onConfirm = viewModel::dismissNotice,
            onDismissRequest = viewModel::dismissNotice,
        )
    }
    uiState.countriesError?.let { message ->
        RetryDialog(
            title = stringResource(R.string.list_supported_countries_failure),
            message = message,
            onRetry = viewModel::loadSupportedCountries,
            onDismiss = {
                viewModel.dismissCountriesError()
                onNavigateBack()
            },
        )
    }
    uiState.searchError?.let { message ->
        RetryDialog(
            title = stringResource(R.string.search_phone_numbers_failure),
            message = message,
            onRetry = viewModel::search,
            onDismiss = viewModel::dismissSearchError,
        )
    }
    uiState.provisionError?.let { message ->
        RetryDialog(
            title = stringResource(R.string.provision_phone_number_failure),
            message = message,
            onRetry = viewModel::retryProvision,
            onDismiss = viewModel::dismissProvisionError,
        )
    }
    if (uiState.isProvisioning) {
        LoadingDialog(stringResource(R.string.provisioning_phone_number))
    }
}

@Composable
private fun ProvisionPhoneNumberContent(
    uiState: ProvisionPhoneNumberUiState,
    onCountrySelected: (String) -> Unit,
    onAreaCodeChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSelectPhoneNumber: (AvailablePhoneNumber) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val isBusy = uiState.isSearching || uiState.isProvisioning
    Scaffold(
        topBar = {
            SampleTopAppBar(
                title = stringResource(R.string.provision_phone_number),
                onNavigateBack = onNavigateBack,
                actions = {
                    ToolbarTextAction(
                        text = stringResource(R.string.search),
                        onClick = onSearch,
                        enabled = uiState.selectedCountry != null && !isBusy,
                        testTag = TestTags.SEARCH_ACTION,
                    )
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        if (uiState.isLoadingCountries) {
            InPlaceLoading(stringResource(R.string.loading_supported_countries), modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            if (uiState.sudoLabel.isNotEmpty()) {
                Text(
                    stringResource(R.string.sudo_heading, uiState.sudoLabel),
                    color = SecondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            DropdownField(
                label = stringResource(R.string.country),
                options = uiState.countries,
                selected = uiState.selectedCountry,
                optionText = { stringResource(R.string.country_cell, countryName(it), it) },
                onSelected = onCountrySelected,
                enabled = !isBusy,
                colors = whiteOutlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.areaCode,
                onValueChange = onAreaCodeChange,
                enabled = !isBusy,
                singleLine = true,
                label = { Text(stringResource(R.string.area_code)) },
                isError = uiState.areaCodeError != null,
                supportingText = { Text(areaCodeSupportingText(uiState)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                colors = whiteOutlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag(TestTags.AREA_CODE_FIELD),
            )
            val search = uiState.search
            when {
                uiState.isSearching ->
                    Box(modifier = Modifier.weight(1f)) {
                        InPlaceLoading(stringResource(R.string.searching_phone_numbers))
                    }
                search != null -> SearchResults(search = search, onSelectPhoneNumber = onSelectPhoneNumber)
                else -> ProvisionLearnMore()
            }
        }
    }
}

@Composable
private fun areaCodeSupportingText(uiState: ProvisionPhoneNumberUiState): String =
    when (uiState.areaCodeError) {
        AreaCodeError.REQUIRED -> stringResource(R.string.area_code_required)
        AreaCodeError.NOT_IN_USE ->
            stringResource(R.string.area_code_not_in_use, uiState.selectedCountry?.let(::countryName).orEmpty())
        null -> stringResource(R.string.area_code_hint)
    }

@Composable
private fun SearchResults(
    search: PhoneNumberSearch,
    onSelectPhoneNumber: (AvailablePhoneNumber) -> Unit,
) {
    if (search.results.isEmpty()) {
        Text(
            stringResource(R.string.no_available_phone_numbers),
            color = SecondaryText,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        )
        return
    }
    Column {
        Text(
            stringResource(R.string.available_phone_numbers),
            color = SecondaryText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
            items(search.results, key = { it.phoneNumber }) { phoneNumber ->
                ItemCell(
                    text = formatPhoneNumber(phoneNumber),
                    subtitle = availablePhoneNumberSubtitle(phoneNumber),
                    onClick = { onSelectPhoneNumber(phoneNumber) },
                    modifier = Modifier.testTag(TestTags.AVAILABLE_PHONE_NUMBER_ROW),
                )
            }
        }
        Text(
            stringResource(R.string.search_expires_at, DateFormat.getTimeInstance(DateFormat.SHORT).format(search.expiresAt)),
            color = SecondaryText,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun availablePhoneNumberSubtitle(phoneNumber: AvailablePhoneNumber): String? {
    val place = listOfNotNull(phoneNumber.locality, phoneNumber.region).joinToString(", ")
    val type =
        when (val numberType = phoneNumber.numberType) {
            PhoneNumberType.Local -> stringResource(R.string.phone_number_type_local)
            is PhoneNumberType.Unknown -> numberType.value
        }
    return listOf(place, type).filter { it.isNotEmpty() }.joinToString(" · ").ifEmpty { null }
}

@Composable
private fun ProvisionLearnMore() {
    val uriHandler = LocalUriHandler.current
    val docUrl = stringResource(R.string.telephony_doc_url)
    Column {
        Text(
            stringResource(R.string.provision_phone_number_learn_more),
            color = Color.Black,
            fontSize = 18.sp,
            modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 40.dp),
        )
        TextButton(
            onClick = { uriHandler.openUri(docUrl) },
            modifier = Modifier.fillMaxWidth().height(55.dp),
        ) {
            Text(stringResource(R.string.learn_more).uppercase(), style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun ProvisionNotice.titleResId(): Int =
    when (this) {
        ProvisionNotice.NOT_ENTITLED -> R.string.not_entitled_title
        ProvisionNotice.LIMIT_REACHED -> R.string.phone_number_limit_title
        ProvisionNotice.NUMBER_UNAVAILABLE -> R.string.number_unavailable_title
        ProvisionNotice.SEARCH_EXPIRED -> R.string.search_expired_title
    }

private fun ProvisionNotice.messageResId(): Int =
    when (this) {
        ProvisionNotice.NOT_ENTITLED -> R.string.not_entitled_message
        ProvisionNotice.LIMIT_REACHED -> R.string.phone_number_limit_message
        ProvisionNotice.NUMBER_UNAVAILABLE -> R.string.number_unavailable_message
        ProvisionNotice.SEARCH_EXPIRED -> R.string.search_expired_message
    }

/** Shows an available number in its own country's national format. */
private fun formatPhoneNumber(phoneNumber: AvailablePhoneNumber): String =
    formatPhoneNumber(phoneNumber.phoneNumber, phoneNumber.countryCode)

@Preview(showSystemUi = true)
@Composable
private fun ProvisionPhoneNumberContentPreview() {
    TelephonyExampleTheme {
        ProvisionPhoneNumberContent(
            uiState =
                ProvisionPhoneNumberUiState(
                    sudoLabel = "Shopping",
                    isLoadingCountries = false,
                    countries = listOf("CA", "US"),
                    selectedCountry = "US",
                    areaCode = "415",
                    search =
                        PhoneNumberSearch(
                            id = "search",
                            countryCode = "US",
                            areaCode = "415",
                            results =
                                (0..2).map {
                                    AvailablePhoneNumber("+1415555010$it", "US", PhoneNumberType.Local, "San Francisco", "CA")
                                },
                            expiresAt = Date(),
                            owner = "owner",
                            version = 1,
                            createdAt = Date(),
                            updatedAt = Date(),
                        ),
                ),
            onCountrySelected = {},
            onAreaCodeChange = {},
            onSearch = {},
            onSelectPhoneNumber = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun ProvisionPhoneNumberContentInitialPreview() {
    TelephonyExampleTheme {
        ProvisionPhoneNumberContent(
            uiState = ProvisionPhoneNumberUiState(isLoadingCountries = false, countries = listOf("US"), selectedCountry = "US"),
            onCountrySelected = {},
            onAreaCodeChange = {},
            onSearch = {},
            onSelectPhoneNumber = {},
            onNavigateBack = {},
        )
    }
}
