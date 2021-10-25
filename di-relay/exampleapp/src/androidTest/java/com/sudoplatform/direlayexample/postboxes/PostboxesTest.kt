/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.postboxes

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.direlayexample.MainActivity
import com.sudoplatform.direlayexample.start.start
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

/**
 * Test navigation from the postboxes display screen to other screens (connection options / connection)
 *
 * @since 2021-06-29
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PostboxesTest {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule<MainActivity>(MainActivity::class.java)

    @Before
    fun setup() {
        Timber.plant(Timber.DebugTree())
    }

    @After
    fun fini() {
        Timber.uprootAll()
        start {
            pressBackUntilAtStartIsDisplayed()
        }
    }

    @Test
    fun testCreateAndDeletePostbox() {
        postboxes {
            createPostboxFlow()
            deletePostbox(0)
        }
    }

    @Test
    fun testCreateMultipleAndDeletePostboxes() {
        postboxes {
            createPostboxFlow()
            createPostbox()
            deletePostboxes(2)
        }
    }
}
