/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sudoplatform.emailexample.R
import com.sudoplatform.sudoemail.SudoEmailNotificationHandler
import com.sudoplatform.sudoemail.types.EmailMessageReceivedNotification

/**
 * Posts a notification for each email message received. Pushes are data-only, so nothing is shown
 * unless the app posts it. Tapping the notification opens the message.
 */
class EmailMessageNotifier(
    private val context: Context,
) : SudoEmailNotificationHandler {
    private companion object {
        const val CHANNEL_ID = "messageReceived"

        /** Notifications are identified by their message ID, used as the tag. */
        const val NOTIFICATION_ID = 0
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        notificationManager.createNotificationChannel(
            NotificationChannelCompat
                .Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.email_messages))
                .build(),
        )
    }

    override fun onEmailMessageReceived(message: EmailMessageReceivedNotification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val contentIntent =
            PendingIntent.getActivity(
                context,
                message.id.hashCode(),
                MessageTarget(message.emailAddressId, message.id).toIntent(context),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        notificationManager.notify(
            message.id,
            NOTIFICATION_ID,
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(message.from.toString())
                .setContentText(message.subject ?: context.getString(R.string.no_subject))
                .setCategory(NotificationCompat.CATEGORY_EMAIL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setWhen(message.receivedAt.time)
                .setShowWhen(true)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build(),
        )
    }
}
