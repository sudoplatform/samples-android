/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.mainmenu

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.virtualcardsexample.BaseRobot
import com.sudoplatform.virtualcardsexample.R
import com.sudoplatform.virtualcardsexample.fundingsources.fundingSources
import com.sudoplatform.virtualcardsexample.register.login
import com.sudoplatform.virtualcardsexample.sudos.sudos

fun mainMenu(func: MainMenuRobot.() -> Unit) = MainMenuRobot().apply { func() }

/**
 * Testing robot that manages the Main Menu screen.
 */
class MainMenuRobot : BaseRobot() {

    private val toolbar = withId(R.id.toolbar)
    private val toolbarDeregisterButton = withId(R.id.deregister)
    private val toolbarInfoButton = withId(R.id.info)
    private val loadingDialog = withId(R.id.progressBar)
    private val secureIdVerificationButton = withId(R.id.secureIdVerificationButton)
    private val fundingSourcesButton = withId(R.id.fundingSourcesButton)
    private val sudosButton = withId(R.id.sudosButton)
    private val orphanVirtualCardsButton = withId(R.id.orphanVirtualCardsButton)
    private val positiveAlertButton = withId(android.R.id.button1)
    private val negativeAlertButton = withId(android.R.id.button2)

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 5_000L)
        waitForViewToNotDisplay(loadingDialog, 10_000L)
    }

    fun checkMainMenuItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(toolbar, timeout)
        waitForViewToDisplay(secureIdVerificationButton, timeout)
        waitForViewToDisplay(fundingSourcesButton, timeout)
        waitForViewToDisplay(sudosButton, timeout)
        waitForViewToDisplay(orphanVirtualCardsButton, timeout)
    }

    fun navigateToIdentityVerificationScreen() {
        clickOnView(secureIdVerificationButton)
    }

    fun navigateToFundingSourcesScreen() {
        clickOnView(fundingSourcesButton)
        fundingSources {
            checkFundingSourcesItemsDisplayed()
        }
    }

    fun navigateToSudosScreen() {
        clickOnView(sudosButton)
        sudos {
            checkSudosItemsDisplayed()
        }
    }

    fun navigateToOrphanVirtualCardsScreen() {
        clickOnView(orphanVirtualCardsButton)
    }

    fun navigateFromLaunchToMainMenu() {
        login {
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

    fun clickOnInfo() {
        clickOnView(toolbarInfoButton)
    }

    fun clickOnPositiveInfoAlertDialogButton() {
        checkInfoAlertDialog()
        Thread.sleep(1000)
        clickOnView(positiveAlertButton)
    }

    fun clickOnPositiveDeregisterAlertDialogButton() {
        checkDeregisterAlertDialog()
        Thread.sleep(1000)
        clickOnView(positiveAlertButton)
    }

    fun clickOnNegativeDeregisterAlertDialogButton() {
        checkDeregisterAlertDialog()
        Thread.sleep(1000)
        clickOnView(negativeAlertButton)
    }

    fun pressBackUntilDeregisterToolbarButtonIsDisplayed() {
        pressBackUntilViewIsDisplayed(toolbarDeregisterButton)
    }

    private fun checkInfoAlertDialog() {
        waitForViewToDisplay(positiveAlertButton, 15_000L)
        waitForViewToDisplay(negativeAlertButton, 15_000L)
        onView(positiveAlertButton)
            .check(ViewAssertions.matches(ViewMatchers.withText(android.R.string.ok)))
        onView(negativeAlertButton)
            .check(ViewAssertions.matches(ViewMatchers.withText(R.string.learn_more)))
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
