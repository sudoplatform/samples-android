/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.phonemessages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudotelephony.SudoTelephonyClient
import com.sudoplatform.sudotelephony.SudoTelephonyClient.PhoneMessageException
import com.sudoplatform.sudotelephony.types.ListAPIResult
import com.sudoplatform.sudotelephony.types.PhoneMessage
import com.sudoplatform.sudotelephony.types.PhoneMessageChangeType
import com.sudoplatform.sudotelephony.types.PhoneMessageEvent
import com.sudoplatform.sudotelephony.types.PhoneNumber
import com.sudoplatform.sudotelephony.types.inputs.DeletePhoneMessagesInput
import com.sudoplatform.sudotelephony.types.inputs.ListPhoneMessagesForPhoneNumberIdInput
import com.sudoplatform.telephonyexample.App
import com.sudoplatform.telephonyexample.TelephonyEvents
import com.sudoplatform.telephonyexample.notifications.TelephonyNotifications
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A message that couldn't be deleted.
 *
 * @property message the error from the SDK, or null when the service reported the message as not
 *  deleted without an error.
 */
data class DeleteFailure(
    val message: String? = null,
)

data class PhoneMessagesUiState(
    /** The number whose messages are listed, once loaded. Used for the title. */
    val phoneNumber: PhoneNumber? = null,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    /** True once the first page has been listed, so an empty list means there are none. */
    val hasLoaded: Boolean = false,
    /** Newest first. */
    val messages: List<PhoneMessage> = emptyList(),
    /** True when there is another page to load. */
    val hasMore: Boolean = false,
    /**
     * The messages are sealed with a key that only the device that provisioned the number holds,
     * and this isn't that device.
     */
    val keyMissing: Boolean = false,
    val listError: String? = null,
    /** A message the user swiped away, awaiting confirmation before it is deleted. */
    val pendingDelete: String? = null,
    val deleteFailure: DeleteFailure? = null,
    /** True while the user is asked to confirm releasing the number. */
    val confirmRelease: Boolean = false,
    val isReleasing: Boolean = false,
    val releaseError: String? = null,
    /** Whether notifications are on for this number, or null when they aren't available. */
    val notificationsEnabled: Boolean? = null,
    val notificationSettingError: String? = null,
)

/** Emitted once the number has been released, so the screen can close. */
data object PhoneNumberReleased

/**
 * Lists a phone number's messages a page at a time, keeps them up to date from the telephony
 * subscription, deletes them on request, releases the number, and turns its notifications on
 * or off.
 */
