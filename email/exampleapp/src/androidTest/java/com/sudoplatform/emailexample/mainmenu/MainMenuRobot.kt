/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.mainmenu

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.register.login
import com.sudoplatform.emailexample.sudos.sudos

fun mainMenu(func: MainMenuRobot.() -> Unit) = MainMenuRobot().apply { func() }

/**
 * Testing robot that manages the Main Menu screen.
 */
class MainMenuRobot : BaseRobot() {

    private val toolbar = withId(R.id.toolbar)
    private val toolbarDeregisterButton = withId(R.id.deregister)
    private val loadingDialog = withId(R.id.progressBar)
    private val sudosButton = withId(R.id.sudosButton)
    private val positiveAlertButton = withId(android.R.id.button1)
    private val negativeAlertButton = withId(android.R.id.button2)

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 5_000L)
        waitForViewToNotDisplay(loadingDialog, 60_000L)
    }

    fun navigateToSudosScreen() {
        clickOnView(sudosButton)
        sudos {
            checkSudosItemsDisplayed()
        }
    }

    fun navigateFromLaunchToMainMenu() {
        login {
            clickNotificationPermissionDialog()
            try {
                clickOnRegister()
            } catch (e: NoMatchingViewException) {
                // Login screen was skipped because already logged in
            }
        }
        checkMainMenuItemsDisplayed(60_000L)
    }

    fun clickOnDeregister() {
        clickOnView(toolbarDeregisterButton)
    }

    fun clickOnPositiveDeregisterAlertDialogButton() {
        checkDeregisterAlertDialog()
        Thread.sleep(1_000L)
        clickOnView(positiveAlertButton)
    }

    fun clickOnNegativeDeregisterAlertDialogButton() {
        checkDeregisterAlertDialog()
        Thread.sleep(1_000L)
        clickOnView(negativeAlertButton)
    }

    fun pressBackUntilDeregisterToolbarButtonIsDisplayed() {
        pressBackUntilViewIsDisplayed(toolbarDeregisterButton)
    }

    private fun checkMainMenuItemsDisplayed(timeout: Long = 1_000L) {
        waitForViewToDisplay(toolbar, timeout)
        waitForViewToDisplay(sudosButton, timeout)
    }

    private fun checkDeregisterAlertDialog() {
        waitForViewToDisplay(positiveAlertButton, 15_000L)
        waitForViewToDisplay(negativeAlertButton, 15_000L)
        onView(positiveAlertButton)
            .check(ViewAssertions.matches(ViewMatchers.withText(R.string.deregister)))
        onView(negativeAlertButton)
            .check(ViewAssertions.matches(ViewMatchers.withText(android.R.string.cancel)))
    }
}
