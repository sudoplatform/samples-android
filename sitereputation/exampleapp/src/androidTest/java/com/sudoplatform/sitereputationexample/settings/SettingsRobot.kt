/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sitereputationexample.settings

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.sitereputationexample.BaseRobot
import com.sudoplatform.sitereputationexample.R

fun settings(func: SettingsRobot.() -> Unit) = SettingsRobot().apply { func() }

/**
 * Testing robot that manages the settings screen.
 *
 * @since 2020-12-04
 */
class SettingsRobot : BaseRobot() {
    private val signOutButton = withId(R.id.signOutButton)
    private val clearStorageButton = withId(R.id.clearStorageButton)
    private val positiveAlertButton = withId(android.R.id.button1)

    fun checkItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(signOutButton, timeout)
        waitForViewToDisplay(clearStorageButton, timeout)
    }

    fun clickOnSignOut() {
        clickOnView(signOutButton)
    }

    fun clickOnClearStorage() {
        clickOnView(clearStorageButton)
    }

    private fun checkAlertDialog() {
        waitForViewToDisplay(positiveAlertButton, 5_000L)
        Espresso.onView(positiveAlertButton)
            .check(ViewAssertions.matches(ViewMatchers.withText(R.string.clear_storage)))
    }

    fun clickOnPositiveAlertDialogButton() {
        checkAlertDialog()
        clickOnView(positiveAlertButton)
    }
}
