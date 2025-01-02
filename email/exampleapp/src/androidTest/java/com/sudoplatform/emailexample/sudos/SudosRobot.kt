/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.sudos

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.R

fun sudos(func: SudosRobot.() -> Unit) = SudosRobot().apply { func() }

/**
 * Testing robot that manages the Sudos screen.
 */
class SudosRobot : BaseRobot() {

    private val toolbar = withId(R.id.toolbar)
    private val createSudoButton = withId(R.id.createSudoButton)
    private val sudoRecyclerView = withId(R.id.sudoRecyclerView)
    private val loadingDialog = withId(R.id.progressBar)

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 5_000L)
        waitForViewToNotDisplay(loadingDialog, 60_000L)
    }

    fun waitForRecyclerView() {
        waitForViewToDisplay(sudoRecyclerView, 5_000L)
    }

    fun checkSudosItemsDisplayed(timeout: Long = 1_000L) {
        waitForViewToDisplay(toolbar, timeout)
        waitForViewToDisplay(createSudoButton)
    }

    fun navigateToCreateSudoScreen() {
        clickOnView(createSudoButton)
        createSudo {
            checkCreateSudoItemsDisplayed()
        }
    }

    fun swipeLeftToDelete(position: Int) {
        onView(sudoRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<SudoViewHolder>(
                position,
                ViewActions.swipeLeft(),
            ),
        )
    }
}
