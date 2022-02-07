/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.establishconnection

import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.sudoplatform.direlayexample.MainActivity
import com.sudoplatform.direlayexample.connection.connection
import com.sudoplatform.direlayexample.keymanager.KeyManagement
import com.sudoplatform.direlayexample.postboxes.postboxes
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

/**
 * Test navigation from the establish connection screens and different connection methods.
 *
 * @since 2021-06-29
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class EstablishConnectionTest {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule<MainActivity>(MainActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    private lateinit var keyManagement: KeyManagement

    @Before
    fun setup() {
        Timber.plant(Timber.DebugTree())
        activityScenarioRule.scenario.onActivity {
            keyManagement = KeyManagement(it)
        }
    }

    @After
    fun fini() {
        postboxes {
            deregisterCleanUpFlow()
        }
        Timber.uprootAll()
    }

    @Test
    fun testEstablishConnectionFromScanInvitation() {
        connectionOptions {
            connectionOptionsFlow()
            navigateToInviteScreen()
        }
        var peerInvitation = ""
        invite {
            waitFor(1_000L)
            peerInvitation = getInvitationText()
        }
        postboxes {
            pressBackUntilPostboxesDisplayed()
            checkPostboxesItemsDisplayed()
            createPostbox()
            clickOnPostbox(1)
        }
        connectionOptions {
            checkConnectionOptionsItemsDisplayed()
            navigateToScanInvitationScreen()
        }
        scanInvitation {
            setInputInvitationField(peerInvitation)
            clickConnectButton()
        }
        connection {
            checkConnectionItemsDisplayed()
            pressBack()
        }
    }

    @Test
    fun testAttemptConnectWithInvalidInvitation() {
        connectionOptions {
            connectionOptionsFlow()
            navigateToScanInvitationScreen()
        }
        scanInvitation {
            setInputInvitationField("invalidInvitation")
            clickConnectButton()
            clickOnNegativeErrorAlertDialogButton()
        }
    }
}
