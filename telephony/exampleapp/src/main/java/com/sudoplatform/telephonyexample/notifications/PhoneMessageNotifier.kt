/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sudoplatform.sudotelephony.SudoTelephonyNotificationHandler
import com.sudoplatform.sudotelephony.types.PhoneMessageReceivedNotification
import com.sudoplatform.telephonyexample.R
import com.sudoplatform.telephonyexample.ui.formatPhoneNumber

/**
 * Posts a notification for each message received. Pushes are data-only, so nothing is shown
 * unless the app posts it. Tapping the notification opens the message.
 */
class PhoneMessageNotifier(
    private val context: Context,
) : SudoTelephonyNotificationHandler {
    private companion object {
        const val CHANNEL_ID = "messages"

        /** Notifications are identified by their message ID, used as the tag. */
        const val NOTIFICATION_ID = 0
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        notificationManager.createNotificationChannel(
            NotificationChannelCompat
                .Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.messages))
                .build(),
        )
    }

    override fun onPhoneMessageReceived(notification: PhoneMessageReceivedNotification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val text = notification.text()
        val contentIntent =
            PendingIntent.getActivity(
                context,
                notification.messageId.hashCode(),
                MessageTarget(notification.phoneNumberId, notification.messageId).toIntent(context),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        notificationManager.notify(
            notification.messageId,
            NOTIFICATION_ID,
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(formatPhoneNumber(notification.remotePhoneNumber))
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setWhen(notification.receivedAt.time)
                .setShowWhen(true)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build(),
        )
    }

    /** The body preview, marked when it was cut short, then a note if media was removed. */
    private fun PhoneMessageReceivedNotification.text(): String {
        val body = bodyPreview.takeIf { it.isNotEmpty() }?.let { if (bodyTruncated) "$it…" else it }
        val media = context.getString(R.string.media_omitted).takeIf { mediaOmitted }
        return listOfNotNull(body, media).joinToString("\n").ifEmpty { context.getString(R.string.no_text) }
    }
}
