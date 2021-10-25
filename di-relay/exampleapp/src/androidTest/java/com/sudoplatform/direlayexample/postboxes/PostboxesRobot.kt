/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.postboxes

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.direlayexample.BaseRobot
import com.sudoplatform.direlayexample.R
import com.sudoplatform.direlayexample.start.start
import org.hamcrest.Matcher

fun postboxes(func: PostboxesRobot.() -> Unit) = PostboxesRobot().apply { func() }

/**
 * Testing robot that manages the Postboxes screen.
 *
 * @since 2021-06-29
 */
class PostboxesRobot : BaseRobot() {

    private val loadingDialog = withId(R.id.progressBar)
    private val createPostboxButton = withId(R.id.createPostboxButton)
    private val postboxList = withId(R.id.postboxRecyclerView)

    private fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 5_000L)
        waitForViewToNotDisplay(loadingDialog, 60_000L)
    }

    fun checkPostboxesItemsDisplayed() {
        waitForViewToDisplay(postboxList)
        waitForViewToDisplay(createPostboxButton)
    }

    fun pressBackUntilPostboxesDisplayed() {
        pressBackUntilViewIsDisplayed(createPostboxButton)
    }

    fun createPostboxFlow() {
        start {
            navigateToPostboxesScreen()
        }
        createPostbox()
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
        onView(withId(R.id.postboxRecyclerView))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<ViewHolder>(
                    position, storeTextViewAction
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
}
