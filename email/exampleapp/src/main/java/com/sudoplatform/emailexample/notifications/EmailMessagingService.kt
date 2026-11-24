/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sudoplatform.emailexample.App

/** Receives pushes and new tokens from Firebase Cloud Messaging. */
class EmailMessagingService : FirebaseMessagingService() {
    private val notifications: EmailNotifications?
        get() = (application as App).emailNotifications

    override fun onMessageReceived(message: RemoteMessage) {
        notifications?.process(message)
    }

    override fun onNewToken(token: String) {
        notifications?.onNewToken(token)
    }
}
