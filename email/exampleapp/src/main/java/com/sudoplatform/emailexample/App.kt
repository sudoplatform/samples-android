/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample

import android.app.Application
import android.net.Uri
import com.sudoplatform.emailexample.notifications.EmailExampleNotificationHandler
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudoemail.SudoEmailNotifiableClient
import com.sudoplatform.sudoentitlements.SudoEntitlementsClient
import com.sudoplatform.sudokeymanager.KeyManager
import com.sudoplatform.sudokeymanager.KeyManagerFactory
import com.sudoplatform.sudologging.AndroidUtilsLogDriver
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudonotification.DefaultNotificationDeviceInputProvider
import com.sudoplatform.sudonotification.SudoNotificationClient
import com.sudoplatform.sudonotification.types.NotificationConfiguration
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import com.sudoplatform.sudouser.ApiResult
import com.sudoplatform.sudouser.SudoUserClient
import com.sudoplatform.sudouser.exceptions.SignOutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception
import kotlin.coroutines.CoroutineContext

class App : Application(), CoroutineScope {

    companion object {
        /** Name of the preference set that holds sign in information. */
        const val SIGN_IN_PREFERENCES = "SignIn"

        /** True if Federated Single On was used. */
        const val FSSO_USED_PREFERENCE = "usedFSSO"

        const val version = "5.0.0"

        internal lateinit var instance: App
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Main
    lateinit var keyManager: KeyManager
    lateinit var sudoUserClient: SudoUserClient
    lateinit var sudoProfilesClient: SudoProfilesClient
    lateinit var sudoEntitlementsClient: SudoEntitlementsClient
    lateinit var sudoEmailClient: SudoEmailClient
    lateinit var sudoEmailNotifiableClient: SudoEmailNotifiableClient
    lateinit var sudoNotificationClient: SudoNotificationClient
    lateinit var notificationHandler: EmailExampleNotificationHandler
    lateinit var deviceInfo: DefaultNotificationDeviceInputProvider
    lateinit var notificationConfiguration: NotificationConfiguration
    lateinit var logger: Logger

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()

        logger = Logger("emailExample", AndroidUtilsLogDriver(LogLevel.DEBUG))

        // Create an instance of SudoUserClient to perform registration and sign in.
        sudoUserClient = SudoUserClient.builder(this)
            .setNamespace("sudo-test")
            .setLogger(logger)
            .build()

        // Create an instance of SudoProfilesClient to perform creation, deletion and modification
        // of Sudos.
        val blobURI = Uri.fromFile(cacheDir)
        sudoProfilesClient = SudoProfilesClient
            .builder(this, sudoUserClient, blobURI)
            .setLogger(logger)
            .build()

        // Create an instance of KeyManager to perform key management.
        keyManager = KeyManagerFactory(this).createAndroidKeyManager() as KeyManager

        // Create an instance of the SudoEntitlementsClient to redeem and check what resources the
        // user is entitled to use.
        sudoEntitlementsClient = SudoEntitlementsClient.builder()
            .setContext(this)
            .setSudoUserClient(sudoUserClient)
            .setLogger(logger)
            .build()

        // Create an instance of SudoEmailClient to perform email address lifecycle operations and
        // sending/receiving of email messages.
        sudoEmailClient = SudoEmailClient.builder()
            .setContext(this)
            .setSudoUserClient(sudoUserClient)
            .build()

        // Create an instance of EmailExampleNotificationHandler to handle subscribing and
        // unsubscribing from notifications coming from the email service.
        notificationHandler = EmailExampleNotificationHandler()

        // Create an instance of SudoEmailNotifiableClient to process notifications coming from
        // the email service.
        sudoEmailNotifiableClient = SudoEmailNotifiableClient.builder()
            .setContext(this)
            .setNotificationHandler(notificationHandler)
            .build()

        // Create an instance of SudoNotificationClient to manage notification settings.
        sudoNotificationClient = SudoNotificationClient.builder()
            .setContext(this)
            .setSudoUserClient(sudoUserClient)
            .setLogger(logger)
            .setNotifiableClients(listOf(sudoEmailNotifiableClient))
            .build()
    }

    @Throws(SignOutException::class)
    suspend fun doFSSOSignout() {
        val userClient = this.sudoUserClient
        try {
            userClient.presentFederatedSignOutUI { result ->
                when (result) {
                    is ApiResult.Success -> {
                        try {
                            launch {
                                userClient.globalSignOut()
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            this.logger.debug("FSSO Signout failed: " + e.localizedMessage)
                            throw SignOutException.FailedException(e.message)
                        } finally {
                            userClient.clearAuthTokens()
                        }
                    }
                    is ApiResult.Failure -> {
                        launch {
                            // If global sign out fails we still want to complete the sign out locally, otherwise we can be left in a bad
                            // state where the auth cookies cannot be cleared.
                            userClient.clearAuthTokens()
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            this.logger.debug("FSSO Signout failed: " + e.localizedMessage)
            throw SignOutException.FailedException(e.message)
        }
    }
}
