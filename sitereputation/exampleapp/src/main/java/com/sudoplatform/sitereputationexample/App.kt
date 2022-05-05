/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sitereputationexample

import android.app.Application
import com.sudoplatform.sudokeymanager.KeyManager
import com.sudoplatform.sudokeymanager.KeyManagerFactory
import com.sudoplatform.sudologging.AndroidUtilsLogDriver
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudositereputation.SudoSiteReputationClient
import com.sudoplatform.sudouser.ApiResult
import com.sudoplatform.sudouser.SudoUserClient
import com.sudoplatform.sudouser.exceptions.SignOutException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class App : Application(), CoroutineScope {

    companion object {
        /** Name of the preference set that holds sign in inforation */
        const val SIGN_IN_PREFERENCES = "SignIn"
        /** True if Federated Single Sign On was used */
        const val FSSO_USED_PREFERENCE = "usedFSSO"
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Main
    lateinit var keyManager: KeyManager
    lateinit var sudoUserClient: SudoUserClient
    lateinit var logger: Logger
    lateinit var siteReputationClient: SudoSiteReputationClient

    override fun onCreate() {
        super.onCreate()

        logger = Logger("siteReputationSample", AndroidUtilsLogDriver(LogLevel.VERBOSE))

        // Create an instance of SudoUserClient to perform registration and sign in.
        sudoUserClient = SudoUserClient.builder(this)
            .setNamespace("sr-test")
            .setLogger(logger)
            .build()

        // Create an instance of KeyManager to perform key management.
        keyManager = KeyManagerFactory(this).createAndroidKeyManager() as KeyManager

        siteReputationClient = SudoSiteReputationClient.builder()
            .setContext(this)
            .setSudoUserClient(sudoUserClient)
            .setLogger(logger)
            .build()
    }

    @Throws(SignOutException::class)
    suspend fun doFSSOSSignout() {
        val userClient = this.sudoUserClient

        try {
            userClient.presentFederatedSignOutUI { result ->
                when (result) {
                    is ApiResult.Success -> {
                        try {
                            launch {
                                userClient.globalSignOut()
                            }
                        } catch (e: Exception) {
                            this.logger.debug("FSSO Signout failed: " + e.localizedMessage)
                            throw SignOutException.FailedException(e.message)
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
        } catch (e: Exception) {
            this.logger.debug("FSSO Signout failed: " + e.localizedMessage)
            throw SignOutException.FailedException(e.message)
        }
    }
}
