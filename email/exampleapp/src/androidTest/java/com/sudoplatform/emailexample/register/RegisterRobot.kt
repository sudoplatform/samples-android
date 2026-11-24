/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.register

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.ui.TestTags

fun login(
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

    fun clickOnEnforceSingletonKeyPair() {
        clickOnTag(TestTags.ENFORCE_SINGLETON_KEY_PAIR_TOGGLE)
    }

    fun assertEnforceSingletonKeyPair(isOn: Boolean) {
        waitForTag(TestTags.ENFORCE_SINGLETON_KEY_PAIR_TOGGLE)
        val toggle = composeRule.onNodeWithTag(TestTags.ENFORCE_SINGLETON_KEY_PAIR_TOGGLE)
        if (isOn) toggle.assertIsOn() else toggle.assertIsOff()
    }
}
