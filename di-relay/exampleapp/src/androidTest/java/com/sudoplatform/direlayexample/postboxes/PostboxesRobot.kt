/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.postboxes

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.direlayexample.BaseRobot
import com.sudoplatform.direlayexample.R
import com.sudoplatform.direlayexample.register.login
import com.sudoplatform.virtualcardsexample.sudos.createSudo
import com.sudoplatform.virtualcardsexample.sudos.sudos
import org.hamcrest.Matcher

fun postboxes(func: PostboxesRobot.() -> Unit) = PostboxesRobot().apply { func() }

/**
 * Testing robot that manages the Postboxes screen.
 *
 * @since 2021-06-29
 */
class PostboxesRobot : BaseRobot() {

    private val toolbar = withId(R.id.toolbar)
    private val toolbarDeregisterButton = withId(R.id.deregister)
    private val positiveAlertButton = withId(android.R.id.button1)
    private val negativeAlertButton = withId(android.R.id.button2)

    private val loadingDialog = withId(R.id.progressBar)
    private val createPostboxButton = withId(R.id.createPostboxButton)
    private val postboxList = withId(R.id.postboxRecyclerView)

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 5_000L, mayMissDisplay = true)
        waitForViewToNotDisplay(loadingDialog, 60_000L)
    }

    fun checkPostboxesItemsDisplayed() {
        waitForViewToDisplay(createPostboxButton)
        waitForViewToDisplay(toolbar)
        waitForViewToDisplay(toolbarDeregisterButton)
    }

    fun pressBackUntilPostboxesDisplayed() {
        pressBackUntilViewIsDisplayed(createPostboxButton)
        waitForLoading()
    }

    fun navigateFromLaunchToPostboxes() {
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
        checkPostboxesItemsDisplayed()
        waitForLoading()
    }

    fun createPostboxFlow() {
        navigateFromLaunchToPostboxes()
        createPostbox()
        waitForViewToDisplay(postboxList, 30_000L)
    }

    fun createPostbox() {
        clickOnView(createPostboxButton)
        waitForLoading()
    }

    fun clickOnPostbox(position: Int) {
        clickRecyclerViewItem(position)
    }

    fun deletePostboxes(numPostboxes: Int) {
        for (i in 1..numPostboxes) {
            deletePostbox(0)
        }
    }

    fun deletePostbox(position: Int) {
        swipeLeftToDelete(position)
        waitForLoading()
    }

    fun getPostboxId(position: Int): String {
        val storeTextViewAction = object : ViewAction {
            lateinit var result: String
            override fun getConstraints(): Matcher<View>? {
                return null
            }

            override fun getDescription(): String {
                return "get Contents"
            }

            override fun perform(uiController: UiController?, view: View) {
                val v = view.findViewById<TextView>(R.id.name)
                result = v.text.toString()
            }
        }
        onView(withId(R.id.messageRecyclerView))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<ViewHolder>(
                    position,
                    storeTextViewAction
                )
            )
        return storeTextViewAction.result
    }

    private fun swipeLeftToDelete(position: Int) {
        onView(postboxList).perform(
            RecyclerViewActions.actionOnItemAtPosition<PostboxViewHolder>(
                position,
                ViewActions.swipeLeft()
            )
        )
    }

    private fun clickRecyclerViewItem(position: Int) {
        onView(postboxList).perform(
            RecyclerViewActions.actionOnItemAtPosition<PostboxViewHolder>(
                position,
                ViewActions.click()
            )
        )
    }

    private fun clickOnDeregister() {
        clickOnView(toolbarDeregisterButton)
    }

    private fun clickOnPositiveDeregisterAlertDialogButton() {
        checkDeregisterAlertDialog()
        Thread.sleep(1_000L)
        clickOnView(positiveAlertButton)
    }

    private fun checkDeregisterAlertDialog() {
        waitForViewToDisplay(positiveAlertButton, 15_000L)
        waitForViewToDisplay(negativeAlertButton, 15_000L)
        onView(positiveAlertButton)
            .check(ViewAssertions.matches(ViewMatchers.withText(R.string.deregister)))
        onView(negativeAlertButton)
            .check(ViewAssertions.matches(ViewMatchers.withText(android.R.string.cancel)))
    }

    fun deregisterCleanUpFlow() {
        pressBackUntilPostboxesDisplayed()
        checkPostboxesItemsDisplayed()
        clickOnDeregister()
        clickOnPositiveDeregisterAlertDialogButton()
        waitForLoading()

        login {
            checkRegisterItemsDisplayed()
        }
    }
}
