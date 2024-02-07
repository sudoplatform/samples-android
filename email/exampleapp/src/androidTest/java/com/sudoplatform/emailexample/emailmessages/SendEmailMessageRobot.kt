/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.R
import org.hamcrest.CoreMatchers.not

fun sendEmailMessage(func: SendEmailMessageRobot.() -> Unit) =
    SendEmailMessageRobot().apply { func() }

/**
 * Testing robot that manages the send email message screen.
 */
class SendEmailMessageRobot : BaseRobot() {

    private val toolbarSendButton = withId(R.id.send)
    private val toolbarSaveButton = withId(R.id.save)
    private val toField = R.id.toTextView
    private val toLabel = withId(R.id.toLabel)
    private val ccField = R.id.ccTextView
    private val ccLabel = withId(R.id.ccLabel)
    private val bccField = R.id.bccTextView
    private val bccLabel = withId(R.id.bccLabel)
    private val subjectField = R.id.subjectTextView
    private val subjectLabel = withId(R.id.subjectLabel)
    private val bodyField = R.id.contentBody
    private val loadingDialog = withId(R.id.progressBar)
    private val positiveAlertButton = withId(android.R.id.button1)
    private val negativeAlertButton = withId(android.R.id.button2)

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 5_000L)
        waitForViewToNotDisplay(loadingDialog, 60_000L)
    }

    fun checkSendEmailMessageItemsDisplayed() {
        waitForViewToDisplay(toolbarSendButton)
        waitForViewToDisplay(withId(toField))
        waitForViewToDisplay(toLabel)
        waitForViewToDisplay(withId(ccField))
        waitForViewToDisplay(ccLabel)
        waitForViewToDisplay(withId(bccField))
        waitForViewToDisplay(bccLabel)
        waitForViewToDisplay(withId(subjectField))
        waitForViewToDisplay(subjectLabel)
        waitForViewToDisplay(withId(bodyField))
    }

    fun clickOnSendEmailButton() {
        clickOnView(toolbarSendButton)
    }

    fun clickOnSaveDraftEmailButton() {
        clickOnView(toolbarSaveButton)
    }

    fun clickOnPositiveAlertDialogButton() {
        checkAlertDialog()
        Thread.sleep(1_000L)
        clickOnView(positiveAlertButton)
    }

    fun clickOnNegativeErrorAlertDialogButton() {
        checkErrorAlertDialog()
        Thread.sleep(1_000L)
        clickOnView(negativeAlertButton)
    }

    fun setToField(to: String) {
        fillEditText(toField, to)
    }

    fun setCcField(cc: String) {
        fillEditText(ccField, cc)
    }

    fun setBccField(bcc: String) {
        fillEditText(bccField, bcc)
    }

    fun setSubjectField(subject: String) {
        fillEditText(subjectField, subject)
    }

    fun setContentBodyField(body: String) {
        fillEditText(bodyField, body)
    }

    fun checkToFieldFilled() {
        onView(withId(toField)).check(matches(not(withText(""))))
    }

    fun checkSubjectFieldFilled() {
        onView(withId(subjectField)).check(matches(withSubstring("Re:")))
    }

    private fun checkAlertDialog() {
        waitForViewToDisplay(positiveAlertButton, 60_000L)
        Thread.sleep(1_000L)
        onView(positiveAlertButton)
            .check(matches(withText(android.R.string.ok)))
    }

    private fun checkErrorAlertDialog() {
        waitForViewToDisplay(positiveAlertButton, 15_000L)
        waitForViewToDisplay(negativeAlertButton, 15_000L)
        onView(positiveAlertButton)
            .check(matches(withText(R.string.try_again)))
        onView(negativeAlertButton)
            .check(matches(withText(android.R.string.cancel)))
    }
}
