/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-Licensee-Identifier: Apache-2.0
 */

package com.sudoplatform.sitereputationexample.explore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.sudoplatform.sitereputationexample.BaseRobot
import com.sudoplatform.sitereputationexample.R
import com.sudoplatform.sitereputationexample.register.login

fun explore(func: ExploreRobot.() -> Unit) = ExploreRobot().apply { func() }

/**
 * Testing robot that manages the Explore screen.
 *
 * @since 2021-01-06
 */
class ExploreRobot : BaseRobot() {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val toolbar = withId(R.id.toolbar)
    private val updatedText = withId(R.id.lastUpdatedTextView)
    private val updateButton = withId(R.id.updateButton)
    private val checkedUrlText = withId(R.id.checkedUrlText)
    private val checkedUrlSpinner = withId(R.id.checkedUrlSpinner)
    private val checkButton = withId(R.id.checkButton)
    private val resultText = withId(R.id.resultText)
    private val positiveAlertButton = withId(android.R.id.button1)
    private val loadingDialog = withId(R.id.progressBar)
    private val toolbarSettingsButton = ViewMatchers.withText(context.getString(R.string.settings))

    fun checkExploreItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(toolbar, timeout)
        waitForViewToDisplay(updatedText, timeout)
        waitForViewToDisplay(updateButton, timeout)
        waitForViewToDisplay(checkedUrlText, timeout)
        waitForViewToDisplay(checkedUrlSpinner, timeout)
        waitForViewToDisplay(checkButton, timeout)
        waitForViewToDisplay(resultText, timeout)
    }

    fun clickOnSettings() {
        clickOnView(toolbarSettingsButton)
    }

    fun clickOnUpdateButton() {
        clickOnView(updateButton)
    }

    fun clickOnCheckButton() {
        clickOnView(checkButton)
    }

    fun clickOnPositiveAlertDialogButton() {
        checkAlertDialog()
        clickOnView(positiveAlertButton)
    }

    fun checkUpdatedHasText(text: String) {
        Espresso.onView(updatedText).check(ViewAssertions.matches(ViewMatchers.withText(text)))
    }

    fun checkUpdatedDoesNotHaveText(text: String) {
        Espresso.onView(withText(text)).check(doesNotExist())
    }

    private fun checkAlertDialog() {
        waitForViewToDisplay(positiveAlertButton, 5_000L)
        Espresso.onView(positiveAlertButton)
            .check(ViewAssertions.matches(ViewMatchers.withText(android.R.string.ok)))
    }

    fun navigateFromLaunchToExplore() {
        login {
            try {
                waitForRegisterButton()
                clickOnRegister()
            } catch (e: NoMatchingViewException) {
                // Login screen was skipped because already logged in
            }
        }
        checkExploreItemsDisplayed()
    }

    fun enterCheckedUrl(url: String) {
        replaceText(checkedUrlText, url)
    }

    fun clickOnSpinner() {
        clickOnView(checkedUrlSpinner)
    }

    fun clickOnSpinnerItemWithText(text: String) {
        onView(withText(text)).perform(click())
    }

    fun waitForLoading(timeout: Long = 2_000L) {
        waitForViewToDisplay(loadingDialog, timeout)
        waitForViewToNotDisplay(loadingDialog, timeout)
    }

    fun checkResultHasText(text: String) {
        Espresso.onView(resultText).check(ViewAssertions.matches(ViewMatchers.withText(text)))
    }
}
