/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.R

fun readEmailMessage(func: ReadEmailMessageRobot.() -> Unit) = ReadEmailMessageRobot().apply { func() }

/**
 * Testing robot that manages the read email message screen.
 */
class ReadEmailMessageRobot : BaseRobot() {

    private val toolbarReplyButton = withId(R.id.reply)
    private val toolbarForwardButton = withId(R.id.forward)
    private val toolbarBlockEmailAddressButton = withId(R.id.block)
    private val fromValue = withId(R.id.fromValue)
    private val fromLabel = withId(R.id.fromLabel)
    private val dateValue = withId(R.id.dateValue)
    private val toValue = withId(R.id.toValue)
    private val toLabel = withId(R.id.toLabel)
    private val ccValue = withId(R.id.ccValue)
    private val ccLabel = withId(R.id.ccLabel)
    private val subjectField = withId(R.id.subject)
    private val bodyField = withId(R.id.contentBody)
    private val loadingDialog = withId(R.id.progressBar)
    private val positiveAlertButton = withId(android.R.id.button1)

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 5_000L)
        waitForViewToNotDisplay(loadingDialog, 60_000L)
    }

    fun checkReadEmailMessageItemsDisplayed() {
        waitForViewToDisplay(toolbarReplyButton)
        waitForViewToDisplay(toolbarForwardButton)
        waitForViewToDisplay(fromValue)
        waitForViewToDisplay(fromLabel)
        waitForViewToDisplay(dateValue)
        waitForViewToDisplay(toValue)
        waitForViewToDisplay(toLabel)
        waitForViewToDisplay(ccValue)
        waitForViewToDisplay(ccLabel)
        waitForViewToDisplay(subjectField)
        waitForViewToDisplay(bodyField)
    }

    fun navigateToReplyScreen() {
        clickOnView(toolbarReplyButton)
        sendEmailMessage {
            checkSendEmailMessageItemsDisplayed()
        }
    }

    fun navigateToForwardScreen() {
        clickOnView(toolbarForwardButton)
        sendEmailMessage {
            checkSendEmailMessageItemsDisplayed()
        }
    }

    fun clickOnBlockEmailAddressButton() {
        clickOnView(toolbarBlockEmailAddressButton)
    }

    fun clickOnPositiveAlertDialogButton() {
        checkAlertDialog()
        Thread.sleep(1_000L)
        clickOnView(positiveAlertButton)
    }

    private fun checkAlertDialog() {
        waitForViewToDisplay(positiveAlertButton, 60_000L)
        Thread.sleep(1_000L)
        Espresso.onView(positiveAlertButton)
            .check(ViewAssertions.matches(ViewMatchers.withText(android.R.string.ok)))
    }
}
