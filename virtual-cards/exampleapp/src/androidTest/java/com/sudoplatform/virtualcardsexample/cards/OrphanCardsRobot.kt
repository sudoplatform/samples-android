/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.cards

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.virtualcardsexample.BaseRobot
import com.sudoplatform.virtualcardsexample.R

fun orphanCards(func: OrphanCardsRobot.() -> Unit) = OrphanCardsRobot().apply { func() }

/**
 * Testing robot that manages the Orphan Cards screen.
 *
 * @since 2020-07-07
 */
class OrphanCardsRobot : BaseRobot() {

    private val orphanCardRecyclerView = withId(R.id.orphanCardRecyclerView)

    fun waitForRecyclerView() {
        waitForViewToDisplay(orphanCardRecyclerView, 5_000L)
    }

    fun navigateToCardDetailScreen(position: Int) {
        clickRecyclerViewItem(position)
        cardDetail {
            checkCardDetailItemsDisplayed()
        }
    }

    private fun clickRecyclerViewItem(position: Int) {
        onView(orphanCardRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<CardViewHolder>(
                position,
                click()
            )
        )
    }
}
