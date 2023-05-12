/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.postbox

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.direlayexample.BaseRobot
import com.sudoplatform.direlayexample.R
import com.sudoplatform.direlayexample.postboxes.postboxes
import com.sudoplatform.direlayexample.register.login
import com.sudoplatform.virtualcardsexample.sudos.createSudo
import com.sudoplatform.virtualcardsexample.sudos.sudos

fun postbox(func: PostboxRobot.() -> Unit) = PostboxRobot().apply { func() }

/**
 * Testing robot that manages the Postbox screen.
 *
 * @since 2021-06-29
 */
class PostboxRobot : BaseRobot() {

    private val toolbar = withId(R.id.toolbar)
    private val toolbarDeregisterButton = withId(R.id.deregister)
    private val positiveAlertButton = withId(android.R.id.button1)

    private val loadingDialog = withId(R.id.progressBar)
    private val postboxServiceEndpoint = withId(R.id.serviceEndpoint)
    private val postboxEnabledSwitch = withId(R.id.postboxEnabledSwitch)
    private val messageList = withId(R.id.messageRecyclerView)
    private val sendMessageButton = withId(R.id.sendMessageButton)
    private val sendMessageText = withId(R.id.messageText)

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 5_000L, mayMissDisplay = true)
        waitForViewToNotDisplay(loadingDialog, 60_000L)
    }

    fun checkPostboxItemsDisplayed() {
        waitForViewToDisplay(postboxServiceEndpoint)
        waitForViewToDisplay(postboxEnabledSwitch)
        waitForViewToDisplay(sendMessageButton)
        waitForViewToDisplay(toolbar)
        waitForViewToDisplay(toolbarDeregisterButton)
    }

    private fun navigateFromLaunchToPostbox() {
        login {
            try {
                clickOnRegister()
            } catch (e: NoMatchingViewException) {
                // Login screen was skipped because already logged in
            }
        }
        sudos {
            checkSudosItemsDisplayed()
            waitForLoading()
            navigateToCreateSudoScreen()
        }
        createSudo {
            checkCreateSudoItemsDisplayed()
            setSudoName("Shopping")
            clickOnCreateButton()
            waitForLoading()
            clickPositiveAlertDialogButton()
        }
        postboxes {
            checkPostboxesItemsDisplayed()
            waitForLoading()
            createPostbox()
            waitForLoading()
            clickOnPostbox(0)
        }
        checkPostboxItemsDisplayed()
        waitForLoading()
    }

    fun createMessageFlow(messageText: String = "Creating test message") {
        navigateFromLaunchToPostbox()
        sendMessage(messageText)
        waitForViewToDisplay(messageList, 30_000L)
    }

    fun checkMessagesCount(count: Int) {
        waitForViewToHaveItems(messageList, count)
    }

    fun setPostboxEnabled() {
        setSwitch(postboxEnabledSwitch, true)
    }

    fun setPostboxDisabled() {
        setSwitch(postboxEnabledSwitch, false)
    }

    fun sendMessage(messageText: String) {
        replaceText(sendMessageText, messageText)
        clickOnView(sendMessageButton)
        clickPositiveAlertDialogButton()
        waitForLoading()
    }

    fun clickOnMessage(position: Int) {
        clickRecyclerViewItem(position)
    }

    fun deleteMessages(numMessages: Int) {
        for (i in 1..numMessages) {
            deleteMessage(0)
        }
    }

    fun deleteMessage(position: Int) {
        swipeLeftToDelete(position)
        waitForLoading()
    }

    private fun swipeLeftToDelete(position: Int) {
        onView(messageList).perform(
            RecyclerViewActions.actionOnItemAtPosition<MessageViewHolder>(
                position,
                ViewActions.swipeLeft()
            )
        )
    }

    private fun clickRecyclerViewItem(position: Int) {
        onView(messageList).perform(
            RecyclerViewActions.actionOnItemAtPosition<MessageViewHolder>(
                position,
                ViewActions.click()
            )
        )
    }

    fun clickPositiveAlertDialogButton() {
        checkAlertDialog()
        Thread.sleep(1000)
        clickOnView(positiveAlertButton)
    }

    private fun checkAlertDialog() {
        waitForViewToDisplay(positiveAlertButton, 60_000L)
        onView(positiveAlertButton)
            .check(ViewAssertions.matches(ViewMatchers.withText(android.R.string.ok)))
    }
}
