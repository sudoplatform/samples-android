/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.notifications

import android.content.Context
import android.content.Intent
import com.sudoplatform.telephonyexample.MainActivity

/** A message to open when the user taps its notification. */
data class MessageTarget(
    val phoneNumberId: String,
    val messageId: String,
) {
    companion object {
        private const val EXTRA_PHONE_NUMBER_ID = "com.sudoplatform.telephonyexample.extra.PHONE_NUMBER_ID"
        private const val EXTRA_MESSAGE_ID = "com.sudoplatform.telephonyexample.extra.MESSAGE_ID"

        /** The message an intent from [toIntent] opens, or null for any other intent. */
        fun fromIntent(intent: Intent): MessageTarget? {
            val phoneNumberId = intent.getStringExtra(EXTRA_PHONE_NUMBER_ID) ?: return null
            val messageId = intent.getStringExtra(EXTRA_MESSAGE_ID) ?: return null
            return MessageTarget(phoneNumberId, messageId)
        }
    }

    /** An intent that opens [MainActivity] on this message. */
    fun toIntent(context: Context): Intent =
        Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_PHONE_NUMBER_ID, phoneNumberId)
            .putExtra(EXTRA_MESSAGE_ID, messageId)
}
