/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.adtrackerblockerexample.settings

import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.adtrackerblockerexample.BaseRobot
import com.sudoplatform.adtrackerblockerexample.R

fun settings(func: SettingsRobot.() -> Unit) = SettingsRobot().apply { func() }

/**
 * Testing robot that manages the settings screen.
 *
 * @since 2020-12-04
 */
class SettingsRobot : BaseRobot() {
    private val signOutButton = withId(R.id.signOutButton)
    private val resetButton = withId(R.id.resetButton)

    fun checkItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(signOutButton, timeout)
        waitForViewToDisplay(resetButton, timeout)
    }

    fun clickOnSignOut() {
        clickOnView(signOutButton)
    }

    fun clickOnReset() {
        clickOnView(resetButton)
    }
}
