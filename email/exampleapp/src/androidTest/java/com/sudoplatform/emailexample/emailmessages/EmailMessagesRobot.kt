/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.emailaddressblocklist.emailAddressBlocklist

fun emailMessages(func: EmailMessagesRobot.() -> Unit) = EmailMessagesRobot().apply { func() }

/**
 * Testing robot that manages the email messages screen.
 */
class EmailMessagesRobot : BaseRobot() {

    private val toolbarComposeButton = withId(R.id.compose)
    private val emailMessagesRecyclerView = withId(R.id.emailMessageRecyclerView)
    private val loadingProgress = withId(R.id.progressBar)
    private val dropdownSpinner = withId(R.id.foldersSpinner)

    fun waitForLoading() {
        waitForViewToDisplay(loadingProgress, 5_000L)
        waitForViewToNotDisplay(loadingProgress, 60_000L)
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

    fun navigateToEditDraftEmailMessageScreen(position: Int) {
        clickRecyclerViewItem(position)
        sendEmailMessage {
            waitForLoading()
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

    fun clickEmailFolderSpinner(folderName: String) {
        clickOnView(dropdownSpinner)
        onView(withText(folderName)).perform(click())
    }

    fun navigateToBlocklistScreen() {
        Thread.sleep(1_000L)
        clickEmailFolderSpinner("BLOCKLIST")
        emailAddressBlocklist {
            waitForLoading()
            checkEmailAddressBlocklistItemsDisplayed()
        }
    }

    fun swipeLeftToDelete(position: Int) {
        checkRecyclerViewHasMinimumItemAmount(emailMessagesRecyclerView, position + 1)
        onView(emailMessagesRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<EmailMessageViewHolder>(
                position,
                ViewActions.swipeLeft(),
            ),
        )
    }

    private fun clickRecyclerViewItem(position: Int) {
        checkRecyclerViewHasMinimumItemAmount(emailMessagesRecyclerView, position + 1)
        onView(emailMessagesRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<EmailMessageViewHolder>(
                position,
                click(),
            ),
        )
    }
}
