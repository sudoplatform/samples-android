/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.register

import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.R

fun login(func: RegisterRobot.() -> Unit) = RegisterRobot().apply { func() }

/**
 * Testing robot that manages the register/login screen.
 */
class RegisterRobot : BaseRobot() {

    private val registerButton = withId(R.id.buttonRegister)

    fun clickOnRegister() {
        waitForViewToDisplay(registerButton)
        clickOnView(registerButton)
    }

    // Locates the Notification permission dialogs and selects "Allow".
    fun clickNotificationPermissionDialog() {
        val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val allowPermissions = uiDevice.findObject(UiSelector().text("Allow"))
        if (allowPermissions.exists()) {
            allowPermissions.click()
        }
    }
}
