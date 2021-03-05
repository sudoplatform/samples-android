/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.passwordgenerator

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.sudoplatform.passwordmanagerexample.BaseRobot
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.settings.settings
import com.sudoplatform.passwordmanagerexample.sudos.sudos
import org.hamcrest.Matchers.not

fun passwordGenerator(func: PasswordGeneratorRobot.() -> Unit) = PasswordGeneratorRobot().apply { func() }

/**
 * Testing robot that manages the password generator dialogue.
 *
 * @since 2020-11-06
 */
class PasswordGeneratorRobot : BaseRobot() {

    private val passwordField = withId(R.id.editText_generated_password)
    private val seekBar = withId(R.id.seekBar)
    private val lengthField = withId(R.id.editText_length)
    private val uppercaseSwitch = withId(R.id.switch_uppercase)
    private val lowercaseSwitch = withId(R.id.switch_lowercase)
    private val numbersSwitch = withId(R.id.switch_numbers)
    private val symbolsSwitch = withId(R.id.switch_symbols)
    private val okButton = withId(R.id.button_ok)
    private val cancelButton = withId(R.id.button_cancel)
    private val testPassword = "testPassword"

    fun checkPasswordGeneratorItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(passwordField, timeout)
        waitForViewToDisplay(seekBar, timeout)
        waitForViewToDisplay(lengthField, timeout)
        waitForViewToDisplay(uppercaseSwitch, timeout)
        waitForViewToDisplay(lowercaseSwitch, timeout)
        waitForViewToDisplay(numbersSwitch, timeout)
        waitForViewToDisplay(symbolsSwitch, timeout)
    }

    fun navigateFromLaunchToPasswordGenerator() {
        sudos {
            navigateFromLaunchToSudos()
            waitForLoading(true)
            clickOnSettings()
        }
        settings {
            clickOnPasswordGenerator()
        }
    }

    fun setTestPassword() {
        replaceText(passwordField, testPassword)
    }

    fun toggleLowercase() {
        Thread.sleep(1000)
        clickOnView(lowercaseSwitch)
    }

    fun toggleUppercase() {
        Thread.sleep(1000)
        clickOnView(uppercaseSwitch)
    }

    fun toggleNumbers() {
        Thread.sleep(1000)
        clickOnView(numbersSwitch)
    }

    fun toggleSymbols() {
        Thread.sleep(1000)
        clickOnView(symbolsSwitch)
    }

    fun checkPasswordMatchesTest() {
        onView(passwordField).check(matches(withText(testPassword)))
    }

    fun checkPasswordDoesNotMatchTest() {
        onView(passwordField).check(matches(not(withText(testPassword))))
    }

    fun checkPasswordLength(length: Int) {
        onView(passwordField).check(matches(withTextLength(length)))
    }

    fun setPasswordLengthField(text: String) {
        replaceText(lengthField, text)
    }

    fun clickOnOkButton() {
        Thread.sleep(1000)
        clickOnView(okButton)
    }

    fun clickOnCancelButton() {
        Thread.sleep(1000)
        clickOnView(cancelButton)
    }
}
