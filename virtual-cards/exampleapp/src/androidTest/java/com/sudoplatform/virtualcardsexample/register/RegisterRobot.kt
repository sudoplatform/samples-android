/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.register

import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.virtualcardsexample.BaseRobot
import com.sudoplatform.virtualcardsexample.R

fun login(func: RegisterRobot.() -> Unit) = RegisterRobot().apply { func() }

/**
 * Testing robot that manages the register/login screen.
 *
 * @since 2020-07-07
 */
class RegisterRobot : BaseRobot() {

    private val registerButton = withId(R.id.buttonRegister)

    fun clickOnRegister() {
        clickOnView(registerButton)
    }
}
