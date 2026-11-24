/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.notifications

import android.content.Context
import android.provider.Settings
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.sudoplatform.emailexample.App
import com.sudoplatform.sudoemail.SudoEmailNotifiableClient
import com.sudoplatform.sudoemail.initEmailNotifications
import com.sudoplatform.sudoemail.isEmailNotificationForAddressIdEnabled
import com.sudoplatform.sudoemail.setEmailNotificationsForAddressId
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudonotification.DefaultNotificationDeviceInputProvider
import com.sudoplatform.sudonotification.NotificationDeviceInputProvider
import com.sudoplatform.sudonotification.SudoNotificationClient
import com.sudoplatform.sudonotification.SudoNotificationClient.NotificationException
import com.sudoplatform.sudonotification.types.NotificationConfiguration
import com.sudoplatform.sudonotification.types.NotificationSettingsInput
import com.sudoplatform.sudouser.SudoUserClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Registers this device for email push notifications, turns them on or off for an email
 * address, and hands incoming pushes to the SDK, which calls [EmailMessageNotifier] for each
 * message received.
 *
 * Construction throws if the environment's `sudoplatformconfig.json` doesn't configure the
 * notification service.
 */
class EmailNotifications(
    private val context: Context,
    private val sudoUserClient: SudoUserClient,
    private val logger: Logger,
) {
    private val notifiableClient: SudoEmailNotifiableClient =
        SudoEmailNotifiableClient
            .builder()
            .setContext(context)
            // Must match the email client's namespace, or notifications can't be decrypted.
            .setNamespace(App.EMAIL_KEY_NAMESPACE)
            .setNotificationHandler(EmailMessageNotifier(context))
            .setLogger(logger)
            .build()

    private val notificationClient: SudoNotificationClient =
        SudoNotificationClient
            .builder()
            .setContext(context)
            .setSudoUserClient(sudoUserClient)
            .setNotifiableClients(listOf(notifiableClient))
            .setLogger(logger)
            .build()

    private val deviceId: String = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Serialises registration and each read-modify-write of the notification configuration. */
    private val mutex = Mutex()

    private var registerJob: Job? = null

    @Volatile private var pushToken: String? = null

    /**
     * Register this device and enable email notifications, in the background. Called after
     * sign in and when Firebase issues a new token. A failure is logged, as the app works
     * without notifications.
     */
    fun registerInBackground() {
        if (!sudoUserClient.isSignedIn()) return
        registerJob =
            scope.launch {
                try {
                    register()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.warning("Failed to register for notifications: $e")
                }
            }
    }

    /** Called by [EmailMessagingService] when Firebase issues a new token. */
    fun onNewToken(token: String) {
        pushToken = token
        registerInBackground()
    }

    /**
     * Stop notifications to this device. Must be called while the user is still signed in.
     * A failure is logged rather than thrown, so it doesn't prevent signing out.
     */
    suspend fun deregister() {
        registerJob?.cancelAndJoin()
        if (!sudoUserClient.isSignedIn()) return
        try {
            mutex.withLock { notificationClient.deRegisterNotification(device()) }
        } catch (e: NotificationException.NoDeviceNotificationException) {
            // Already deregistered.
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warning("Failed to deregister for notifications: $e")
        }
    }

    /** True if notifications are on for an email address. */
    suspend fun isEnabled(emailAddressId: String): Boolean =
        mutex.withLock { currentConfiguration().isEmailNotificationForAddressIdEnabled(emailAddressId) }

    /** Turn notifications on or off for an email address. */
    suspend fun setEnabled(
        emailAddressId: String,
        enabled: Boolean,
    ) = mutex.withLock {
        saveConfiguration(currentConfiguration().setEmailNotificationsForAddressId(emailAddressId, enabled))
    }

    /**
     * Pass a push to the SDK. A message that isn't from the Sudo Platform makes the notification
     * client throw, so it is logged and ignored.
     */
    fun process(message: RemoteMessage) {
        try {
            notificationClient.process(message)
        } catch (e: Exception) {
            logger.warning("Ignored a push that couldn't be processed: ${e.javaClass.simpleName}")
        }
    }

    private suspend fun register() =
        mutex.withLock {
            val device = device()
            try {
                notificationClient.updateNotificationRegistration(device)
            } catch (e: NotificationException.NoDeviceNotificationException) {
                notificationClient.registerNotification(device)
            }
            saveConfiguration(currentConfiguration())
            logger.info("Registered for notifications")
        }

    /**
     * The device's notification configuration with the email default rules added, which are
     * required for any email push to be sent.
     */
    private suspend fun currentConfiguration(): NotificationConfiguration {
        val configuration =
            try {
                notificationClient.getNotificationConfiguration(device())
            } catch (e: NotificationException.NoNotificationConfigException) {
                NotificationConfiguration(configs = emptyList())
            }
        return configuration.initEmailNotifications()
    }

    /** The email schema must be included, or the service rejects the email rules. */
    private suspend fun saveConfiguration(configuration: NotificationConfiguration) {
        val device = device()
        notificationClient.setNotificationConfiguration(
            NotificationSettingsInput(
                bundleId = device.bundleIdentifier,
                deviceId = device.deviceIdentifier,
                filter = configuration.configs,
                services = listOf(notifiableClient.getSchema()),
            ),
        )
    }

    private suspend fun device(): NotificationDeviceInputProvider {
        val token =
            pushToken ?: withContext(Dispatchers.IO) { Tasks.await(FirebaseMessaging.getInstance().token) }.also { pushToken = it }
        return DefaultNotificationDeviceInputProvider(context, deviceId, token)
    }
}
