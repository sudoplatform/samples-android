/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sudoplatform.telephonyexample.App

/** Receives pushes and new tokens from Firebase Cloud Messaging. */
class TelephonyMessagingService : FirebaseMessagingService() {
    private val notifications: TelephonyNotifications?
        get() = (application as App).telephonyNotifications

    override fun onMessageReceived(message: RemoteMessage) {
        notifications?.process(message)
    }

    override fun onNewToken(token: String) {
        notifications?.onNewToken(token)
    }
}
