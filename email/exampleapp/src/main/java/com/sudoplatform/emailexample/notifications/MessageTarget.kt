/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.notifications

import android.content.Context
import android.content.Intent
import com.sudoplatform.emailexample.MainActivity

/** An email message to open when the user taps its notification. */
data class MessageTarget(
    val emailAddressId: String,
    val messageId: String,
) {
    companion object {
        private const val EXTRA_EMAIL_ADDRESS_ID = "com.sudoplatform.emailexample.extra.EMAIL_ADDRESS_ID"
        private const val EXTRA_MESSAGE_ID = "com.sudoplatform.emailexample.extra.MESSAGE_ID"

        /** The message an intent from [toIntent] opens, or null for any other intent. */
        fun fromIntent(intent: Intent): MessageTarget? {
            val emailAddressId = intent.getStringExtra(EXTRA_EMAIL_ADDRESS_ID) ?: return null
            val messageId = intent.getStringExtra(EXTRA_MESSAGE_ID) ?: return null
            return MessageTarget(emailAddressId, messageId)
        }
    }

    /** An intent that opens [MainActivity] on this message. */
    fun toIntent(context: Context): Intent =
        Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_EMAIL_ADDRESS_ID, emailAddressId)
            .putExtra(EXTRA_MESSAGE_ID, messageId)
}
