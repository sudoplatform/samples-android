/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.register

import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import com.sudoplatform.direlayexample.BaseRobot
import com.sudoplatform.direlayexample.R
import org.hamcrest.CoreMatchers.anything
import org.hamcrest.CoreMatchers.containsString

fun login(func: RegisterRobot.() -> Unit) = RegisterRobot().apply { func() }

/**
 * Testing robot that manages the start screen.
 *
 * @since 2021-06-29
 */
class RegisterRobot : BaseRobot() {

    private val registerButton = withId(R.id.buttonRegister)
    private val resetButton = withId(R.id.buttonSignOut)
    private val registrationSpinner = withId(R.id.registrationMethodSpinner)

    fun clickOnRegister() {
        checkRegisterItemsDisplayed()
        clickOnView(registerButton)
    }

    fun checkRegistrationSpinner() {
        clickOnView(registrationSpinner)
        onData(anything()).atPosition(0).perform(click())
        onView(registrationSpinner).check(matches(withSpinnerText(containsString("TEST Registration"))))
    }

    fun checkRegisterItemsDisplayed(timeout: Long = 1_000L) {
        waitForViewToDisplay(registerButton, timeout)
        waitForViewToDisplay(registrationSpinner, timeout)
        waitForViewToDisplay(resetButton, timeout)
    }
}
