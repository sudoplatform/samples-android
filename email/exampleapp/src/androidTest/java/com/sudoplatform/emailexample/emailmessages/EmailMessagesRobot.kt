/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.R

fun emailMessages(func: EmailMessagesRobot.() -> Unit) = EmailMessagesRobot().apply { func() }

/**
 * Testing robot that manages the email messages screen.
 *
 * @since 2020-08-18
 */
class EmailMessagesRobot : BaseRobot() {

    private val toolbarComposeButton = withId(R.id.compose)
    private val emailMessagesRecyclerView = withId(R.id.emailMessageRecyclerView)
    private val loadingProgress = withId(R.id.progressBar)
    private val positiveAlertButton = withId(android.R.id.button1)

    fun waitForLoading() {
        waitForViewToDisplay(loadingProgress, 5_000L)
        waitForViewToNotDisplay(loadingProgress, 10_000L)
    }

    fun checkEmailMessagesItemsDisplayed() {
        waitForViewToDisplay(toolbarComposeButton)
    }

    fun waitForRecyclerView() {
        waitForViewToDisplay(emailMessagesRecyclerView, 5_000L)
    }

    fun navigateToSendEmailMessageScreen() {
        clickOnView(toolbarComposeButton)
        sendEmailMessage {
            checkSendEmailMessageItemsDisplayed()
        }
    }

    fun navigateToReadEmailMessageScreen(position: Int) {
        clickRecyclerViewItem(position)
        readEmailMessage {
            waitForLoading()
            checkReadEmailMessageItemsDisplayed()
        }
    }

    fun clickOnPositiveAlertDialogButton() {
        checkAlertDialog()
        clickOnView(positiveAlertButton)
    }

    fun swipeLeftToDelete(position: Int) {
        onView(emailMessagesRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<EmailMessageViewHolder>(position,
                ViewActions.swipeLeft()
            )
        )
    }

    private fun checkAlertDialog() {
        waitForViewToDisplay(positiveAlertButton, 15_000L)
        onView(positiveAlertButton).check(matches(withText(android.R.string.ok)))
    }

    private fun clickRecyclerViewItem(position: Int) {
        onView(emailMessagesRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<EmailMessageViewHolder>(position,
                ViewActions.click()
            )
        )
    }
}
