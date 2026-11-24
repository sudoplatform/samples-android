/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import com.google.firebase.FirebaseApp
import com.sudoplatform.emailexample.notifications.EmailNotifications
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudoemail.types.ConfigurationData
import com.sudoplatform.sudoentitlements.SudoEntitlementsClient
import com.sudoplatform.sudologging.AndroidUtilsLogDriver
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import com.sudoplatform.sudouser.ApiResult
import com.sudoplatform.sudouser.SudoUserClient
import com.sudoplatform.sudouser.exceptions.SudoUserException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class App : Application() {
    companion object {
        /**
         * Key namespace of the email clients. Key backups import only into the namespace they were
         * exported from, so the sample uses the same value on every platform.
         */
        const val EMAIL_KEY_NAMESPACE = "sudo-email-example"

        /** Name of the preference set that holds sign in information. */
        const val SIGN_IN_PREFERENCES = "SignIn"

        /** True if Federated Single Sign On was used. */
        const val FSSO_USED_PREFERENCE = "usedFSSO"

        /**
         * True if every email address and alias shares one key pair, chosen on the register screen
         * and applied to the email client created at sign in.
         */
        const val ENFORCE_SINGLETON_KEY_PAIR_PREFERENCE = "com.sudoplatform.enforce-singleton-key-pair"

        /**
         * True if the app may supply the key pair to provision with, chosen on the register screen
         * and applied to the email client created at sign in. When it may, the email client never
         * removes key pairs on deprovision. Defaults to true, as the SDK does.
         */
        const val ALLOW_CALLER_MANAGED_KEYS_PREFERENCE = "com.sudoplatform.allow-caller-managed-keys"

        /** User name of the last user to sign in, used to notice when a different user signs in. */
        private const val LAST_SIGNED_IN_USER_PREFERENCE = "lastSignedInUser"
    }

    lateinit var sudoUserClient: SudoUserClient
    lateinit var sudoProfilesClient: SudoProfilesClient
    lateinit var sudoEntitlementsClient: SudoEntitlementsClient

    lateinit var logger: Logger

    @Volatile private var emailClient: SudoEmailClient? = null
    private var emailEventsOrNull: EmailEvents? = null

    /**
     * The email client, to provision email addresses and aliases and to send and read email.
     * Created by [startEmail] when the user signs in.
     */
    val sudoEmailClient: SudoEmailClient
        get() {
            restoreEmailIfNeeded()
            return checkNotNull(emailClient)
        }

    /** The email message subscription shared between screens for the signed in session. */
    val emailEvents: EmailEvents
        get() {
            restoreEmailIfNeeded()
            return checkNotNull(emailEventsOrNull)
        }

    private var enforceSingletonKeyPair = false

    /**
     * True if this device created a new shared email key that no key backup contains yet, so the
     * user should export one.
     */
    var emailKeyNeedsBackup: Boolean = false
        private set

    /**
     * Push notifications for received email, or null when this build has no Firebase
     * configuration or the environment doesn't configure the notification service.
     */
    var emailNotifications: EmailNotifications? = null
        private set

    /**
     * Set when the Sudo Platform clients could not be constructed, most commonly because
     * `sudoplatformconfig.json` has not been added to the assets. The register screen reports it
     * rather than the app crashing on launch.
     */
    var initializationError: Throwable? = null
        private set

    /** Cached email service configuration. See [getConfigurationData]. */
    private var configurationData: ConfigurationData? = null
    private val configurationDataMutex = Mutex()

    /**
     * Returns the email service configuration.
     *
     * The configuration is fetched from the [SudoEmailClient] the first time it is requested and
     * reused afterwards. Failures are not cached, so the next call will try again.
     *
     * @throws SudoEmailClient.EmailConfigurationException if the configuration cannot be retrieved.
     */
    @Throws(SudoEmailClient.EmailConfigurationException::class)
    suspend fun getConfigurationData(): ConfigurationData =
        configurationDataMutex.withLock {
            configurationData
                ?: sudoEmailClient.getConfigurationData().also { configurationData = it }
        }

    override fun onCreate() {
        super.onCreate()

        logger = Logger("emailExample", AndroidUtilsLogDriver(LogLevel.DEBUG))

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
            return
        }

        emailNotifications = createEmailNotifications()
    }

    private fun createEmailNotifications(): EmailNotifications? {
        if (FirebaseApp.getApps(this).isEmpty()) {
            logger.info("Push notifications are off: this build has no google-services.json")
            return null
        }
        return try {
            EmailNotifications(this, sudoUserClient, logger)
        } catch (e: Exception) {
            logger.warning("Push notifications are off: ${e.localizedMessage}")
            null
        }
    }

    private val signInPreferences get() = getSharedPreferences(SIGN_IN_PREFERENCES, Context.MODE_PRIVATE)

    /**
     * Create the email client for the user who has just signed in, replacing any left from an
     * earlier sign in. Call after [prepareForSignedInUser], which may reset the email client.
     *
     * @param enforceSingletonKeyPair true if every email address and alias shares one key pair, as
     * chosen on the register screen.
     * @param allowCallerManagedKeys true if the app may supply key pairs, so deprovisioning never
     * removes one, as chosen on the register screen.
     */
    suspend fun startEmail(
        enforceSingletonKeyPair: Boolean,
        allowCallerManagedKeys: Boolean,
    ) {
        stopEmail()
        setEmail(enforceSingletonKeyPair, allowCallerManagedKeys)
    }

    /**
     * Create the shared email key if the session uses one and this device doesn't have it.
     * Provisioning never creates it, so this must run before the user gets an email address or
     * alias.
     *
     * Call once email has started for the signed in user, and again after importing keys, in case
     * the backup didn't contain one.
     */
    suspend fun prepareEmailKeys() {
        val client = emailClient ?: return
        if (!enforceSingletonKeyPair) return
        val result = client.createSingletonKeysIfAbsent()
        emailKeyNeedsBackup = result.keyPair.created
        if (result.keyPair.created) {
            logger.info("Created the shared email key ${result.keyPair.keyId}")
        }
    }

    /** Notes that the user has exported a key backup, which now contains the shared email key. */
    fun didExportKeys() {
        emailKeyNeedsBackup = false
    }

    /** Register for push notifications once the user has signed in. */
    fun registerForNotifications() {
        emailNotifications?.registerInBackground()
    }

    /**
     * Deregister from the Sudo Platform and clear all local data, including the keys that unseal
     * email messages, so the next registration starts afresh.
     */
    suspend fun deregister() {
        emailNotifications?.deregister()
        stopEmail()
        try {
            sudoUserClient.deregister()
        } catch (e: SudoUserException.NotAuthorizedException) {
            // The service no longer accepts this user, e.g. it was removed elsewhere, so only this
            // device's data is left to clear.
            logger.warning("Deregister was rejected, clearing this device only: ${e.localizedMessage}")
        } catch (e: SudoUserException.NotSignedInException) {
            // Signing in failed, so the service can't be told; clear this device so the user can start again.
            logger.warning("Not signed in to deregister, clearing this device only")
        }
        removeUserData()
        sudoUserClient.reset()
    }

    /**
     * Sign out of Federated Sign In. A plain sign out keeps the user's keys so they can read their
     * Sudos and email when they sign back in; with [removeData] they are deleted too.
     *
     * Local tokens are cleared even if the browser or global sign out fails, otherwise the app can
     * be left unable to clear the auth cookies.
     */
    suspend fun signOutFederated(removeData: Boolean) {
        emailNotifications?.deregister()
        stopEmail()
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
     * neither sees them nor encrypts with them. Also creates the Sudo encryption key if this device
     * doesn't have one.
     */
    suspend fun prepareForSignedInUser() {
        val userName = sudoUserClient.getUserName() ?: return
        val lastUserName = signInPreferences.getString(LAST_SIGNED_IN_USER_PREFERENCE, null)
        if (lastUserName != null && lastUserName != userName) {
            removeUserData()
        }
        if (sudoProfilesClient.getSymmetricKeyId() == null) {
            sudoProfilesClient.generateEncryptionKey()
        }
        signInPreferences.edit { putString(LAST_SIGNED_IN_USER_PREFERENCE, userName) }
    }

    /** Clear the user's keys and cached data, leaving the sign in itself alone. */
    private suspend fun removeUserData() {
        sudoProfilesClient.reset()
        // reset() removes the Sudo encryption key, which the client only generates when constructed.
        sudoProfilesClient.generateEncryptionKey()
        // reset() clears the whole email key namespace whatever the key mode, and also closes the
        // client's subscriptions for good, so it's done with a temporary client and the session's
        // client is created afterwards.
        stopEmail()
        buildEmailClient(enforceSingletonKeyPair = false, allowCallerManagedKeys = true).use { it.reset() }
        signInPreferences.edit { remove(LAST_SIGNED_IN_USER_PREFERENCE) }
    }

    /**
     * If the process was restarted on a screen past sign in, starts email with the stored key
     * options, as sign in would have.
     */
    @Synchronized
    private fun restoreEmailIfNeeded() {
        if (emailClient != null) return
        check(::sudoUserClient.isInitialized && sudoUserClient.isSignedIn()) {
            "The email client is only available once the user has signed in"
        }
        setEmail(
            signInPreferences.getBoolean(ENFORCE_SINGLETON_KEY_PAIR_PREFERENCE, false),
            signInPreferences.getBoolean(ALLOW_CALLER_MANAGED_KEYS_PREFERENCE, true),
        )
    }

    @Synchronized
    private fun setEmail(
        enforceSingletonKeyPair: Boolean,
        allowCallerManagedKeys: Boolean,
    ) {
        val client = buildEmailClient(enforceSingletonKeyPair, allowCallerManagedKeys)
        emailEventsOrNull = EmailEvents(client, logger)
        this.enforceSingletonKeyPair = enforceSingletonKeyPair
        emailKeyNeedsBackup = false
        emailClient = client
    }

    /** Close the client's subscription and drop it, so the next sign in creates one. */
    private suspend fun stopEmail() {
        val client = emailClient ?: return
        val events = emailEventsOrNull
        emailClient = null
        emailEventsOrNull = null
        enforceSingletonKeyPair = false
        emailKeyNeedsBackup = false
        try {
            events?.unsubscribe()
        } finally {
            client.close()
        }
    }

    private fun buildEmailClient(
        enforceSingletonKeyPair: Boolean,
        allowCallerManagedKeys: Boolean,
    ): SudoEmailClient =
        SudoEmailClient
            .builder()
            .setContext(this)
            .setSudoUserClient(sudoUserClient)
            .setNamespace(EMAIL_KEY_NAMESPACE)
            .setLogger(logger)
            .setEnforceSingletonKeyPair(enforceSingletonKeyPair)
            .setAllowCallerManagedKeys(allowCallerManagedKeys)
            .build()
}
