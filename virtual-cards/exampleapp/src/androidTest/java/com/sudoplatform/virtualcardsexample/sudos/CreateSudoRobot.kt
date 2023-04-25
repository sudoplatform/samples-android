/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.sudos

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.sudoplatform.virtualcardsexample.BaseRobot
import com.sudoplatform.virtualcardsexample.R

fun createSudo(func: CreateSudoRobot.() -> Unit) = CreateSudoRobot().apply { func() }

/**
 * Testing robot that manages the Create Sudo screen.
 */
class CreateSudoRobot : BaseRobot() {

    private val toolbarCreateButton = withId(R.id.create)
    private val sudoLabelEditText = withId(R.id.editText)
    private val detailTextView = withId(R.id.textView)
    private val learnMoreButton = withId(R.id.learnMoreButton)
    private val learnMoreTextView = withId(R.id.learnMoreTextView)
    private val loadingDialog = withId(R.id.progressBar)
    private val positiveAlertButton = withId(android.R.id.button1)

    fun checkCreateSudoItemsDisplayed() {
        waitForViewToDisplay(toolbarCreateButton)
        waitForViewToDisplay(sudoLabelEditText)
        waitForViewToDisplay(detailTextView)
        waitForViewToDisplay(learnMoreTextView)
        waitForViewToDisplay(learnMoreButton)
    }

    fun clickOnCreateButton() {
        clickOnView(toolbarCreateButton)
    }

    fun clickPositiveAlertDialogButton() {
        checkAlertDialog()
        Thread.sleep(1000)
        clickOnView(positiveAlertButton)
    }

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 5_000L, true)
        waitForViewToNotDisplay(loadingDialog, 60_000L)
    }

    fun setSudoName(name: String) {
        setTextFieldValue(name)
    }

    private fun setTextFieldValue(inputText: String) {
        replaceText(sudoLabelEditText, inputText)
    }

    private fun checkAlertDialog() {
        waitForViewToDisplay(positiveAlertButton, 60_000L)
        onView(positiveAlertButton)
            .check(matches(withText(android.R.string.ok)))
    }
}
