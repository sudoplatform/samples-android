/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.sudos

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.mainmenu.mainMenu

fun createSudo(func: CreateSudoRobot.() -> Unit) = CreateSudoRobot().apply { func() }

/**
 * Testing robot that manages the Create Sudo screen.
 *
 * @since 2020-08-06
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
        clickOnView(positiveAlertButton)
    }

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 5_000L)
        waitForViewToNotDisplay(loadingDialog, 10_000L)
    }

    fun setSudoName(name: String) {
        setTextFieldValue(name)
    }

    fun registerAndCreateSudoFlow() {
        mainMenu {
            navigateFromLaunchToMainMenu()
            navigateToSudosScreen()
        }
        sudos {
            navigateToCreateSudoScreen()
        }
        createSudo {
            setSudoName("Shopping")
            clickOnCreateButton()
            waitForLoading()
            clickPositiveAlertDialogButton()
        }
    }

    private fun setTextFieldValue(inputText: String) {
        replaceText(sudoLabelEditText, inputText)
    }

    private fun checkAlertDialog() {
        waitForViewToDisplay(positiveAlertButton, 15_000L)
        Espresso.onView(positiveAlertButton)
            .check(ViewAssertions.matches(ViewMatchers.withText(android.R.string.ok)))
    }
}
