/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.virtualcards

import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.virtualcardsexample.BaseRobot
import com.sudoplatform.virtualcardsexample.R

fun virtualCardDetail(func: VirtualCardDetailRobot.() -> Unit) = VirtualCardDetailRobot().apply { func() }

/**
 * Testing robot that manages the Virtual Card Detail screen.
 */
class VirtualCardDetailRobot : BaseRobot() {

    private val cardView = withId(R.id.cardView)
    private val transactionTextView = withId(R.id.transactionTitle)
    private val transactionRecyclerView = withId(R.id.transactionRecyclerView)

    fun waitForRecyclerView() {
        waitForViewToDisplay(transactionRecyclerView, 15_000L)
    }

    fun checkVirtualCardDetailItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(cardView, timeout)
        waitForViewToDisplay(transactionTextView, timeout)
    }
}
