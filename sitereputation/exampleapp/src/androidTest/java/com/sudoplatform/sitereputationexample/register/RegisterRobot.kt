/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sitereputationexample.register

import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.sitereputationexample.BaseRobot
import com.sudoplatform.sitereputationexample.R

fun login(func: RegisterRobot.() -> Unit) = RegisterRobot().apply { func() }

/**
 * Testing robot that manages the register/login screen.
 *
 * @since 2020-12-04
 */
class RegisterRobot : BaseRobot() {

    private val registerButton = withId(R.id.buttonRegister)

    fun waitForRegisterButton() {
        waitForViewToDisplay(registerButton, 5_000L)
    }

    fun clickOnRegister() {
        clickOnView(registerButton)
    }
}
