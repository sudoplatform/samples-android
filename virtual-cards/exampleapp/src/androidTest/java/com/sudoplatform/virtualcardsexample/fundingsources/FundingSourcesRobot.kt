/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.fundingsources

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.virtualcardsexample.BaseRobot
import com.sudoplatform.virtualcardsexample.R

fun fundingSources(func: FundingSourcesRobot.() -> Unit) = FundingSourcesRobot().apply { func() }

/**
 * Testing robot that manages the Funding Sources screen.
 */
class FundingSourcesRobot : BaseRobot() {
    private val createFundingSourceButton = withId(R.id.createFundingSourceButton)
    private val fundingSourceRecyclerView = withId(R.id.fundingSourceRecyclerView)
    private val loadingProgress = withId(R.id.progressBar)
    private val positiveAlertButton = withId(android.R.id.button1)

    fun waitForLoading() {
        waitForViewToDisplay(loadingProgress, 5_000L, true)
        waitForViewToNotDisplay(loadingProgress, 10_000L)
    }

    fun checkFundingSourcesItemsDisplayed() {
        waitForViewToDisplay(createFundingSourceButton)
    }

    fun waitForRecyclerView() {
        waitForViewToDisplay(fundingSourceRecyclerView, 5_000L)
    }

    fun navigateToCreateFundingSourcesScreen() {
        clickOnView(createFundingSourceButton)
        createFundingSource {
            checkCreateFundingSourceItemsDisplayed()
        }
    }

    fun clickOnPositiveAlertDialogButton() {
        waitForViewToDisplay(positiveAlertButton, 15_000L)
        Thread.sleep(1000)
        clickOnView(positiveAlertButton)
    }

    fun swipeLeftToCancel(position: Int) {
        onView(fundingSourceRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<FundingSourceViewHolder>(
                position,
                ViewActions.swipeLeft()
            )
        )
    }
}
