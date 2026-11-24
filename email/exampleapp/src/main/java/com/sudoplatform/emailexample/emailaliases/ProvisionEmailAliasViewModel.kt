/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaliases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sudoplatform.emailexample.App
import com.sudoplatform.emailexample.NavArgs
import com.sudoplatform.emailexample.emailaddresses.AddressAvailability
import com.sudoplatform.emailexample.emailaddresses.ProvisionEmailAddressViewModel
import com.sudoplatform.emailexample.sudos.displayLabel
import com.sudoplatform.emailexample.sudos.findSudo
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudoemail.types.EmailAddress
import com.sudoplatform.sudoemail.types.ListAPIResult
import com.sudoplatform.sudoemail.types.inputs.CheckEmailAddressAvailabilityInput
import com.sudoplatform.sudoemail.types.inputs.ListEmailAddressesForSudoIdInput
import com.sudoplatform.sudoemail.types.inputs.ProvisionEmailAliasInput
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudoprofiles.exceptions.SudoProfileException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Something the user is told in a dialog before they can carry on. */
enum class AliasProvisionNotice {
    /** An internal alias needs one of the Sudo's addresses to forward to, and it has none. */
    NO_EMAIL_ADDRESSES,
    NO_ALIAS_DOMAINS,
    ENTER_EXTERNAL_ADDRESS,
    INVALID_EXTERNAL_ADDRESS,
    ENTER_LOCAL_PART,
}

data class ProvisionEmailAliasUiState(
    /** True for an alias forwarding to an address outside the platform. */
    val external: Boolean = false,
    val sudoLabel: String = "",
    val isLoadingAddresses: Boolean = false,
    /** The Sudo's addresses, one of which an internal alias forwards to. */
    val emailAddresses: List<EmailAddress> = emptyList(),
    val selectedAddress: EmailAddress? = null,
    /** The address an external alias forwards to. */
    val externalAddress: String = "",
    val localPart: String = "",
    val aliasDomains: List<String> = emptyList(),
    val selectedDomain: String? = null,
    /** The result for the current local part and domain, or null until they have been checked. */
    val availability: AddressAvailability? = null,
    val isProvisioning: Boolean = false,
    val notice: AliasProvisionNotice? = null,
    val loadAddressesError: String? = null,
    val loadDomainsError: String? = null,
    val provisionError: String? = null,
) {
    /** False while provisioning, or when an internal alias has no address to forward to. */
    val isFormEnabled: Boolean get() = !isProvisioning && (external || emailAddresses.isNotEmpty())

    /** The alias is available and, for an internal alias, the user has picked its real address. */
    val canProvision: Boolean
        get() =
            availability is AddressAvailability.Available &&
                selectedDomain != null &&
                (external || selectedAddress != null) &&
                !isProvisioning
}

/** Emitted once the alias has been provisioned, so the screen can close. */
data object EmailAliasProvisioned

/**
 * Provisions an email alias that forwards to one of a Sudo's addresses or, when external, to an
 * address outside the platform. The alias address is checked for availability as the user types.
 */
