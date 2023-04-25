/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.virtualcards

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.virtualcardsexample.BaseRobot
import com.sudoplatform.virtualcardsexample.R

fun virtualCards(func: VirtualCardsRobot.() -> Unit) = VirtualCardsRobot().apply { func() }

/**
 * Testing robot that manages the Virtual Cards screen.
 */
class VirtualCardsRobot : BaseRobot() {

    private val createVirtualCardButton = withId(R.id.createVirtualCardButton)
    private val virtualCardRecyclerView = withId(R.id.virtualCardRecyclerView)
    private val loadingDialog = withId(R.id.progressBar)
    private val positiveAlertButton = withId(android.R.id.button1)

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 5_000L, true)
        waitForViewToNotDisplay(loadingDialog, 30_000L)
    }

    fun waitForRecyclerView() {
        waitForViewToDisplay(virtualCardRecyclerView, 30_000L)
    }

    fun checkVirtualCardsItemsDisplayed(timeout: Long = 60_000L) {
        waitForViewToDisplay(createVirtualCardButton, timeout)
    }

    fun navigateToCreateVirtualCardScreen() {
        clickOnView(createVirtualCardButton)
        createVirtualCard {
            checkCreateVirtualCardItemsDisplayed()
        }
    }

    fun navigateToVirtualCardDetailScreen(position: Int) {
        clickRecyclerViewItem(position)
        virtualCardDetail {
            checkVirtualCardDetailItemsDisplayed()
        }
    }

    fun clickOnPositiveAlertDialogButton() {
        waitForViewToDisplay(positiveAlertButton, 15_000L)
        Thread.sleep(1000)
        clickOnView(positiveAlertButton)
    }

    fun swipeLeftToCancel(position: Int) {
        onView(virtualCardRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<VirtualCardViewHolder>(
                position,
                ViewActions.swipeLeft()
            )
        )
    }

    private fun clickRecyclerViewItem(position: Int) {
        onView(virtualCardRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<VirtualCardViewHolder>(
                position,
                ViewActions.click()
            )
        )
    }
}
