/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample

import android.app.Application
import com.sudoplatform.sudoidentityverification.SudoIdentityVerificationClient
import com.sudoplatform.sudokeymanager.KeyManager
import com.sudoplatform.sudokeymanager.KeyManagerFactory
import com.sudoplatform.sudologging.AndroidUtilsLogDriver
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import com.sudoplatform.sudouser.SudoUserClient
import com.sudoplatform.sudovirtualcards.SudoVirtualCardsClient
import java.net.URI

class App : Application() {

    lateinit var keyManager: KeyManager
    lateinit var sudoUserClient: SudoUserClient
    lateinit var sudoProfilesClient: SudoProfilesClient
    lateinit var identityVerificationClient: SudoIdentityVerificationClient
    lateinit var sudoVirtualCardsClient: SudoVirtualCardsClient
    lateinit var logger: Logger

    override fun onCreate() {
        super.onCreate()

        logger = Logger("virtualCardsExample", AndroidUtilsLogDriver(LogLevel.DEBUG))

        // Create an instance of SudoUserClient to perform registration and sign in.
        sudoUserClient = SudoUserClient.builder(this)
            .setNamespace("sudo-test")
            .setLogger(logger)
            .build()

        // Create an instance of SudoProfilesClient to perform creation, deletion and modification
        // of Sudos.
        val blobURI = URI(cacheDir.path)

        sudoProfilesClient = SudoProfilesClient
            .builder(this, sudoUserClient, blobURI)
            .setLogger(logger)
            .build()

        // Create an instance of KeyManager to perform key management.
        keyManager = KeyManagerFactory(this).createAndroidKeyManager() as KeyManager

        // Create an instance of SudoIdentityVerificationClient to perform secure id verification.
        identityVerificationClient = SudoIdentityVerificationClient.builder(this, sudoUserClient).build()

        // Create an instance of SudoVirtualCardsClient to perform funding source and card lifecycle
        // operations and access transactions.
        sudoVirtualCardsClient = SudoVirtualCardsClient.builder()
            .setContext(this)
            .setSudoUserClient(sudoUserClient)
            .setSudoProfilesClient(sudoProfilesClient)
            .build()
    }
}
