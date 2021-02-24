/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.cards

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.virtualcardsexample.BaseRobot
import com.sudoplatform.virtualcardsexample.R

fun cards(func: CardsRobot.() -> Unit) = CardsRobot().apply { func() }

/**
 * Testing robot that manages the Cards screen.
 *
 * @since 2020-07-29
 */
class CardsRobot : BaseRobot() {

    private val createCardButton = withId(R.id.createCardButton)
    private val cardRecyclerView = withId(R.id.cardRecyclerView)
    private val loadingDialog = withId(R.id.progressBar)
    private val positiveAlertButton = withId(android.R.id.button1)

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 5_000L, true)
        waitForViewToNotDisplay(loadingDialog, 30_000L)
    }

    fun waitForRecyclerView() {
        waitForViewToDisplay(cardRecyclerView, 30_000L)
    }

    fun checkCardsItemsDisplayed(timeout: Long = 60_000L) {
        waitForViewToDisplay(createCardButton, timeout)
    }

    fun navigateToCreateCardScreen() {
        clickOnView(createCardButton)
        createCard {
            checkCreateCardItemsDisplayed()
        }
    }

    fun navigateToCardDetailScreen(position: Int) {
        clickRecyclerViewItem(position)
        cardDetail {
            checkCardDetailItemsDisplayed()
        }
    }

    fun clickOnPositiveAlertDialogButton() {
        waitForViewToDisplay(positiveAlertButton, 15_000L)
        Thread.sleep(1000)
        clickOnView(positiveAlertButton)
    }

    fun swipeLeftToCancel(position: Int) {
        onView(cardRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<CardViewHolder>(
                position,
                ViewActions.swipeLeft()
            )
        )
    }

    private fun clickRecyclerViewItem(position: Int) {
        onView(cardRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<CardViewHolder>(
                position,
                ViewActions.click()
            )
        )
    }
}
