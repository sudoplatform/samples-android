/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.direlayexample.connection

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.sudoplatform.direlayexample.MainActivity
import com.sudoplatform.direlayexample.postboxes.postboxes
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

/**
 * Test navigation from the connection screen and different interactions.
 *
 * @since 2021-06-29
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ConnectionTest {

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
    fun testSendMessage() {
        connection {
            createPostboxAndConnectFlow()
            sendMessage("hello world")
            checkMessageItemDisplayed()
        }
    }
}
