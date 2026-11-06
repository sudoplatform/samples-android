/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample

import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudotelephony.SudoTelephonyClient
import com.sudoplatform.sudotelephony.SudoTelephonyClient.PhoneMessageException
import com.sudoplatform.sudotelephony.SudoTelephonyClient.PhoneNumberException
import com.sudoplatform.sudotelephony.subscription.PhoneMessageSubscriber
import com.sudoplatform.sudotelephony.subscription.PhoneNumberSubscriber
import com.sudoplatform.sudotelephony.subscription.Subscriber
import com.sudoplatform.sudotelephony.types.PhoneMessageEvent
import com.sudoplatform.sudotelephony.types.PhoneNumber
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Shares the telephony subscriptions between screens. Each subscription is opened once and kept
 * for the signed-in session: removing the last subscriber closes the connection, and subscribing
 * again while it is closing can leave a subscription that reports connected but delivers nothing.
 *
 * Events that arrive while no screen is collecting are dropped, so screens list again when they
 * resume.
 */
class TelephonyEvents(
    private val telephonyClient: SudoTelephonyClient,
    private val logger: Logger,
) {
    private companion object {
        const val SUBSCRIPTION_ID = "telephonyExample"
        const val BUFFER_CAPACITY = 64
    }

    private val _phoneNumbers = MutableSharedFlow<PhoneNumber>(extraBufferCapacity = BUFFER_CAPACITY)

    /** The user's phone numbers as they change, for example from provisioning to active. */
    val phoneNumbers: SharedFlow<PhoneNumber> = _phoneNumbers.asSharedFlow()

    private val _phoneMessages = MutableSharedFlow<PhoneMessageEvent>(extraBufferCapacity = BUFFER_CAPACITY)

    /** Messages received, updated or deleted on any of the user's numbers. */
    val phoneMessages: SharedFlow<PhoneMessageEvent> = _phoneMessages.asSharedFlow()

    private val mutex = Mutex()

    @Volatile private var phoneNumbersSubscribed = false

    @Volatile private var phoneMessagesSubscribed = false

    private val phoneNumberSubscriber =
        object : PhoneNumberSubscriber {
            override fun phoneNumberUpdated(phoneNumber: PhoneNumber) {
                _phoneNumbers.tryEmit(phoneNumber)
            }

            override fun connectionStatusChanged(state: Subscriber.ConnectionState) {
                logger.debug("Phone number subscription $state")
                // The SDK drops every subscriber on disconnect, so the next subscribe() reopens it.
                if (state == Subscriber.ConnectionState.DISCONNECTED) phoneNumbersSubscribed = false
            }
        }

    private val phoneMessageSubscriber =
        object : PhoneMessageSubscriber {
            override fun phoneMessageEvent(event: PhoneMessageEvent) {
                _phoneMessages.tryEmit(event)
            }

            override fun connectionStatusChanged(state: Subscriber.ConnectionState) {
                logger.debug("Phone message subscription $state")
                if (state == Subscriber.ConnectionState.DISCONNECTED) phoneMessagesSubscribed = false
            }
        }

    /**
     * Open whichever subscription isn't open. Screens call this when they resume. A failure is
     * logged rather than reported, as the screen still lists without live updates.
     */
    suspend fun subscribe() =
        mutex.withLock {
            if (!phoneNumbersSubscribed) {
                // Set first, so a disconnect reported while subscribing isn't overwritten.
                phoneNumbersSubscribed = true
                var subscribed = false
                try {
                    telephonyClient.subscribeToPhoneNumbers(SUBSCRIPTION_ID, phoneNumberSubscriber)
                    subscribed = true
                } catch (e: PhoneNumberException) {
                    logger.warning("Failed to subscribe to phone numbers: ${e.localizedMessage}")
                } finally {
                    // Also covers the caller being cancelled mid-subscribe.
                    if (!subscribed) phoneNumbersSubscribed = false
                }
            }
            if (!phoneMessagesSubscribed) {
                phoneMessagesSubscribed = true
                var subscribed = false
                try {
                    telephonyClient.subscribeToPhoneMessages(SUBSCRIPTION_ID, phoneMessageSubscriber)
                    subscribed = true
                } catch (e: PhoneMessageException) {
                    logger.warning("Failed to subscribe to phone messages: ${e.localizedMessage}")
                } finally {
                    if (!subscribed) phoneMessagesSubscribed = false
                }
            }
        }

    /** Close both subscriptions when the user signs out or deregisters. */
    suspend fun unsubscribe() =
        mutex.withLock {
            telephonyClient.unsubscribeFromPhoneNumbers(SUBSCRIPTION_ID)
            telephonyClient.unsubscribeFromPhoneMessages(SUBSCRIPTION_ID)
            phoneNumbersSubscribed = false
            phoneMessagesSubscribed = false
        }
}
