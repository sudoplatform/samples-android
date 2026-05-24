/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.R
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.instanceOf

fun sendMaskedEmail(func: SendMaskedEmailRobot.() -> Unit) = SendMaskedEmailRobot().apply { func() }

/**
 * Testing robot that manages the send masked email interactions.
 */
class SendMaskedEmailRobot : BaseRobot() {
    private val emailMaskSpinner = withId(R.id.emailMaskSpinner)
    private val loadingDialog = withId(R.id.progressBar)

    fun checkEmailMaskSpinnerDisplayed() {
        waitForViewToDisplay(emailMaskSpinner)
    }

    fun selectEmailMask(position: Int) {
        clickOnView(emailMaskSpinner)
        onView(isRoot()).perform(waitFor(500L))
        onData(instanceOf(String::class.java))
            .atPosition(position)
            .perform(click())
    }

    fun checkEmailMaskSpinnerDefaultSelected() {
        onView(isRoot()).perform(waitFor(2_000L))
        onView(emailMaskSpinner)
            .check(matches(withSpinnerText(containsString("None (send from email address)"))))
    }

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 5_000L)
        waitForViewToNotDisplay(loadingDialog, 60_000L)
    }
}