class PhoneMessagesViewModel(
    private val phoneNumberId: String,
    private val telephonyClient: SudoTelephonyClient,
    private val telephonyEvents: TelephonyEvents,
    private val telephonyNotifications: TelephonyNotifications?,
    private val logger: Logger,
) : ViewModel() {
    companion object {
        const val PHONE_NUMBER_ID_ARG = "phoneNumberId"

        private const val PAGE_SIZE = 20

        val Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as App
                    PhoneMessagesViewModel(
                        phoneNumberId = checkNotNull(createSavedStateHandle().get<String>(PHONE_NUMBER_ID_ARG)),
                        telephonyClient = app.sudoTelephonyClient,
                        telephonyEvents = app.telephonyEvents,
                        telephonyNotifications = app.telephonyNotifications,
                        logger = app.logger,
                    )
                }
            }
    }

    private val _uiState = MutableStateFlow(PhoneMessagesUiState())
    val uiState: StateFlow<PhoneMessagesUiState> = _uiState.asStateFlow()

    private val _released = Channel<PhoneNumberReleased>(Channel.BUFFERED)
    val released: Flow<PhoneNumberReleased> = _released.receiveAsFlow()

    private var nextToken: String? = null
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                val phoneNumber = withContext(Dispatchers.IO) { telephonyClient.getPhoneNumber(phoneNumberId) }
                _uiState.update { it.copy(phoneNumber = phoneNumber) }
            } catch (e: SudoTelephonyClient.PhoneNumberException) {
                // The number is only used for the title; the messages are still listed without it.
            }
        }
        viewModelScope.launch { telephonyEvents.phoneMessages.collect(::applyEvent) }
        loadNotificationSetting()
    }

    /** Fetch the first page again, for example to pick up messages that arrived or were read. */
    fun refresh() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, isLoadingMore = false, listError = null) }
        loadJob =
            viewModelScope.launch {
                // Subscribe before listing, so that a message arriving in between isn't missed.
                telephonyEvents.subscribe()
                loadPage(nextToken = null) { page ->
                    _uiState.update { it.copy(isLoading = false, hasLoaded = true, messages = page) }
                }
            }
    }

    /** Fetch the next page, when there is one and nothing else is loading. */
    fun loadMore() {
        val state = _uiState.value
        val token = nextToken
        if (token == null || state.isLoading || state.isLoadingMore) return
        _uiState.update { it.copy(isLoadingMore = true) }
        loadJob =
            viewModelScope.launch {
                loadPage(nextToken = token) { page ->
                    _uiState.update { state -> state.copy(isLoadingMore = false, messages = (state.messages + page).distinctBy { it.id }) }
                }
            }
    }

    fun requestDelete(messageId: String) {
        _uiState.update { it.copy(pendingDelete = messageId) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(pendingDelete = null) }
    }

    /**
     * Delete the message awaiting confirmation. It is removed from the list immediately and
     * restored if the delete fails. The service deletes in batches that can partly succeed, so a
     * message it couldn't delete is reported in the result rather than as an exception.
     */
    fun confirmDelete() {
        val messageId = _uiState.value.pendingDelete ?: return
        _uiState.update { state -> state.copy(pendingDelete = null, messages = state.messages.filterNot { it.id == messageId }) }
        viewModelScope.launch {
            try {
                val result =
                    withContext(Dispatchers.IO) { telephonyClient.deletePhoneMessages(DeletePhoneMessagesInput(ids = listOf(messageId))) }
                if (messageId !in result.succeededIds) {
                    _uiState.update { it.copy(deleteFailure = DeleteFailure()) }
                    refresh()
                }
            } catch (e: PhoneMessageException) {
                _uiState.update { it.copy(deleteFailure = DeleteFailure(e.describe())) }
                refresh()
            }
        }
    }

    fun requestRelease() {
        _uiState.update { it.copy(confirmRelease = true) }
    }

    fun cancelRelease() {
        _uiState.update { it.copy(confirmRelease = false) }
    }

    fun confirmRelease() {
        _uiState.update { it.copy(confirmRelease = false, isReleasing = true) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { telephonyClient.deprovisionPhoneNumber(phoneNumberId) }
                _released.send(PhoneNumberReleased)
            } catch (e: SudoTelephonyClient.PhoneNumberException.KeyPairRemovalFailedException) {
                // The number was released; only removing its key from this device failed. This
                // screen closes once the number is released, so log the failure instead of
                // showing it on the way out.
                logger.error("Released phone number ${e.deprovisioned.id}, but couldn't remove its key: ${e.cause}")
                _released.send(PhoneNumberReleased)
            } catch (e: SudoTelephonyClient.PhoneNumberException) {
                _uiState.update { it.copy(isReleasing = false, releaseError = e.describe()) }
            }
        }
    }

    /** Turn this number's notifications off if they are on, or on if they are off. */
    fun toggleNotifications() {
        val notifications = telephonyNotifications ?: return
        val enabled = _uiState.value.notificationsEnabled ?: return
        _uiState.update { it.copy(notificationsEnabled = !enabled) }
        viewModelScope.launch {
            try {
                notifications.setEnabled(phoneNumberId, !enabled)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(notificationsEnabled = enabled, notificationSettingError = e.describe()) }
            }
        }
    }

    fun dismissNotificationSettingError() {
        _uiState.update { it.copy(notificationSettingError = null) }
    }

    fun dismissReleaseError() {
        _uiState.update { it.copy(releaseError = null) }
    }

    fun dismissListError() {
        _uiState.update { it.copy(listError = null) }
    }

    fun dismissDeleteFailure() {
        _uiState.update { it.copy(deleteFailure = null) }
    }

    /**
     * listPhoneMessagesForPhoneNumberId unseals each message with the number's private key on
     * this device. If the key isn't here, the page can't be read at all, which is reported
     * separately from other failures. Messages that can't be unsealed are left out of the list.
     */
    private suspend fun loadPage(
        nextToken: String?,
        onPage: (List<PhoneMessage>) -> Unit,
    ) {
        try {
            val result =
                withContext(Dispatchers.IO) {
                    telephonyClient.listPhoneMessagesForPhoneNumberId(
                        ListPhoneMessagesForPhoneNumberIdInput(phoneNumberId = phoneNumberId, limit = PAGE_SIZE, nextToken = nextToken),
                    )
                }
            val (items, unreadable, pageToken) =
                when (result) {
                    is ListAPIResult.Success -> Triple(result.result.items, 0, result.result.nextToken)
                    is ListAPIResult.Partial -> Triple(result.result.items, result.result.failed.size, result.result.nextToken)
                }
            this.nextToken = pageToken
            val keyMissing = nextToken == null && items.isEmpty() && unreadable > 0
            _uiState.update { it.copy(hasMore = pageToken != null, keyMissing = keyMissing) }
            onPage(items)
        } catch (e: PhoneMessageException) {
            _uiState.update {
                when (e) {
                    is PhoneMessageException.KeyNotFoundException,
                    is PhoneMessageException.UnsealingFailedException,
                    -> it.copy(isLoading = false, isLoadingMore = false, keyMissing = true)
                    else -> it.copy(isLoading = false, isLoadingMore = false, listError = e.describe())
                }
            }
        }
    }

    /**
     * Apply a change to one of this number's messages. Events carry no message content, so a
     * received message is fetched; a read state change is applied to the message in the list.
     */
    private suspend fun applyEvent(event: PhoneMessageEvent) {
        if (event.phoneNumberId != phoneNumberId) return
        when (event.changeType) {
            PhoneMessageChangeType.Deleted ->
                _uiState.update { state -> state.copy(messages = state.messages.filterNot { it.id == event.id }) }
            PhoneMessageChangeType.Updated, PhoneMessageChangeType.Received -> {
                val listed = _uiState.value.messages.any { it.id == event.id }
                if (listed && event.changeType == PhoneMessageChangeType.Updated) {
                    _uiState.update { state ->
                        state.copy(messages = state.messages.map { if (it.id == event.id) it.copy(seen = event.seen) else it })
                    }
                } else {
                    fetchMessage(event.id)
                }
            }
            is PhoneMessageChangeType.Unknown -> {}
        }
    }

    private suspend fun fetchMessage(messageId: String) {
        val message =
            try {
                withContext(Dispatchers.IO) { telephonyClient.getPhoneMessage(messageId) }
            } catch (e: PhoneMessageException) {
                // Picked up by the next refresh.
                return
            } ?: return
        _uiState.update { state ->
            val messages = (state.messages.filterNot { it.id == message.id } + message).sortedByDescending { it.receivedAt }
            state.copy(messages = messages)
        }
    }

    /** If reading the setting fails, the toggle stays hidden. */
    private fun loadNotificationSetting() {
        val notifications = telephonyNotifications ?: return
        viewModelScope.launch {
            try {
                val enabled = notifications.isEnabled(phoneNumberId)
                _uiState.update { it.copy(notificationsEnabled = enabled) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warning("Failed to read the notification setting: ${e.describe()}")
            }
        }
    }

    private fun Throwable.describe(): String = localizedMessage ?: toString()
}