class ProvisionEmailAliasViewModel(
    private val sudoId: String,
    external: Boolean,
    private val app: App,
) : ViewModel() {
    companion object {
        /** How long typing must pause before the alias is checked. */
        private const val CHECK_DELAY_MILLIS = 1000L

        val Factory =
            viewModelFactory {
                initializer {
                    val savedStateHandle = createSavedStateHandle()
                    ProvisionEmailAliasViewModel(
                        sudoId = checkNotNull(savedStateHandle.get<String>(NavArgs.SUDO_ID)),
                        external = savedStateHandle.get<Boolean>(NavArgs.EXTERNAL) ?: false,
                        app = this[APPLICATION_KEY] as App,
                    )
                }
            }
    }

    /** An alias address to check. */
    private data class AliasQuery(
        val localPart: String,
        val domain: String?,
    )

    private val emailClient: SudoEmailClient get() = app.sudoEmailClient

    private val _uiState = MutableStateFlow(ProvisionEmailAliasUiState(external = external))
    val uiState: StateFlow<ProvisionEmailAliasUiState> = _uiState.asStateFlow()

    private val _provisioned = Channel<EmailAliasProvisioned>(Channel.BUFFERED)
    val provisioned: Flow<EmailAliasProvisioned> = _provisioned.receiveAsFlow()

    private val queries = MutableStateFlow(AliasQuery("", null))
    private var sudo: Sudo? = null

    init {
        loadSudo()
        if (!external) {
            loadEmailAddresses()
        }
        loadAliasDomains()
        checkAvailabilityAsTyped()
    }

    fun selectAddress(emailAddress: EmailAddress) {
        _uiState.update { it.copy(selectedAddress = emailAddress) }
    }

    fun updateExternalAddress(externalAddress: String) {
        _uiState.update { it.copy(externalAddress = externalAddress) }
    }

    fun updateLocalPart(localPart: String) {
        _uiState.update { it.copy(localPart = localPart) }
        setQuery(queries.value.copy(localPart = localPart.trim()))
    }

    fun selectDomain(domain: String) {
        _uiState.update { it.copy(selectedDomain = domain) }
        setQuery(queries.value.copy(domain = domain))
    }

    /** Check the form, in the order the fields appear, then provision the alias. */
    fun provision() {
        val state = _uiState.value
        val realAddress =
            if (state.external) {
                val externalAddress = state.externalAddress.trim()
                when {
                    externalAddress.isEmpty() -> return showNotice(AliasProvisionNotice.ENTER_EXTERNAL_ADDRESS)
                    !isValidEmail(externalAddress) -> return showNotice(AliasProvisionNotice.INVALID_EXTERNAL_ADDRESS)
                }
                externalAddress
            } else {
                state.selectedAddress?.emailAddress
            }
        val localPart = state.localPart.trim()
        if (localPart.isEmpty()) return showNotice(AliasProvisionNotice.ENTER_LOCAL_PART)
        val domain = state.selectedDomain ?: return
        if (realAddress == null) return
        _uiState.update { it.copy(isProvisioning = true, provisionError = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val ownershipProof =
                        app.sudoProfilesClient.getOwnershipProof(
                            sudo ?: app.sudoProfilesClient.findSudo(sudoId),
                            ProvisionEmailAddressViewModel.EMAIL_AUDIENCE,
                        )
                    emailClient.provisionEmailAlias(
                        ProvisionEmailAliasInput(
                            aliasAddress = "$localPart@$domain",
                            realAddress = realAddress,
                            ownershipProofToken = ownershipProof,
                        ),
                    )
                }
                _provisioned.send(EmailAliasProvisioned)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(provisionError = e.describe()) }
            } finally {
                _uiState.update { it.copy(isProvisioning = false) }
            }
        }
    }

    /** List the Sudo's addresses, one of which an internal alias forwards to. */
    fun loadEmailAddresses() {
        _uiState.update { it.copy(isLoadingAddresses = true, loadAddressesError = null) }
        viewModelScope.launch {
            try {
                val result =
                    withContext(Dispatchers.IO) {
                        emailClient.listEmailAddressesForSudoId(ListEmailAddressesForSudoIdInput(sudoId = sudoId))
                    }
                val (addresses, failure) =
                    when (result) {
                        is ListAPIResult.Success -> result.result.items to null
                        is ListAPIResult.Partial ->
                            result.result.items to
                                result.result.failed
                                    .first()
                                    .cause
                    }
                _uiState.update {
                    it.copy(
                        isLoadingAddresses = false,
                        emailAddresses = addresses,
                        loadAddressesError = failure?.describe(),
                        notice = if (failure == null && addresses.isEmpty()) AliasProvisionNotice.NO_EMAIL_ADDRESSES else it.notice,
                    )
                }
            } catch (e: SudoEmailClient.EmailAddressException) {
                _uiState.update { it.copy(isLoadingAddresses = false, loadAddressesError = e.describe()) }
            }
        }
    }

    /** List the domains aliases can be created in, selecting the first. */
    fun loadAliasDomains() {
        _uiState.update { it.copy(loadDomainsError = null) }
        viewModelScope.launch {
            try {
                val domains = withContext(Dispatchers.IO) { emailClient.listEmailDomains().filter { it.isAliasDomain }.map { it.domain } }
                val selected = domains.firstOrNull()
                _uiState.update {
                    it.copy(
                        aliasDomains = domains,
                        selectedDomain = selected,
                        notice = if (domains.isEmpty()) AliasProvisionNotice.NO_ALIAS_DOMAINS else it.notice,
                    )
                }
                setQuery(queries.value.copy(domain = selected))
            } catch (e: SudoEmailClient.EmailAddressException) {
                _uiState.update { it.copy(loadDomainsError = e.describe()) }
            }
        }
    }

    fun dismissNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    fun dismissLoadAddressesError() {
        _uiState.update { it.copy(loadAddressesError = null) }
    }

    fun dismissLoadDomainsError() {
        _uiState.update { it.copy(loadDomainsError = null) }
    }

    fun dismissProvisionError() {
        _uiState.update { it.copy(provisionError = null) }
    }

    private fun showNotice(notice: AliasProvisionNotice) {
        _uiState.update { it.copy(notice = notice) }
    }

    /** A changed alias address clears the previous result until it has been checked. */
    private fun setQuery(query: AliasQuery) {
        if (query != queries.value) {
            _uiState.update { it.copy(availability = null) }
            queries.value = query
        }
    }

    /**
     * Check each alias address once typing pauses. A newer address cancels a check still running
     * for an older one.
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun checkAvailabilityAsTyped() {
        viewModelScope.launch {
            queries
                .debounce(CHECK_DELAY_MILLIS)
                .mapLatest { query ->
                    val domain = query.domain
                    query to if (query.localPart.isEmpty() || domain == null) null else checkAvailability(query.localPart, domain)
                }.collect { (query, availability) ->
                    if (query == queries.value) {
                        _uiState.update { it.copy(availability = availability) }
                    }
                }
        }
    }

    private suspend fun checkAvailability(
        localPart: String,
        domain: String,
    ): AddressAvailability =
        try {
            val available =
                withContext(Dispatchers.IO) {
                    emailClient.checkEmailAddressAvailability(CheckEmailAddressAvailabilityInput(listOf(localPart), listOf(domain)))
                }
            available.firstOrNull()?.let { AddressAvailability.Available(it) } ?: AddressAvailability.Unavailable
        } catch (e: SudoEmailClient.EmailAddressException) {
            AddressAvailability.Failed(e.describe())
        }

    private fun loadSudo() {
        viewModelScope.launch {
            try {
                val found = withContext(Dispatchers.IO) { app.sudoProfilesClient.findSudo(sudoId) }
                sudo = found
                _uiState.update { it.copy(sudoLabel = found.displayLabel) }
            } catch (e: SudoProfileException) {
                // Looked up again when provisioning, where a failure is reported.
            }
        }
    }

    private fun Throwable.describe(): String = localizedMessage ?: toString()
}
