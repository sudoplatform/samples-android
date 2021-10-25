/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.start

import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.direlayexample.BaseRobot
import com.sudoplatform.direlayexample.R
import com.sudoplatform.direlayexample.postboxes.postboxes

fun start(func: StartRobot.() -> Unit) = StartRobot().apply { func() }

/**
 * Testing robot that manages the start screen.
 *
 * @since 2021-06-29
 */
class StartRobot : BaseRobot() {

    private val startButton = withId(R.id.buttonStart)

    private fun clickOnStart() {
        waitForViewToDisplay(startButton)
        clickOnView(startButton)
    }

    fun navigateToPostboxesScreen() {
        clickOnStart()
        postboxes {
            checkPostboxesItemsDisplayed()
        }
    }

    fun pressBackUntilAtStartIsDisplayed() {
        pressBackUntilViewIsDisplayed(startButton)
    }
}
