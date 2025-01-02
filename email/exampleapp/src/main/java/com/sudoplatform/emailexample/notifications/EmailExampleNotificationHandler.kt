/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sudoplatform.emailexample.App
import com.sudoplatform.emailexample.MainActivity
import com.sudoplatform.emailexample.R
import com.sudoplatform.sudoemail.SudoEmailNotificationHandler
import com.sudoplatform.sudoemail.initEmailNotifications
import com.sudoplatform.sudoemail.types.EmailMessageReceivedNotification
import com.sudoplatform.sudonotification.DefaultNotificationDeviceInputProvider
import com.sudoplatform.sudonotification.SudoNotificationClient
import com.sudoplatform.sudonotification.types.NotificationConfiguration
import com.sudoplatform.sudonotification.types.NotificationSettingsInput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Notification handler handling subscribing and unsubscribing from notifications from
 * the email service.
 */
class EmailExampleNotificationHandler :
    FirebaseMessagingService(),
    CoroutineScope,
    SudoEmailNotificationHandler {

    private val deviceId: String
    private var deviceToken: String? = null
    private var registered = false
    private val app: App = App.instance

    init {
        deviceId = Settings.Secure.getString(app.contentResolver, Settings.Secure.ANDROID_ID)

        FirebaseMessaging.getInstance().subscribeToTopic("all")
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            if (throwable !is CancellationException) {
                app.logger.error("Error in EmailExampleNotificationHandler coroutine")
            }
        }

    override fun onNewToken(token: String) {
        app.logger.info("FCM onNewToken")

        // Store the token, even if we can't register it yet, otherwise
        // we won't know it
        deviceToken = token
        registered = false

        launch {
            register()
        }
    }

    /** Subscribe to notifications from the email service. */
    suspend fun register() {
        if (registered || !app.sudoUserClient.isSignedIn()) {
            return
        }
        if (deviceToken == null) {
            deviceToken = Tasks.await(FirebaseMessaging.getInstance().token)
        }

        try {
            app.logger.info { "Registering token $deviceToken for device ID $deviceId" }
            val deviceInfo = DefaultNotificationDeviceInputProvider(app, deviceId, deviceToken!!)
            app.deviceInfo = deviceInfo
            try {
                app.sudoNotificationClient.updateNotificationRegistration(deviceInfo)
            } catch (e: SudoNotificationClient.NotificationException.NoDeviceNotificationException) {
                try {
                    app.sudoNotificationClient.registerNotification(deviceInfo)
                } catch (e: SudoNotificationClient.NotificationException.AlreadyRegisteredNotificationException) {
                    // Sometimes we have two threads racing through - its OK if one wins.
                    // The remainder of this method is idempotent.
                }
            }
            val configuration =
                try {
                    app.sudoNotificationClient
                        .getNotificationConfiguration(deviceInfo)
                        .initEmailNotifications()
                } catch (e: SudoNotificationClient.NotificationException.NoNotificationConfigException) {
                    // If there is no current configuration, set default configuration which enables
                    // all notifications
                    NotificationConfiguration(configs = emptyList())
                        .initEmailNotifications()
                }
            app.sudoNotificationClient.setNotificationConfiguration(
                NotificationSettingsInput(
                    bundleId = deviceInfo.bundleIdentifier,
                    deviceId = deviceInfo.deviceIdentifier,
                    services = listOf(app.sudoEmailNotifiableClient.getSchema()),
                    filter = configuration.configs,
                ),
            )
            app.notificationConfiguration = configuration
        } catch (e: Exception) {
            app.logger.outputError(Error(e))
            throw e
        }
        registered = true
    }

    /** Unsubscribe from notifications from the email service. */
    suspend fun unregister() {
        if (!registered || !app.sudoUserClient.isSignedIn() || deviceToken == null) {
            return
        }
        try {
            app.sudoNotificationClient.deRegisterNotification(
                DefaultNotificationDeviceInputProvider(app, deviceId, deviceToken!!),
            )
        } catch (e: SudoNotificationClient.NotificationException.NoDeviceNotificationException) {
            // Already deregistered
        } catch (e: Exception) {
            app.logger.outputError(Error(e))
            throw e
        }
        registered = false
    }

    /** Process notifications from the email service when email messages are received. */
    override fun onMessageReceived(message: RemoteMessage) {
        app.sudoNotificationClient.process(message)
    }

    /** Emit a notification when an email message is received. */
    override fun onEmailMessageReceived(message: EmailMessageReceivedNotification) {
        val intent = Intent(app, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent = PendingIntent.getActivity(
            app,
            0, // Request code
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val channelId = "messageReceived"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(
            app,
            channelId,
        )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(message.from.toString())
            .setContentText(message.subject)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setDefaults(android.app.Notification.DEFAULT_ALL)
            .setFullScreenIntent(pendingIntent, true)

        val notificationManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (notificationManager.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(
                channelId,
                "messageReceived",
                // HIGH Importance for heads up notification
                NotificationManager.IMPORTANCE_HIGH,
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(message.hashCode(), notificationBuilder.build())
    }
}
