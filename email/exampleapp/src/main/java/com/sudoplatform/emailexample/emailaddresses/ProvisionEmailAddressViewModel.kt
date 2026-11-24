/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaddresses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sudoplatform.emailexample.App
import com.sudoplatform.emailexample.NavArgs
import com.sudoplatform.emailexample.sudos.displayLabel
import com.sudoplatform.emailexample.sudos.findSudo
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudoemail.types.inputs.CheckEmailAddressAvailabilityInput
import com.sudoplatform.sudoemail.types.inputs.ProvisionEmailAddressInput
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

/** The outcome of checking whether an address can be provisioned. */
sealed interface AddressAvailability {
    /** [emailAddress] is free to provision. */
    data class Available(
        val emailAddress: String,
    ) : AddressAvailability

    data object Unavailable : AddressAvailability

    /** The check failed, for example because the local part isn't allowed. */
    data class Failed(
        val message: String,
    ) : AddressAvailability
}

data class ProvisionEmailAddressUiState(
    val sudoLabel: String = "",
    val localPart: String = "",
    val displayName: String = "",
    /** The result for the current local part, or null until it has been checked. */
    val availability: AddressAvailability? = null,
    val isProvisioning: Boolean = false,
    val provisionError: String? = null,
)

/** Emitted once the address has been provisioned, so the screen can close. */
data object EmailAddressProvisioned

/**
 * Checks addresses for availability as the user types a local part, and provisions one for a
 * Sudo.
 */
class ProvisionEmailAddressViewModel(
    private val sudoId: String,
    private val app: App,
) : ViewModel() {
    companion object {
        /** How long typing must pause before the address is checked. */
        private const val CHECK_DELAY_MILLIS = 1000L

        /** The audience of the ownership proof that makes a Sudo the owner of an address. */
        const val EMAIL_AUDIENCE = "sudoplatform.email.email-address"

        val Factory =
            viewModelFactory {
                initializer {
                    ProvisionEmailAddressViewModel(
                        sudoId = checkNotNull(createSavedStateHandle().get<String>(NavArgs.SUDO_ID)),
                        app = this[APPLICATION_KEY] as App,
                    )
                }
            }
    }

    private val emailClient: SudoEmailClient get() = app.sudoEmailClient

    private val _uiState = MutableStateFlow(ProvisionEmailAddressUiState())
    val uiState: StateFlow<ProvisionEmailAddressUiState> = _uiState.asStateFlow()

    private val _provisioned = Channel<EmailAddressProvisioned>(Channel.BUFFERED)
    val provisioned: Flow<EmailAddressProvisioned> = _provisioned.receiveAsFlow()

    private val localParts = MutableStateFlow("")
    private var sudo: Sudo? = null

    init {
        loadSudo()
        checkAvailabilityAsTyped()
    }

    /** The address is checked again once the user stops typing. */
    fun updateLocalPart(localPart: String) {
        val trimmed = localPart.trim()
        _uiState.update { it.copy(localPart = localPart, availability = it.availability.takeIf { trimmed == localParts.value }) }
        localParts.value = trimmed
    }

    fun updateDisplayName(displayName: String) {
        _uiState.update { it.copy(displayName = displayName) }
    }

    fun provision() {
        val state = _uiState.value
        val availability = state.availability as? AddressAvailability.Available ?: return
        _uiState.update { it.copy(isProvisioning = true, provisionError = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // The ownership proof is a short-lived token from the Sudo service showing that
                    // the user owns the Sudo. Provisioning with it makes the Sudo the address's owner.
                    val ownershipProof =
                        app.sudoProfilesClient.getOwnershipProof(sudo ?: app.sudoProfilesClient.findSudo(sudoId), EMAIL_AUDIENCE)
                    emailClient.provisionEmailAddress(
                        ProvisionEmailAddressInput(
                            emailAddress = availability.emailAddress,
                            ownershipProofToken = ownershipProof,
                            displayName = state.displayName.trim().ifBlank { null },
                        ),
                    )
                }
                _provisioned.send(EmailAddressProvisioned)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(provisionError = e.describe()) }
            } finally {
                _uiState.update { it.copy(isProvisioning = false) }
            }
        }
    }

    fun dismissProvisionError() {
        _uiState.update { it.copy(provisionError = null) }
    }

    /**
     * Check each local part once typing pauses. A newer local part cancels a check still running
     * for an older one.
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun checkAvailabilityAsTyped() {
        viewModelScope.launch {
            localParts
                .debounce(CHECK_DELAY_MILLIS)
                .mapLatest { localPart -> localPart to localPart.takeIf { it.isNotEmpty() }?.let { checkAvailability(it) } }
                .collect { (localPart, availability) ->
                    _uiState.update { state ->
                        if (state.localPart.trim() == localPart) state.copy(availability = availability) else state
                    }
                }
        }
    }

    /** Check [localPart] in each of the email domains, as opposed to the alias domains. */
    private suspend fun checkAvailability(localPart: String): AddressAvailability =
        try {
            val available =
                withContext(Dispatchers.IO) {
                    val domains = emailClient.listEmailDomains().filter { !it.isAliasDomain }.map { it.domain }
                    emailClient.checkEmailAddressAvailability(CheckEmailAddressAvailabilityInput(listOf(localPart), domains))
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
