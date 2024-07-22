/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.sudos

import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.mainmenu.mainMenu

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

    fun checkCreateSudoItemsDisplayed() {
        waitForViewToDisplay(toolbarCreateButton)
        waitForViewToDisplay(sudoLabelEditText)
        waitForViewToDisplay(detailTextView)
        waitForViewToDisplay(learnMoreTextView)
        waitForViewToDisplay(learnMoreButton)
    }

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 5_000L)
        waitForViewToNotDisplay(loadingDialog, 60_000L)
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
        }
    }

    private fun setSudoName(name: String) {
        setTextFieldValue(name)
    }

    private fun clickOnCreateButton() {
        clickOnView(toolbarCreateButton)
    }

    private fun setTextFieldValue(inputText: String) {
        replaceText(sudoLabelEditText, inputText)
    }
}
