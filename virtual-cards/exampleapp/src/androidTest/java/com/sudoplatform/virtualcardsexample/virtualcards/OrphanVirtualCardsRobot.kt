/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.virtualcards

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.virtualcardsexample.BaseRobot
import com.sudoplatform.virtualcardsexample.R

fun orphanVirtualCards(func: OrphanVirtualCardsRobot.() -> Unit) = OrphanVirtualCardsRobot().apply { func() }

/**
 * Testing robot that manages the Orphan Virtual Cards screen.
 */
class OrphanVirtualCardsRobot : BaseRobot() {

    private val orphanCardRecyclerView = withId(R.id.orphanVirtualCardRecyclerView)

    fun waitForRecyclerView() {
        waitForViewToDisplay(orphanCardRecyclerView, 5_000L)
    }

    fun navigateToVirtualCardDetailScreen(position: Int) {
        clickRecyclerViewItem(position)
        virtualCardDetail {
            checkVirtualCardDetailItemsDisplayed()
        }
    }

    private fun clickRecyclerViewItem(position: Int) {
        onView(orphanCardRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<VirtualCardViewHolder>(
                position,
                click(),
            ),
        )
    }
}
