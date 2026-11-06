/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.register

import androidx.compose.ui.test.junit4.ComposeTestRule
import com.sudoplatform.telephonyexample.BaseRobot
import com.sudoplatform.telephonyexample.ui.TestTags

fun register(
    composeRule: ComposeTestRule,
    func: RegisterRobot.() -> Unit,
) = RegisterRobot(composeRule).apply { func() }

/**
 * Testing robot that manages the register/login screen.
 */
class RegisterRobot(
    composeRule: ComposeTestRule,
) : BaseRobot(composeRule) {
    fun waitForRegisterScreen() {
        waitForTag(TestTags.REGISTER_BUTTON)
    }

    fun clickOnRegister() {
        clickOnTag(TestTags.REGISTER_BUTTON)
    }
}
