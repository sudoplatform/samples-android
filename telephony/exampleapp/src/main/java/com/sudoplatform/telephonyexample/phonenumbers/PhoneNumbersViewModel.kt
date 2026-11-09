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
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import com.sudoplatform.sudoprofiles.exceptions.SudoProfileException
import com.sudoplatform.sudotelephony.SudoTelephonyClient
import com.sudoplatform.sudotelephony.SudoTelephonyClient.PhoneMessageException
import com.sudoplatform.sudotelephony.types.ListAPIResult
import com.sudoplatform.sudotelephony.types.PhoneMessageChangeType
import com.sudoplatform.sudotelephony.types.PhoneMessageEvent
import com.sudoplatform.sudotelephony.types.PhoneNumber
import com.sudoplatform.sudotelephony.types.PhoneNumberState
import com.sudoplatform.sudotelephony.types.inputs.ListPhoneMessagesForPhoneNumberIdInput
import com.sudoplatform.sudotelephony.types.inputs.ListPhoneNumbersInput
import com.sudoplatform.telephonyexample.App
import com.sudoplatform.telephonyexample.TelephonyEvents
import com.sudoplatform.telephonyexample.sudos.displayLabel
import com.sudoplatform.telephonyexample.sudos.findSudo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PhoneNumbersUiState(
    val sudoLabel: String = "",
    val isLoading: Boolean = false,
    /** True once the numbers have been listed, so an empty list means there are none. */
    val hasLoaded: Boolean = false,
    /** The Sudo's numbers, oldest first. */
    val phoneNumbers: List<PhoneNumber> = emptyList(),
    /** True when [phoneNumbers] includes a number that belongs to no Sudo. */
    val includesUnownedNumbers: Boolean = false,
    /** How many unread messages each number has, by phone number ID. Numbers with none are left out. */
    val unreadCounts: Map<String, Int> = emptyMap(),
    /** A number the user swiped away, awaiting confirmation before it is released. */
    val pendingRelease: PhoneNumber? = null,
    val isReleasing: Boolean = false,
    val listError: String? = null,
    val releaseError: String? = null,
)

/**
 * Lists the phone numbers that belong to one Sudo with their unread message counts, keeps them
 * up to date from the telephony subscriptions, and releases them.
 */
