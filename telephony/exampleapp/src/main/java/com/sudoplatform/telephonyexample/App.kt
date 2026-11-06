/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import com.sudoplatform.sudoentitlements.SudoEntitlementsClient
import com.sudoplatform.sudokeymanager.KeyManager
import com.sudoplatform.sudokeymanager.KeyManagerFactory
import com.sudoplatform.sudologging.AndroidUtilsLogDriver
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import com.sudoplatform.sudotelephony.SudoTelephonyClient
import com.sudoplatform.sudouser.ApiResult
import com.sudoplatform.sudouser.SudoUserClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class App : Application() {
    companion object {
        /** Name of the preference set that holds sign in information. */
        const val SIGN_IN_PREFERENCES = "SignIn"

        /** True if Federated Single Sign On was used. */
        const val FSSO_USED_PREFERENCE = "usedFSSO"

        /** User name of the last user to sign in, used to notice when a different user signs in. */
        private const val LAST_SIGNED_IN_USER_PREFERENCE = "lastSignedInUser"
    }

    lateinit var keyManager: KeyManager
    lateinit var sudoUserClient: SudoUserClient
    lateinit var sudoEntitlementsClient: SudoEntitlementsClient
    lateinit var sudoProfilesClient: SudoProfilesClient
    lateinit var sudoTelephonyClient: SudoTelephonyClient

    lateinit var logger: Logger
    lateinit var telephonyEvents: TelephonyEvents

    /**
     * Set when the Sudo Platform clients could not be constructed, most commonly because
     * `sudoplatformconfig.json` has not been added to the assets. The register screen reports it
     * rather than the app crashing on launch.
     */
    var initializationError: Throwable? = null
        private set

    override fun onCreate() {
        super.onCreate()

        logger = Logger("telephonyExample", AndroidUtilsLogDriver(LogLevel.DEBUG))

        try {
            // Create an instance of SudoUserClient to perform registration and sign in.
            sudoUserClient =
                SudoUserClient
                    .builder(this)
                    .setNamespace("sudo-test")
                    .setLogger(logger)
                    .build()

            // Create an instance of SudoProfilesClient to perform creation, deletion and modification
            // of Sudos.
            sudoProfilesClient =
                SudoProfilesClient
                    .builder(this, sudoUserClient, Uri.fromFile(cacheDir))
                    .setLogger(logger)
                    .build()

            // Create an instance of KeyManager to perform key management.
            keyManager = KeyManagerFactory(this).createAndroidKeyManager() as KeyManager

            // Create an instance of the SudoEntitlementsClient to redeem and check what resources the
            // user is entitled to use.
            sudoEntitlementsClient =
                SudoEntitlementsClient
                    .builder()
                    .setContext(this)
                    .setSudoUserClient(sudoUserClient)
                    .setLogger(logger)
                    .build()

            // Create an instance of SudoTelephonyClient to search for and provision phone numbers, and
            // to read their messages.
            sudoTelephonyClient =
                SudoTelephonyClient
                    .builder()
                    .setContext(this)
                    .setSudoUserClient(sudoUserClient)
                    .setLogger(logger)
                    .build()
            telephonyEvents = TelephonyEvents(sudoTelephonyClient, logger)
        } catch (e: Exception) {
            logger.error("Failed to initialize Sudo Platform clients: ${e.localizedMessage}")
            initializationError = e
        }
    }

    private val signInPreferences get() = getSharedPreferences(SIGN_IN_PREFERENCES, Context.MODE_PRIVATE)

    /**
     * Deregister from the Sudo Platform and clear all local data, including the keys that seal
     * phone messages, so the next registration starts afresh.
     */
    suspend fun deregister() {
        telephonyEvents.unsubscribe()
        sudoUserClient.deregister()
        removeUserData()
        sudoUserClient.reset()
    }

    /**
     * Sign out of Federated Sign In. A plain sign out keeps the user's keys so they can read their
     * Sudos and messages when they sign back in; with [removeData] they are deleted too.
     *
     * Local tokens are cleared even if the browser or global sign out fails, otherwise the app can
     * be left unable to clear the auth cookies.
     */
    suspend fun signOutFederated(removeData: Boolean) {
        telephonyEvents.unsubscribe()
        // Opens the browser to clear the sign in cookies. The result isn't awaited because it is
        // never delivered when the sign out redirect returns to the app.
        sudoUserClient.presentFederatedSignOutUI { result ->
            if (result is ApiResult.Failure) {
                logger.debug("FSSO sign out failed: ${result.error.localizedMessage}")
            }
        }
        withContext(Dispatchers.IO) {
            try {
                sudoUserClient.globalSignOut()
            } finally {
                sudoUserClient.clearAuthTokens()
            }
            if (removeData) {
                removeUserData()
            }
            // The sign in state still has to go, otherwise a TEST registration finds the federated
            // user registered.
            sudoUserClient.reset()
        }
        signInPreferences.edit { putBoolean(FSSO_USED_PREFERENCE, false) }
    }

    /**
     * Get the clients ready for the user who has just signed in. If a different user signs in
     * after a plain sign out, the previous user's keys and data are removed first, so the new user
     * neither sees them nor encrypts with them.
     */
    suspend fun prepareForSignedInUser() {
        val userName = sudoUserClient.getUserName() ?: return
        val lastUserName = signInPreferences.getString(LAST_SIGNED_IN_USER_PREFERENCE, null)
        if (lastUserName != null && lastUserName != userName) {
            removeUserData()
        }
        signInPreferences.edit { putString(LAST_SIGNED_IN_USER_PREFERENCE, userName) }
    }

    /** Clear the user's keys and cached data, leaving the sign in itself alone. */
    private suspend fun removeUserData() {
        sudoProfilesClient.reset()
        // reset() removes the Sudo encryption key, which the client only generates when constructed.
        sudoProfilesClient.generateEncryptionKey()
        sudoTelephonyClient.reset()
        signInPreferences.edit { remove(LAST_SIGNED_IN_USER_PREFERENCE) }
    }
}
