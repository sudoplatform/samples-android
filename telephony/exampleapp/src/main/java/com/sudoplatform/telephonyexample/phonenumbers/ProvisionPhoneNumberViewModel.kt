/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.phonenumbers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import com.sudoplatform.sudoprofiles.exceptions.SudoProfileException
import com.sudoplatform.sudotelephony.SudoTelephonyClient
import com.sudoplatform.sudotelephony.SudoTelephonyClient.PhoneNumberException
import com.sudoplatform.sudotelephony.SudoTelephonyClient.PhoneNumberSearchException
import com.sudoplatform.sudotelephony.types.AvailablePhoneNumber
import com.sudoplatform.sudotelephony.types.PhoneNumberSearch
import com.sudoplatform.sudotelephony.types.inputs.ProvisionPhoneNumberInput
import com.sudoplatform.sudotelephony.types.inputs.SearchAvailablePhoneNumbersInput
import com.sudoplatform.telephonyexample.App
import com.sudoplatform.telephonyexample.sudos.displayLabel
import com.sudoplatform.telephonyexample.sudos.findSudo
import com.sudoplatform.telephonyexample.ui.countryName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID

/** Why the area code field is showing an error. */
enum class AreaCodeError {
    /** The user searched without a three-digit area code. */
    REQUIRED,

    /** The service doesn't recognise the area code in the selected country. */
    NOT_IN_USE,
}

/** An outcome the user is told about in a dialog, after which they can carry on. */
enum class ProvisionNotice {
    NOT_ENTITLED,
    LIMIT_REACHED,
    NUMBER_UNAVAILABLE,
    SEARCH_EXPIRED,
}

data class ProvisionPhoneNumberUiState(
    val sudoLabel: String = "",
    val isLoadingCountries: Boolean = true,
    /** ISO 3166-1 alpha-2 codes, sorted by country name. */
    val countries: List<String> = emptyList(),
    val selectedCountry: String? = null,
    val areaCode: String = "",
    val areaCodeError: AreaCodeError? = null,
    val isSearching: Boolean = false,
    /** The latest search, or null before the first search or after the criteria change. */
    val search: PhoneNumberSearch? = null,
    /** A number the user tapped, awaiting confirmation before it is provisioned. */
    val pendingProvision: AvailablePhoneNumber? = null,
    val isProvisioning: Boolean = false,
    val notice: ProvisionNotice? = null,
    val countriesError: String? = null,
    val searchError: String? = null,
    val provisionError: String? = null,
)

/** Emitted once a number has been provisioned, so the screen can close. */
data object PhoneNumberProvisioned

/**
 * Searches for available phone numbers in a supported country and area code, and provisions one
 * for a Sudo.
 */
