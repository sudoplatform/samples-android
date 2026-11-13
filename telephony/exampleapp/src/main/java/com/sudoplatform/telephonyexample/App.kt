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
import com.sudoplatform.sudouser.exceptions.SudoUserException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class App : Application() {
    companion object {
        /** Name of the preference set that holds sign in information. */
        const val SIGN_IN_PREFERENCES = "SignIn"

        /** True if Federated Single Sign On was used. */
        const val FSSO_USED_PREFERENCE = "usedFSSO"

        /**
         * True if every phone number shares one key pair, chosen on the register screen and
         * applied to the telephony client created at sign in.
         */
        const val ENFORCE_SINGLETON_KEY_PAIR_PREFERENCE = "com.sudoplatform.enforce-singleton-key-pair"

        /** User name of the last user to sign in, used to notice when a different user signs in. */
        private const val LAST_SIGNED_IN_USER_PREFERENCE = "lastSignedInUser"
    }

    lateinit var keyManager: KeyManager
    lateinit var sudoUserClient: SudoUserClient
    lateinit var sudoEntitlementsClient: SudoEntitlementsClient
    lateinit var sudoProfilesClient: SudoProfilesClient

    lateinit var logger: Logger

    @Volatile private var telephonyClient: SudoTelephonyClient? = null
    private var telephonyEventsOrNull: TelephonyEvents? = null

    /**
     * The telephony client, to search for and provision phone numbers and read their messages.
     * Created by [startTelephony] when the user signs in.
     */
    val sudoTelephonyClient: SudoTelephonyClient
        get() {
            restoreTelephonyIfNeeded()
            return checkNotNull(telephonyClient)
        }

    /** The telephony subscriptions shared between screens for the signed in session. */
    val telephonyEvents: TelephonyEvents
        get() {
            restoreTelephonyIfNeeded()
            return checkNotNull(telephonyEventsOrNull)
        }

    private var enforceSingletonKeyPair = false

    /**
     * True if this device created a new shared phone number key that no key backup contains yet,
     * so the user should export one.
     */
    var telephonyKeyNeedsBackup: Boolean = false
        private set

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
        } catch (e: Exception) {
            logger.error("Failed to initialize Sudo Platform clients: ${e.localizedMessage}")
            initializationError = e
        }
    }

    private val signInPreferences get() = getSharedPreferences(SIGN_IN_PREFERENCES, Context.MODE_PRIVATE)

    /**
     * Create the telephony client for the user who has just signed in, replacing any left from an
     * earlier sign in.
     *
     * @param enforceSingletonKeyPair true if every phone number shares one key pair, as chosen on
     * the register screen.
     */
    suspend fun startTelephony(enforceSingletonKeyPair: Boolean) {
        stopTelephony()
        setTelephony(enforceSingletonKeyPair)
    }

    /**
     * Create the shared phone number key if the session uses one and this device doesn't have it.
     * Provisioning never creates it, so this must run before the user gets a number.
     *
     * Call once the user is signed in and prepared for, since preparing for a different user removes
     * the previous user's keys, and again after importing keys, in case the backup didn't contain one.
     */
    suspend fun prepareTelephonyKeys() {
        val client = telephonyClient ?: return
        if (!enforceSingletonKeyPair) return
        val result = client.createSingletonKeysIfAbsent()
        telephonyKeyNeedsBackup = result.keyPair.created
        if (result.keyPair.created) {
            logger.info("Created the shared phone number key ${result.keyPair.keyId}")
        }
    }

    /** Notes that the user has exported a key backup, which now contains the shared phone number key. */
    fun didExportKeys() {
        telephonyKeyNeedsBackup = false
    }

    /**
     * Deregister from the Sudo Platform and clear all local data, including the keys that seal
     * phone messages, so the next registration starts afresh.
     */
    suspend fun deregister() {
        stopTelephony()
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
        stopTelephony()
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
            } catch (e: SudoUserException) {
                // An expired session can't sign out globally, but must still sign out locally,
                // otherwise the federated user stays registered and blocks every sign in.
                logger.warning("Global sign out failed, signing out on this device only: ${e.localizedMessage}")
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
        // reset() clears the whole telephony key namespace whatever the key mode, so without a
        // signed in session a temporary client does it.
        val client = telephonyClient
        if (client != null) {
            client.reset()
        } else {
            buildTelephonyClient(enforceSingletonKeyPair = false).use { it.reset() }
        }
        signInPreferences.edit { remove(LAST_SIGNED_IN_USER_PREFERENCE) }
    }

    /**
     * If the process was restarted on a screen past sign in, starts telephony with the stored key
     * mode, as sign in would have.
     */
    @Synchronized
    private fun restoreTelephonyIfNeeded() {
        if (telephonyClient != null) return
        check(::sudoUserClient.isInitialized && sudoUserClient.isSignedIn()) {
            "The telephony client is only available once the user has signed in"
        }
        setTelephony(signInPreferences.getBoolean(ENFORCE_SINGLETON_KEY_PAIR_PREFERENCE, false))
    }

    @Synchronized
    private fun setTelephony(enforceSingletonKeyPair: Boolean) {
        val client = buildTelephonyClient(enforceSingletonKeyPair)
        telephonyEventsOrNull = TelephonyEvents(client, logger)
        this.enforceSingletonKeyPair = enforceSingletonKeyPair
        telephonyKeyNeedsBackup = false
        telephonyClient = client
    }

    /** Close the client's subscriptions and drop it, so the next sign in creates one. */
    private suspend fun stopTelephony() {
        val client = telephonyClient ?: return
        val events = telephonyEventsOrNull
        telephonyClient = null
        telephonyEventsOrNull = null
        enforceSingletonKeyPair = false
        telephonyKeyNeedsBackup = false
        try {
            events?.unsubscribe()
        } finally {
            client.close()
        }
    }

    private fun buildTelephonyClient(enforceSingletonKeyPair: Boolean): SudoTelephonyClient =
        SudoTelephonyClient
            .builder()
            .setContext(this)
            .setSudoUserClient(sudoUserClient)
            .setLogger(logger)
            .setEnforceSingletonKeyPair(enforceSingletonKeyPair)
            .build()
}
