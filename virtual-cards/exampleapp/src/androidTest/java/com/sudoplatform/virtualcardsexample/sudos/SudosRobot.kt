/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.sudos

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.sudoplatform.virtualcardsexample.BaseRobot
import com.sudoplatform.virtualcardsexample.R
import com.sudoplatform.virtualcardsexample.virtualcards.virtualCards

fun sudos(func: SudosRobot.() -> Unit) = SudosRobot().apply { func() }

/**
 * Testing robot that manages the Sudos screen.
 */
class SudosRobot : BaseRobot() {

    private val toolbarInfoButton = withId(R.id.info)
    private val createSudoButton = withId(R.id.createSudoButton)
    private val sudoRecyclerView = withId(R.id.sudoRecyclerView)
    private val loadingDialog = withId(R.id.progressBar)
    private val positiveAlertButton = withId(android.R.id.button1)
    private val negativeAlertButton = withId(android.R.id.button2)

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 5_000L, true)
        waitForViewToNotDisplay(loadingDialog, 10_000L)
    }

    fun waitForRecyclerView() {
        waitForViewToDisplay(sudoRecyclerView, 5_000L)
    }

    fun checkSudosItemsDisplayed() {
        waitForViewToDisplay(toolbarInfoButton)
        waitForViewToDisplay(createSudoButton)
    }

    fun navigateToCreateSudoScreen() {
        clickOnView(createSudoButton)
        createSudo {
            checkCreateSudoItemsDisplayed()
        }
    }

    fun navigateToVirtualCardsScreen(position: Int) {
        clickRecyclerViewItem(position)
        virtualCards {
            checkVirtualCardsItemsDisplayed()
        }
    }

    fun clickOnInfo() {
        clickOnView(toolbarInfoButton)
    }

    fun clickOnPositiveInfoAlertDialogButton() {
        checkInfoAlertDialog()
        Thread.sleep(1000)
        clickOnView(positiveAlertButton)
    }

    fun clickOnPositiveAlertDialogButton() {
        checkAlertDialog()
        Thread.sleep(1000)
        clickOnView(positiveAlertButton)
    }

    private fun clickRecyclerViewItem(position: Int) {
        onView(sudoRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<SudoViewHolder>(
                position,
                ViewActions.click(),
            ),
        )
    }

    fun swipeLeftToDelete(position: Int) {
        onView(sudoRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<SudoViewHolder>(
                position,
                ViewActions.swipeLeft(),
            ),
        )
    }

    private fun checkInfoAlertDialog() {
        waitForViewToDisplay(positiveAlertButton, 15_000L)
        waitForViewToDisplay(negativeAlertButton, 15_000L)
        onView(positiveAlertButton).check(matches(withText(android.R.string.ok)))
        onView(negativeAlertButton).check(matches(withText(R.string.learn_more)))
    }

    private fun checkAlertDialog() {
        waitForViewToDisplay(positiveAlertButton, 60_000L)
        onView(positiveAlertButton).check(matches(withText(android.R.string.ok)))
    }
}
