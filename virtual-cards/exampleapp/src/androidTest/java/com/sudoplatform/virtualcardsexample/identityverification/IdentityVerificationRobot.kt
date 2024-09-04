/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.identityverification

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.sudoplatform.virtualcardsexample.BaseRobot
import com.sudoplatform.virtualcardsexample.R
import junit.framework.AssertionFailedError

fun identityVerification(func: IdentityVerificationRobot.() -> Unit) = IdentityVerificationRobot().apply { func() }

/**
 * Testing robot that manages the Secure Identity Verification screen.
 */
class IdentityVerificationRobot : BaseRobot() {
    private val loadingDialog = withId(R.id.progressBar)
    private val toolBarVerifyButton = withId(R.id.verify)
    private val inputFormRecyclerView = withId(R.id.formRecyclerView)
    private val statusLabel = withId(R.id.statusLabel)
    private val learnMoreTextView = withId(R.id.learnMoreTextView)
    private val learnMoreButton = withId(R.id.learnMoreButton)
    private val positiveAlertButton = withId(android.R.id.button1)

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 5_000L, true)
        waitForViewToNotDisplay(loadingDialog, 20_000L)
    }

    fun checkIdentityVerificationItemsDisplayed() {
        waitForViewToDisplay(toolBarVerifyButton)
        waitForViewToDisplay(inputFormRecyclerView)
        waitForViewToDisplay(statusLabel)
        scrollToView(learnMoreButton)
        waitForViewToDisplay(learnMoreTextView)
        waitForViewToDisplay(learnMoreButton)
    }

    fun clickOnVerifyButton() {
        clickOnView(toolBarVerifyButton)
    }

    fun clickOnPositiveAlertDialogButton() {
        checkAlertDialog()
        Thread.sleep(1000)
        clickOnView(positiveAlertButton)
    }

    fun isVerified(): Boolean {
        var verified = true
        try {
            onView(statusLabel).check(
                matches(
                    withText("Verified"),
                ),
            ).withFailureHandler { _, _ ->
                verified = false
            }
        } catch (e: NoMatchingViewException) {
            println("NoMatchingViewException Exception")
            verified = false
        } catch (e: AssertionFailedError) {
            println("AssertionFailedError Exception")
            verified = false
        }
        return verified
    }

    private fun checkAlertDialog() {
        waitForViewToDisplay(positiveAlertButton, 15_000L)
        onView(positiveAlertButton)
            .check(matches(withText(android.R.string.ok)))
    }
}