class ProvisionPhoneNumberViewModel(
    private val sudoId: String,
    private val sudoProfilesClient: SudoProfilesClient,
    private val telephonyClient: SudoTelephonyClient,
) : ViewModel() {
    companion object {
        const val SUDO_ID_ARG = "sudoId"

        private const val SEARCH_LIMIT = 10
        private const val AREA_CODE_LENGTH = 3
        private const val DEFAULT_COUNTRY = "US"

        val Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as App
                    ProvisionPhoneNumberViewModel(
                        sudoId = checkNotNull(createSavedStateHandle().get<String>(SUDO_ID_ARG)),
                        sudoProfilesClient = app.sudoProfilesClient,
                        telephonyClient = app.sudoTelephonyClient,
                    )
                }
            }
    }

    /** A provisioning request, kept so that a retry repeats it exactly. */
    private data class ProvisionAttempt(
        val searchId: String,
        val phoneNumber: AvailablePhoneNumber,
        val clientRefId: String,
    )

    private val _uiState = MutableStateFlow(ProvisionPhoneNumberUiState())
    val uiState: StateFlow<ProvisionPhoneNumberUiState> = _uiState.asStateFlow()

    private val _provisioned = Channel<PhoneNumberProvisioned>(Channel.BUFFERED)
    val provisioned: Flow<PhoneNumberProvisioned> = _provisioned.receiveAsFlow()

    private var sudo: Sudo? = null
    private var lastAttempt: ProvisionAttempt? = null

    init {
        loadSudo()
        loadSupportedCountries()
    }

    /** Phone numbers can only be searched for and provisioned in the countries the service supports. */
    fun loadSupportedCountries() {
        _uiState.update { it.copy(isLoadingCountries = true, countriesError = null) }
        viewModelScope.launch {
            try {
                val countries = withContext(Dispatchers.IO) { telephonyClient.getSupportedCountries() }.sortedBy(::countryName)
                _uiState.update {
                    it.copy(
                        isLoadingCountries = false,
                        countries = countries,
                        selectedCountry = DEFAULT_COUNTRY.takeIf(countries::contains) ?: countries.firstOrNull(),
                    )
                }
            } catch (e: SudoTelephonyClient.SupportedCountriesException) {
                _uiState.update { it.copy(isLoadingCountries = false, countriesError = e.describe()) }
            }
        }
    }

    fun selectCountry(countryCode: String) {
        _uiState.update { it.copy(selectedCountry = countryCode, search = null, areaCodeError = null) }
    }

    fun updateAreaCode(areaCode: String) {
        val digits = areaCode.filter(Char::isDigit).take(AREA_CODE_LENGTH)
        _uiState.update { it.copy(areaCode = digits, search = null, areaCodeError = null) }
    }

    /**
     * Search for numbers in the selected country and area code. The service keeps the search for
     * a short time and only provisions numbers that came from it, so a new search replaces the
     * previous results.
     */
    fun search() {
        val state = _uiState.value
        val countryCode = state.selectedCountry ?: return
        if (state.areaCode.length != AREA_CODE_LENGTH) {
            _uiState.update { it.copy(areaCodeError = AreaCodeError.REQUIRED) }
            return
        }
        _uiState.update { it.copy(isSearching = true, searchError = null, areaCodeError = null) }
        viewModelScope.launch {
            try {
                val search =
                    withContext(Dispatchers.IO) {
                        telephonyClient.searchAvailablePhoneNumbers(
                            SearchAvailablePhoneNumbersInput(
                                countryCode = countryCode,
                                areaCode = state.areaCode,
                                limit = SEARCH_LIMIT,
                            ),
                        )
                    }
                _uiState.update { it.copy(isSearching = false, search = search) }
            } catch (e: PhoneNumberSearchException) {
                _uiState.update {
                    when (e) {
                        is PhoneNumberSearchException.InvalidAreaCodeException ->
                            it.copy(isSearching = false, search = null, areaCodeError = AreaCodeError.NOT_IN_USE)
                        is PhoneNumberSearchException.InsufficientEntitlementsException ->
                            it.copy(isSearching = false, notice = ProvisionNotice.NOT_ENTITLED)
                        else -> it.copy(isSearching = false, searchError = e.describe())
                    }
                }
            }
        }
    }

    /**
     * Ask the user to confirm provisioning [phoneNumber]. A search that has expired can no longer be
     * provisioned from, so it is run again instead.
     */
    fun requestProvision(phoneNumber: AvailablePhoneNumber) {
        val search = _uiState.value.search ?: return
        if (search.expiresAt.before(Date())) {
            _uiState.update { it.copy(notice = ProvisionNotice.SEARCH_EXPIRED) }
            search()
            return
        }
        _uiState.update { it.copy(pendingProvision = phoneNumber) }
    }

    fun cancelProvision() {
        _uiState.update { it.copy(pendingProvision = null) }
    }

    fun confirmProvision() {
        val state = _uiState.value
        val phoneNumber = state.pendingProvision ?: return
        val search = state.search ?: return
        _uiState.update { it.copy(pendingProvision = null) }
        provision(ProvisionAttempt(search.id, phoneNumber, clientRefId = UUID.randomUUID().toString()))
    }

    /**
     * Repeat the last attempt with the same client reference. If the first attempt did reach the
     * service, it returns the number already provisioned rather than ordering another.
     */
    fun retryProvision() {
        lastAttempt?.let(::provision)
    }

    fun dismissNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    fun dismissCountriesError() {
        _uiState.update { it.copy(countriesError = null) }
    }

    fun dismissSearchError() {
        _uiState.update { it.copy(searchError = null) }
    }

    fun dismissProvisionError() {
        _uiState.update { it.copy(provisionError = null) }
    }

    private fun provision(attempt: ProvisionAttempt) {
        lastAttempt = attempt
        _uiState.update { it.copy(isProvisioning = true, provisionError = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // The ownership proof is a short-lived token from the Sudo service showing that
                    // the user owns the Sudo. Provisioning with it makes the Sudo the number's owner.
                    val ownershipProof =
                        sudoProfilesClient.getOwnershipProof(
                            sudo ?: sudoProfilesClient.findSudo(sudoId),
                            SudoTelephonyClient.OWNERSHIP_PROOF_AUDIENCE,
                        )
                    // The SDK generates the key pair that seals the number's messages and keeps the
                    // private key on this device.
                    telephonyClient.provisionPhoneNumber(
                        ProvisionPhoneNumberInput(
                            searchId = attempt.searchId,
                            phoneNumber = attempt.phoneNumber.phoneNumber,
                            ownershipProofToken = ownershipProof,
                            clientRefId = attempt.clientRefId,
                        ),
                    )
                }
                _provisioned.send(PhoneNumberProvisioned)
            } catch (e: SudoProfileException) {
                _uiState.update { it.copy(provisionError = e.describe()) }
            } catch (e: PhoneNumberException) {
                handleProvisionFailure(e)
            } finally {
                _uiState.update { it.copy(isProvisioning = false) }
            }
        }
    }

    /**
     * The carrier doesn't hold searched numbers, so one can be taken before it is provisioned, and
     * the search itself expires. Both are resolved by searching again.
     */
    private fun handleProvisionFailure(e: PhoneNumberException) {
        when (e) {
            is PhoneNumberException.PhoneNumberUnavailableException,
            is PhoneNumberException.PhoneNumberNotInSearchException,
            -> {
                _uiState.update { it.copy(notice = ProvisionNotice.NUMBER_UNAVAILABLE) }
                search()
            }
            is PhoneNumberException.PhoneNumberSearchExpiredException -> {
                _uiState.update { it.copy(notice = ProvisionNotice.SEARCH_EXPIRED) }
                search()
            }
            is PhoneNumberException.InsufficientEntitlementsException ->
                _uiState.update { it.copy(notice = ProvisionNotice.LIMIT_REACHED) }
            else -> _uiState.update { it.copy(provisionError = e.describe()) }
        }
    }

    private fun loadSudo() {
        viewModelScope.launch {
            try {
                val found = withContext(Dispatchers.IO) { sudoProfilesClient.findSudo(sudoId) }
                sudo = found
                _uiState.update { it.copy(sudoLabel = found.displayLabel) }
            } catch (e: SudoProfileException) {
                // Looked up again when provisioning, where a failure is reported.
            }
        }
    }

    private fun Throwable.describe(): String = localizedMessage ?: toString()
}