class PhoneNumbersViewModel(
    private val sudoId: String,
    private val sudoProfilesClient: SudoProfilesClient,
    private val telephonyClient: SudoTelephonyClient,
    private val telephonyEvents: TelephonyEvents,
) : ViewModel() {
    companion object {
        const val SUDO_ID_ARG = "sudoId"

        val Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as App
                    PhoneNumbersViewModel(
                        sudoId = checkNotNull(createSavedStateHandle().get<String>(SUDO_ID_ARG)),
                        sudoProfilesClient = app.sudoProfilesClient,
                        telephonyClient = app.sudoTelephonyClient,
                        telephonyEvents = app.telephonyEvents,
                    )
                }
            }
    }

    private val _uiState = MutableStateFlow(PhoneNumbersUiState())
    val uiState: StateFlow<PhoneNumbersUiState> = _uiState.asStateFlow()

    /** The IDs of each number's unread messages, so that an event repeated for a message counts once. */
    private var unreadMessageIds: Map<String, Set<String>> = emptyMap()

    init {
        viewModelScope.launch {
            try {
                val sudo = withContext(Dispatchers.IO) { sudoProfilesClient.findSudo(sudoId) }
                _uiState.update { it.copy(sudoLabel = sudo.displayLabel) }
            } catch (e: SudoProfileException) {
                // The label is only a heading; the numbers are still listed without it.
            }
        }
        viewModelScope.launch { telephonyEvents.phoneNumbers.collect(::applyPhoneNumber) }
        viewModelScope.launch { telephonyEvents.phoneMessages.collect(::applyMessageEvent) }
    }

    /** Fetch the Sudo's numbers and their unread counts from the service. */
    fun refresh() {
        _uiState.update { it.copy(isLoading = true, listError = null) }
        viewModelScope.launch {
            // Subscribe before listing, so that a change made in between isn't missed.
            telephonyEvents.subscribe()
            val numbers =
                try {
                    withContext(Dispatchers.IO) { listSudoPhoneNumbers(telephonyClient) }
                } catch (e: SudoTelephonyClient.PhoneNumberException) {
                    _uiState.update { it.copy(isLoading = false, listError = e.describe()) }
                    return@launch
                }
            _uiState.update { it.copy(isLoading = false, hasLoaded = true).withPhoneNumbers(numbers) }
            unreadMessageIds = withContext(Dispatchers.IO) { listUnreadMessageIds(telephonyClient, numbers) }
            publishUnreadCounts()
        }
    }

    fun requestRelease(phoneNumberId: String) {
        _uiState.update { state -> state.copy(pendingRelease = state.phoneNumbers.find { it.id == phoneNumberId }) }
    }

    fun cancelRelease() {
        _uiState.update { it.copy(pendingRelease = null) }
    }

    /**
     * Release the number awaiting confirmation. The service returns the number's allowance at once
     * and hands the number back to the carrier in the background, so it can be removed from the
     * list as soon as the call returns.
     */
    fun confirmRelease() {
        val phoneNumber = _uiState.value.pendingRelease ?: return
        _uiState.update { it.copy(pendingRelease = null, isReleasing = true) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { telephonyClient.deprovisionPhoneNumber(phoneNumber.id) }
                _uiState.update { state ->
                    state.copy(isReleasing = false).withPhoneNumbers(state.phoneNumbers.filterNot { it.id == phoneNumber.id })
                }
            } catch (e: SudoTelephonyClient.PhoneNumberException) {
                _uiState.update { it.copy(isReleasing = false, releaseError = e.describe()) }
            }
        }
    }

    fun dismissListError() {
        _uiState.update { it.copy(listError = null) }
    }

    fun dismissReleaseError() {
        _uiState.update { it.copy(releaseError = null) }
    }

    /**
     * listPhoneNumbers returns every number the user has, a page at a time. A number belongs to a
     * Sudo when the ownership proof it was provisioned with names that Sudo, which shows up as
     * the number's sudoId. A number provisioned without a proof has no sudoId, so it is listed
     * under every Sudo to keep it reachable.
     */
    private suspend fun listSudoPhoneNumbers(telephonyClient: SudoTelephonyClient): List<PhoneNumber> {
        val numbers = mutableListOf<PhoneNumber>()
        var nextToken: String? = null
        do {
            val page = telephonyClient.listPhoneNumbers(ListPhoneNumbersInput(nextToken = nextToken))
            numbers += page.items.filter(::belongsToSudo)
            nextToken = page.nextToken
        } while (nextToken != null)
        return numbers
    }

    /**
     * The service has no unread count, so each number's messages are listed and the unread ones
     * collected. A number whose messages can't be read on this device is left without a count.
     */
    private suspend fun listUnreadMessageIds(
        telephonyClient: SudoTelephonyClient,
        numbers: List<PhoneNumber>,
    ): Map<String, Set<String>> =
        numbers
            .filter { it.state == PhoneNumberState.Active || it.state == PhoneNumberState.Suspended }
            .associate { number ->
                val unread = mutableSetOf<String>()
                try {
                    var nextToken: String? = null
                    do {
                        val result =
                            telephonyClient.listPhoneMessagesForPhoneNumberId(
                                ListPhoneMessagesForPhoneNumberIdInput(phoneNumberId = number.id, nextToken = nextToken),
                            )
                        nextToken =
                            when (result) {
                                is ListAPIResult.Success -> {
                                    unread +=
                                        result.result.items
                                            .filterNot { it.seen }
                                            .map { it.id }
                                    result.result.nextToken
                                }
                                is ListAPIResult.Partial -> {
                                    unread +=
                                        result.result.items
                                            .filterNot { it.seen }
                                            .map { it.id }
                                    unread +=
                                        result.result.failed
                                            .map { it.partial }
                                            .filterNot { it.seen }
                                            .map { it.id }
                                    result.result.nextToken
                                }
                            }
                    } while (nextToken != null)
                } catch (e: PhoneMessageException) {
                    unread.clear()
                }
                number.id to unread
            }

    /** A number's state changed, for example a new number becoming active or failing. */
    private fun applyPhoneNumber(phoneNumber: PhoneNumber) {
        if (!belongsToSudo(phoneNumber)) return
        _uiState.update { state ->
            val others = state.phoneNumbers.filterNot { it.id == phoneNumber.id }
            val numbers =
                if (phoneNumber.state == PhoneNumberState.Released) others else (others + phoneNumber).sortedBy { it.createdAt }
            state.withPhoneNumbers(numbers)
        }
    }

    /** A message arrived, was read or was deleted, which changes its number's unread count. */
    private fun applyMessageEvent(event: PhoneMessageEvent) {
        val ids = unreadMessageIds[event.phoneNumberId].orEmpty()
        val updated =
            when {
                event.changeType is PhoneMessageChangeType.Unknown -> return
                event.changeType == PhoneMessageChangeType.Deleted || event.seen -> ids - event.id
                else -> ids + event.id
            }
        unreadMessageIds = unreadMessageIds + (event.phoneNumberId to updated)
        publishUnreadCounts()
    }

    private fun publishUnreadCounts() {
        _uiState.update { state ->
            state.copy(unreadCounts = unreadMessageIds.mapValues { it.value.size }.filterValues { it > 0 })
        }
    }

    private fun belongsToSudo(phoneNumber: PhoneNumber): Boolean = phoneNumber.sudoId.let { it == null || it == sudoId }

    private fun PhoneNumbersUiState.withPhoneNumbers(numbers: List<PhoneNumber>) =
        copy(
            phoneNumbers = numbers,
            includesUnownedNumbers = numbers.any { it.sudoId == null },
        )

    private fun Throwable.describe(): String = localizedMessage ?: toString()
}
