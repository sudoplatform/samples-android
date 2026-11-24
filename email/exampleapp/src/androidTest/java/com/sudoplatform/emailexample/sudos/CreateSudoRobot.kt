/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.sudos

import androidx.compose.ui.test.junit4.ComposeTestRule
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.emailaddresses.emailAddresses
import com.sudoplatform.emailexample.ui.TestTags

fun createSudo(
    composeRule: ComposeTestRule,
    func: CreateSudoRobot.() -> Unit,
) = CreateSudoRobot(composeRule).apply { func() }

/**
 * Testing robot that manages the Create Sudo screen.
 */
class CreateSudoRobot(
    composeRule: ComposeTestRule,
) : BaseRobot(composeRule) {
    fun checkCreateSudoItemsDisplayed() {
        waitForTag(TestTags.CREATE_SUDO_ACTION)
        waitForTag(TestTags.SUDO_LABEL_FIELD)
        waitForText(string(R.string.create_sudo_learn_more))
        waitForText(string(R.string.learn_more).uppercase())
    }

    /** Register, create a Sudo and wait for its email addresses, which the app shows next. */
    fun registerAndCreateSudoFlow() {
        sudos(composeRule) {
            navigateFromLaunchToSudos()
            navigateToCreateSudoScreen()
        }
        setSudoName("Shopping")
        clickOnCreateButton()
        emailAddresses(composeRule) {
            checkEmailAddressesItemsDisplayed()
        }
    }

    private fun setSudoName(name: String) {
        replaceText(TestTags.SUDO_LABEL_FIELD, name)
    }

    private fun clickOnCreateButton() {
        clickOnEnabledTag(TestTags.CREATE_SUDO_ACTION)
    }
}
