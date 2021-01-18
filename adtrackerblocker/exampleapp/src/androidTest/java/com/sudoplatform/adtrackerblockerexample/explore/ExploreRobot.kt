/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.adtrackerblockerexample.explore

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.sudoplatform.adtrackerblockerexample.BaseRobot
import com.sudoplatform.adtrackerblockerexample.R
import com.sudoplatform.adtrackerblockerexample.register.login
import com.sudoplatform.adtrackerblockerexample.rulesets.rulesets
import com.sudoplatform.sudoadtrackerblocker.types.Ruleset

fun explore(func: ExploreRobot.() -> Unit) = ExploreRobot().apply { func() }

/**
 * Testing robot that manages the Explore screen.
 *
 * @since 2020-12-14
 */
class ExploreRobot : BaseRobot() {

    private val toolbar = withId(R.id.toolbar)
    private val checkedUrlText = withId(R.id.checkedUrlText)
    private val checkedUrlSpinner = withId(R.id.checkedUrlSpinner)
    private val rulesetAdsSwitch = withId(R.id.rulesetAdsSwitch)
    private val rulesetPrivacySwitch = withId(R.id.rulesetPrivacySwitch)
    private val rulesetSocialSwitch = withId(R.id.rulesetSocialSwitch)
    private val checkButton = withId(R.id.checkButton)
    private val resultText = withId(R.id.resultText)
    private val positiveAlertButton = withId(android.R.id.button1)
    private val loadingDialog = withId(R.id.progressBar)

    fun checkExploreItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(toolbar, timeout)
        waitForViewToDisplay(checkedUrlText, timeout)
        waitForViewToDisplay(checkedUrlSpinner, timeout)
        waitForViewToDisplay(rulesetAdsSwitch, timeout)
        waitForViewToDisplay(rulesetPrivacySwitch, timeout)
        waitForViewToDisplay(rulesetSocialSwitch, timeout)
        waitForViewToDisplay(checkButton, timeout)
        waitForViewToDisplay(resultText, timeout)
    }

    fun clickOnCheckButton() {
        clickOnView(checkButton)
    }

    fun clickOnPositiveAlertDialogButton() {
        checkAlertDialog()
        clickOnView(positiveAlertButton)
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
        rulesets {
            waitForLoading()
            waitForRecyclerView()
            checkItemsDisplayed(10_000L)
            clickOnKebabMenu()
            clickOnExplore()
        }
        checkExploreItemsDisplayed()
    }

    private fun checkAlertDialog() {
        waitForViewToDisplay(positiveAlertButton, 5_000L)
        onView(positiveAlertButton).check(matches(withText(android.R.string.ok)))
    }

    fun enterCheckedUrl(url: String) {
        replaceText(checkedUrlText, url)
    }

    fun clickOnRuleset(type: Ruleset.Type) {
        when (type) {
            Ruleset.Type.AD_BLOCKING -> clickOnView(rulesetAdsSwitch)
            Ruleset.Type.PRIVACY -> clickOnView(rulesetPrivacySwitch)
            Ruleset.Type.SOCIAL -> clickOnView(rulesetSocialSwitch)
            else -> throw AssertionError("Bad ruleset type")
        }
    }

    fun clickOnSpinner() {
        clickOnView(checkedUrlSpinner)
    }

    fun waitForLoading(timeout: Long = 2_000L) {
        waitForViewToDisplay(loadingDialog, timeout)
        waitForViewToNotDisplay(loadingDialog, timeout)
    }

    fun checkResultHasText(text: String) {
        onView(resultText).check(matches(withText(text)))
    }
}
