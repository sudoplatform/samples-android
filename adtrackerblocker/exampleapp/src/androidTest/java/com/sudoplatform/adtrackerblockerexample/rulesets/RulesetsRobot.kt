/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.adtrackerblockerexample.rulesets

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.sudoplatform.adtrackerblockerexample.BaseRobot
import com.sudoplatform.adtrackerblockerexample.R
import com.sudoplatform.adtrackerblockerexample.register.login

fun rulesets(func: RulesetsRobot.() -> Unit) = RulesetsRobot().apply { func() }

/**
 * Testing robot that manages the rulesets screen.
 *
 * @since 2020-12-04
 */
class RulesetsRobot : BaseRobot() {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val toolbar = withId(R.id.toolbar)
    private val toolbarSettingsButton = withText(context.getString(R.string.settings))
    private val toolbarExceptionsListButton = withText(context.getString(R.string.exceptions_list))
    private val toolbarExploreButton = withText(context.getString(R.string.explore))
    private val rulesetRecyclerView = withId(R.id.rulesetRecyclerView)
    private val tryAgain = withId(R.string.try_again)
    private val cancel = withId(android.R.string.cancel)
    private val loadingDialog = withId(R.id.progressBar)

    fun navigateFromLaunchToRulesets() {
        login {
            try {
                waitForRegisterButton()
                clickOnRegister()
            } catch (e: NoMatchingViewException) {
                // Login screen was skipped because already logged in
            }
        }
        checkItemsDisplayed(20_000L)
    }

    fun checkItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(toolbar, timeout)
    }

    fun checkListFailedDialogDisplayed() {
        onView(withText(R.string.list_rulesets_failure)).check(matches(isDisplayed()))
    }

    fun clickOnKebabMenu() {
        // From https://developer.android.com/training/testing/espresso/recipes
        openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext())
    }

    fun clickOnSettings() {
        clickOnView(toolbarSettingsButton)
    }

    fun clickOnExceptionsList() {
        clickOnView(toolbarExceptionsListButton)
    }

    fun clickOnExplore() {
        clickOnView(toolbarExploreButton)
    }

    fun clickOnTryAgain() {
        onView(withId(android.R.id.button1)).perform(click())
    }

    fun clickOnCancel() {
        onView(withId(android.R.id.button2)).perform(click())
    }

    fun waitForLoading(timeout: Long = 2_000L) {
        waitForViewToDisplay(loadingDialog, timeout)
        waitForViewToNotDisplay(loadingDialog, timeout)
    }

    fun waitForRecyclerView() {
        waitForViewToDisplay(rulesetRecyclerView, 5_000L)
    }
}
