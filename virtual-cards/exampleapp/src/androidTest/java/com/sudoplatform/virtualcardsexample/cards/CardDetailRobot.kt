/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.cards

import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.virtualcardsexample.BaseRobot
import com.sudoplatform.virtualcardsexample.R

fun cardDetail(func: CardDetailRobot.() -> Unit) = CardDetailRobot().apply { func() }

/**
 * Testing robot that manages the Card Detail screen.
 *
 * @since 2020-07-29
 */
class CardDetailRobot : BaseRobot() {

    private val cardView = withId(R.id.cardView)
    private val transactionTextView = withId(R.id.transactionTitle)
    private val transactionRecyclerView = withId(R.id.transactionRecyclerView)

    fun waitForRecyclerView() {
        waitForViewToDisplay(transactionRecyclerView, 15_000L)
    }

    fun checkCardDetailItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(cardView, timeout)
        waitForViewToDisplay(transactionTextView, timeout)
    }
}
