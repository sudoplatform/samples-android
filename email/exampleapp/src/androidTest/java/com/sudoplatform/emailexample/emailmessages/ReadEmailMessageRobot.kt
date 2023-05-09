/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.R

fun readEmailMessage(func: ReadEmailMessageRobot.() -> Unit) = ReadEmailMessageRobot().apply { func() }

/**
 * Testing robot that manages the read email message screen.
 */
class ReadEmailMessageRobot : BaseRobot() {

    private val toolbarReplyButton = withId(R.id.reply)
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

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 5_000L)
        waitForViewToNotDisplay(loadingDialog, 60_000L)
    }

    fun checkReadEmailMessageItemsDisplayed() {
        waitForViewToDisplay(toolbarReplyButton)
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
}
