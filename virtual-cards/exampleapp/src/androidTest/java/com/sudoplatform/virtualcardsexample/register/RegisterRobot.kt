/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.register

import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.sudoplatform.virtualcardsexample.BaseRobot
import com.sudoplatform.virtualcardsexample.R

fun login(func: RegisterRobot.() -> Unit) = RegisterRobot().apply { func() }

/**
 * Testing robot that manages the register/login screen.
 */
class RegisterRobot : BaseRobot() {
    private val registerButton = withId(R.id.buttonRegister)

    fun clickOnRegister() {
        allowNotificationPermissionIfDialogPresent()
        waitForViewToDisplay(registerButton)
        clickOnView(registerButton)
    }

    /**
     * Clicks the system notification permission dialog's "Allow" button if it appears.
     */
    fun allowNotificationPermissionIfDialogPresent() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val allowButton = device.findObject(UiSelector().textMatches("(?i)allow"))
        if (allowButton.exists() && allowButton.isEnabled) {
            allowButton.click()
        }
    }
}
