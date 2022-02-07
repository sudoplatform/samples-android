/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.direlayexample.connectiondetails

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.sudoplatform.direlayexample.MainActivity
import com.sudoplatform.direlayexample.connection.connection
import com.sudoplatform.direlayexample.establishconnection.connectionOptions
import com.sudoplatform.direlayexample.establishconnection.invite
import com.sudoplatform.direlayexample.establishconnection.scanInvitation
import com.sudoplatform.direlayexample.postboxes.postboxes
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

/**
 * Test navigation to connection details screen and correctness of values displayed.
 *
 * @since 2021-06-29
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ConnectionDetailsTest {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule<MainActivity>(MainActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    @Before
    fun setup() {
        Timber.plant(Timber.DebugTree())
    }

    @After
    fun fini() {
        postboxes {
            deregisterCleanUpFlow()
        }
        Timber.uprootAll()
    }

    @Test
    fun testExpectedDetailsDisplay() {
        var invitation = ""
        var postboxId = ""
        invite {
            postboxInviteFlow()
            invitation = getInvitationText()
        }

        postboxes {
            pressBackUntilPostboxesDisplayed()
            createPostbox()
            postboxId = getPostboxId(1)
            clickOnPostbox(1)
        }

        connectionOptions {
            checkConnectionOptionsItemsDisplayed()
            navigateToScanInvitationScreen()
        }

        scanInvitation {
            setInputInvitationField(invitation)
            clickConnectButton()
        }

        connection {
            checkConnectionItemsDisplayed()
            waitForLoading()
            navigateToConnectionDetails()
        }
        connectionDetails {
            assert(getMyConnectionIdField() == postboxId)
        }
    }
}
