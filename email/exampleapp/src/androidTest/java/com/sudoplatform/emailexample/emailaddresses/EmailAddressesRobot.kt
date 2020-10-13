/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaddresses

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.emailmessages.emailMessages

fun emailAddresses(func: EmailAddressesRobot.() -> Unit) = EmailAddressesRobot().apply { func() }

/**
 * Testing robot that manages the email addresses screen.
 *
 * @since 2020-08-6
 */
class EmailAddressesRobot : BaseRobot() {

    private val createEmailAddressButton = withId(R.id.createEmailAddressButton)
    private val emailAddressRecyclerView = withId(R.id.emailAddressRecyclerView)
    private val loadingProgress = withId(R.id.progressBar)
    private val positiveAlertButton = withId(android.R.id.button1)

    fun waitForLoading() {
        waitForViewToDisplay(loadingProgress, 5_000L)
        waitForViewToNotDisplay(loadingProgress, 10_000L)
    }

    fun checkEmailAddressesItemsDisplayed() {
        waitForViewToDisplay(createEmailAddressButton)
    }

    fun waitForRecyclerView() {
        waitForViewToDisplay(emailAddressRecyclerView, 5_000L)
    }

    fun navigateToProvisionEmailAddressScreen() {
        clickOnView(createEmailAddressButton)
        provisionEmailAddress {
            waitForLoading()
            checkProvisionEmailAddressItemsDisplayed()
        }
    }

    fun clickOnPositiveAlertDialogButton() {
        waitForViewToDisplay(positiveAlertButton, 15_000L)
        clickOnView(positiveAlertButton)
    }

    fun navigateToEmailMessagesScreen(position: Int) {
        clickRecyclerViewItem(position)
        emailMessages {
            waitForLoading()
            checkEmailMessagesItemsDisplayed()
        }
    }

    fun swipeLeftToDelete(position: Int) {
        Espresso.onView(emailAddressRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<EmailAddressViewHolder>(position,
                ViewActions.swipeLeft()
            )
        )
    }

    private fun clickRecyclerViewItem(position: Int) {
        Espresso.onView(emailAddressRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<EmailAddressViewHolder>(position,
                ViewActions.click()
            )
        )
    }
}
