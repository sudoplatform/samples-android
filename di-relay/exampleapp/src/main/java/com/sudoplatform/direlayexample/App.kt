/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample

import android.app.Application
import com.sudoplatform.direlayexample.db.PostboxConnectionsStorage
import com.sudoplatform.direlayexample.keymanager.KeyManagement
import com.sudoplatform.sudoconfigmanager.DefaultSudoConfigManager
import com.sudoplatform.sudodirelay.SudoDIRelayClient
import com.sudoplatform.sudologging.AndroidUtilsLogDriver
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudologging.Logger

class App : Application() {

    lateinit var logger: Logger
    lateinit var diRelayClient: SudoDIRelayClient
    lateinit var connectionsStorage: PostboxConnectionsStorage
    lateinit var keyManagement: KeyManagement
    lateinit var basePostboxEndpoint: String

    override fun onCreate() {
        super.onCreate()

        logger = Logger("decentralizedIdentityExample", AndroidUtilsLogDriver(LogLevel.DEBUG))

        // Create an instance of PostboxConnectionsStorage to manage storage of postboxes and connections.
        connectionsStorage = PostboxConnectionsStorage(this)

        // Create an instance of KeyManagement to handle key management of peer connections.
        keyManagement = KeyManagement(context = this)

        // Extract base endpoint from the sudoplatformconfig
        val endpoint = DefaultSudoConfigManager(this, logger)
            .getConfigSet("relayService")
            ?.get("httpEndpoint") as String?
        requireNotNull(endpoint)
        basePostboxEndpoint = "$endpoint/"

        // Create an instance of SudoDIRelayClient to perform relay postbox
        // lifecycle operations and sending/receiving of relay messages.
        diRelayClient =
            SudoDIRelayClient.builder()
                .setContext(this)
                .setLogger(logger)
                .build()
    }
}
