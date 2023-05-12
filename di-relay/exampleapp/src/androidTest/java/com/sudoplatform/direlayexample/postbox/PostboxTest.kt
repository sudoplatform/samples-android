/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.postbox

import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.direlayexample.MainActivity
import com.sudoplatform.direlayexample.postboxes.postboxes
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

/**
 * Test display of Postbox and Message screens
 *
 * @since 2021-06-29
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PostboxTest {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule<MainActivity>(MainActivity::class.java)

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
    fun testCreateAndDeleteMessage() {
        postbox {
            createMessageFlow("testCreateAndDeleteMessage")
            deleteMessage(0)
        }
    }

    @Test
    fun testCreateMultipleAndDeletePostboxes() {
        postbox {
            createMessageFlow("testCreateAndDeleteMultipleMessages - 1")
            sendMessage("testCreateAndDeleteMultipleMessages - 2")

            deleteMessages(2)
        }
    }

    @Test
    fun testDisablePostbox() {
        postbox {
            createMessageFlow("testDisablePostbox")
            checkMessagesCount(1)
            setPostboxDisabled()
            waitForLoading()
            sendMessage("testDisabledPostbox - postboxDisabled")
            checkMessagesCount(1)
            setPostboxEnabled()
            waitForLoading()
            sendMessage("testDisabledPostbox - postboxEnabled")
            checkMessagesCount(2)
        }
    }

    @Test
    fun testCreateAndDisplayMessage() {
        postbox {
            createMessageFlow("testDisplayMessage")
            checkMessagesCount(1)
            clickOnMessage(0)
        }
        message {
            checkMessageItemsDisplayed()
        }
        pressBack()
    }
}
