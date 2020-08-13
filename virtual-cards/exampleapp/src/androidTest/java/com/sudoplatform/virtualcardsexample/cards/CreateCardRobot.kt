/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.cards

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.sudoplatform.virtualcardsexample.BaseRobot
import com.sudoplatform.virtualcardsexample.R

fun createCard(func: CreateCardRobot.() -> Unit) = CreateCardRobot().apply { func() }

/**
 * Testing robot that manages the Create Card screen.
 *
 * @since 2020-07-29
 */
class CreateCardRobot : BaseRobot() {

    private val inputForm = R.id.formRecyclerView
    private val toolbarCreateButton = withId(R.id.create)
    private val sudoTextView = withId(R.id.sudoText)
    private val fundingSourceTextView = withId(R.id.fundingSourceText)
    private val learnMoreTextView = withId(R.id.learnMoreTextView)
    private val learnMoreButton = withId(R.id.learnMoreButton)
    private val loadingDialog = withId(R.id.progressBar)
    private val positiveAlertButton = withId(android.R.id.button1)

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 5_000L)
        waitForViewToNotDisplay(loadingDialog, 10_000L)
    }

    fun checkCreateCardItemsDisplayed() {
        waitForViewToDisplay(withId(inputForm))
        waitForViewToDisplay(toolbarCreateButton)
        waitForViewToDisplay(sudoTextView)
        waitForViewToDisplay(fundingSourceTextView)
        waitForViewToDisplay(learnMoreTextView)
        waitForViewToDisplay(learnMoreButton)
    }

    fun clickCreateButton() {
        clickOnView(toolbarCreateButton)
    }

    fun clickPositiveAlertDialogButton() {
        checkAlertDialog()
        clickOnView(positiveAlertButton)
    }

    private fun checkAlertDialog() {
        waitForViewToDisplay(positiveAlertButton, 15_000L)
        onView(positiveAlertButton)
            .check(matches(withText(android.R.string.ok)))
    }
}
