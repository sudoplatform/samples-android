/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.virtualcards

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.sudoplatform.virtualcardsexample.BaseRobot
import com.sudoplatform.virtualcardsexample.R

fun createVirtualCard(func: CreateVirtualCardRobot.() -> Unit) = CreateVirtualCardRobot().apply { func() }

/**
 * Testing robot that manages the Create Virtual Card screen.
 */
class CreateVirtualCardRobot : BaseRobot() {

    private val inputForm = R.id.formRecyclerView
    private val toolbarCreateButton = withId(R.id.create)
    private val sudoTextView = withId(R.id.sudoText)
    private val fundingSourceTextView = withId(R.id.fundingSourceText)
    private val learnMoreTextView = withId(R.id.learnMoreTextView)
    private val learnMoreButton = withId(R.id.learnMoreButton)
    private val loadingDialog = withId(R.id.progressBar)
    private val positiveAlertButton = withId(android.R.id.button1)

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 5_000L, true)
        waitForViewToNotDisplay(loadingDialog, 30_000L)
    }

    fun checkCreateVirtualCardItemsDisplayed() {
        waitForViewToDisplay(withId(inputForm))
        waitForViewToDisplay(toolbarCreateButton)
        scrollToView(sudoTextView)
        waitForViewToDisplay(sudoTextView)
        scrollToView(fundingSourceTextView)
        waitForViewToDisplay(fundingSourceTextView)
        scrollToView(learnMoreButton)
        waitForViewToDisplay(learnMoreTextView)
        waitForViewToDisplay(learnMoreButton)
    }

    fun clickCreateButton() {
        Thread.sleep(1000)
        clickOnView(toolbarCreateButton)
    }

    fun clickPositiveAlertDialogButton() {
        checkAlertDialog()
        Thread.sleep(1000)
        clickOnView(positiveAlertButton)
    }

    private fun checkAlertDialog() {
        waitForViewToDisplay(positiveAlertButton, 15_000L)
        onView(positiveAlertButton)
            .check(matches(withText(android.R.string.ok)))
    }
}
