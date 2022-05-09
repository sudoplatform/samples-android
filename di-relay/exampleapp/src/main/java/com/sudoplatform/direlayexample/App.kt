/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample

import android.app.Application
import android.net.Uri
import com.sudoplatform.direlayexample.db.PostboxConnectionsStorage
import com.sudoplatform.direlayexample.keymanager.KeyManagement
import com.sudoplatform.sudodirelay.SudoDIRelayClient
import com.sudoplatform.sudoentitlements.SudoEntitlementsClient
import com.sudoplatform.sudologging.AndroidUtilsLogDriver
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import com.sudoplatform.sudouser.SudoUserClient
import com.sudoplatform.sudouser.exceptions.SignOutException
import java.lang.Exception

class App : Application() {

    companion object {
        /** Name of the preference set that holds sign in information. */
        const val SIGN_IN_PREFERENCES = "SignIn"

        /** True if Federated Single Sign On was used. */
        const val FSSO_USED_PREFERENCE = "usedFSSO"
    }

    lateinit var logger: Logger
    lateinit var diRelayClient: SudoDIRelayClient
    lateinit var connectionsStorage: PostboxConnectionsStorage
    lateinit var keyManagement: KeyManagement
    lateinit var sudoUserClient: SudoUserClient
    lateinit var sudoProfilesClient: SudoProfilesClient
    lateinit var sudoEntitlementsClient: SudoEntitlementsClient

    override fun onCreate() {
        super.onCreate()

        logger = Logger("decentralizedIdentityExample", AndroidUtilsLogDriver(LogLevel.DEBUG))

        // Create an instance of SudoUserClient to perform registration and sign in.
        sudoUserClient = SudoUserClient.builder(this)
            .setNamespace("sudo-test")
            .setLogger(logger)
            .build()

        // Create an instance of PostboxConnectionsStorage to manage storage of postboxes and connections.
        connectionsStorage = PostboxConnectionsStorage(this)

        // Create an instance of SudoProfilesClient to perform creation, deletion and modification
        // of Sudos.
        val blobURI = Uri.fromFile(cacheDir)

        sudoProfilesClient = SudoProfilesClient
            .builder(this, sudoUserClient, blobURI)
            .setLogger(logger)
            .build()

        // Create an instance of the SudoEntitlementsClient to redeem and check what resources the
        // user is entitled to use.
        sudoEntitlementsClient = SudoEntitlementsClient.builder()
            .setContext(this)
            .setSudoUserClient(sudoUserClient)
            .setLogger(logger)
            .build()

        // Create an instance of KeyManagement to handle key management of peer connections.
        keyManagement = KeyManagement(context = this)

        // Create an instance of SudoDIRelayClient to perform relay postbox
        // lifecycle operations and sending/receiving of relay messages.
        diRelayClient =
            SudoDIRelayClient.builder()
                .setContext(this)
                .setLogger(logger)
                .setSudoUserClient(sudoUserClient)
                .build()
    }

    @Throws(SignOutException::class)
    fun doFSSOSignout() {
        val userClient = this.sudoUserClient
        try {
            userClient.presentFederatedSignOutUI { }
        } catch (e: Exception) {
            this.logger.debug("FSSO Signout failed: " + e.localizedMessage)
            throw SignOutException.FailedException(e.message)
        }
    }
}
