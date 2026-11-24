/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample

import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudoemail.subscription.EmailMessageSubscriber
import com.sudoplatform.sudoemail.subscription.Subscriber
import com.sudoplatform.sudoemail.types.EmailMessage
import com.sudoplatform.sudologging.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** An email message that was created, updated or deleted. */
data class EmailMessageEvent(
    val emailMessage: EmailMessage,
    val type: EmailMessageSubscriber.ChangeType,
)

/**
 * Shares the email message subscription between screens. The subscription is opened once and kept
 * for the signed in session, rather than each screen subscribing and unsubscribing.
 *
 * Events that arrive while no screen is collecting are dropped, so screens list again when they
 * resume.
 */
class EmailEvents(
    private val emailClient: SudoEmailClient,
    private val logger: Logger,
) {
    private companion object {
        const val SUBSCRIPTION_ID = "emailExample"
        const val BUFFER_CAPACITY = 64
    }

    private val _emailMessages = MutableSharedFlow<EmailMessageEvent>(extraBufferCapacity = BUFFER_CAPACITY)

    /** Messages created, updated or deleted in any of the user's email addresses. */
    val emailMessages: SharedFlow<EmailMessageEvent> = _emailMessages.asSharedFlow()

    private val mutex = Mutex()

    @Volatile private var subscribed = false

    private val subscriber =
        object : EmailMessageSubscriber {
            override fun emailMessageChanged(
                emailMessage: EmailMessage,
                type: EmailMessageSubscriber.ChangeType,
            ) {
                _emailMessages.tryEmit(EmailMessageEvent(emailMessage, type))
            }

            override fun connectionStatusChanged(state: Subscriber.ConnectionState) {
                logger.debug("Email message subscription $state")
                // The SDK drops every subscriber on disconnect, so the next subscribe() reopens it.
                if (state == Subscriber.ConnectionState.DISCONNECTED) {
                    subscribed = false
                }
            }
        }

    /**
     * Open the subscription if it isn't open. Screens call this when they resume. A failure is
     * logged rather than reported, as the screen still lists without live updates.
     */
    suspend fun subscribe() =
        mutex.withLock {
            if (subscribed) return@withLock
            // Set first, so a disconnect reported while subscribing isn't overwritten.
            subscribed = true
            var succeeded = false
            try {
                emailClient.subscribeToEmailMessages(SUBSCRIPTION_ID, subscriber)
                succeeded = true
            } catch (e: SudoEmailClient.EmailMessageException) {
                logger.warning("Failed to subscribe to email messages: ${e.localizedMessage}")
            } finally {
                // Also covers the caller being cancelled mid-subscribe.
                if (!succeeded) subscribed = false
            }
        }

    /** Close the subscription when the user signs out or deregisters. */
    suspend fun unsubscribe() =
        mutex.withLock {
            emailClient.unsubscribeFromEmailMessages(SUBSCRIPTION_ID)
            subscribed = false
        }
}
