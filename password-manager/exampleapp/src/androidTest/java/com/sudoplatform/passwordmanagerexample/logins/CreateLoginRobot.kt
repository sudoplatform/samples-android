/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.logins

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.passwordmanagerexample.BaseRobot
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.passwordgenerator.DEFAULT_PASSWORD_LENGTH

fun createLogin(func: CreateLoginRobot.() -> Unit) = CreateLoginRobot().apply { func() }

/**
 * Testing robot that manages the create login screen.
 *
 * @since 2020-10-28
 */
class CreateLoginRobot : BaseRobot() {

    private val favoriteSwitch = withId(R.id.add_as_favorite_switch)
    private val loginNameField = withId(R.id.editText_loginName)
    private val webAddressField = withId(R.id.editText_webAddress)
    private val usernameField = withId(R.id.editText_username)
    private val passwordField = withId(R.id.editText_password)
    private val notesField = withId(R.id.editText_notes)
    private val generatePasswordButton = withId(R.id.button_passwordGenerator)
    private val saveButton = withId(R.id.save)
    private val loadingDialog = withId(R.id.progressBar)
    private val radioRed = withId(R.id.radio_red)
    private val radioGray = withId(R.id.radio_gray)

    fun checkCreateLoginItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(loginNameField, timeout)
        waitForViewToDisplay(webAddressField, timeout)
        waitForViewToDisplay(usernameField, timeout)
        waitForViewToDisplay(passwordField, timeout)
        waitForViewToDisplay(notesField, timeout)
        waitForViewToDisplay(generatePasswordButton, timeout)
    }

    fun checkPasswordIsNotBlank() {
        onView(passwordField)
            .check(matches(withTextLength(DEFAULT_PASSWORD_LENGTH)))
    }

    fun clickOnSave() {
        Thread.sleep(1000)
        clickOnView(saveButton)
    }

    fun toggleFavorite() {
        clickOnView(favoriteSwitch)
    }

    fun clickOnGeneratePassword() {
        Thread.sleep(1000)
        clickOnView(generatePasswordButton)
    }

    fun enterLoginName(text: String) {
        replaceText(loginNameField, text)
    }

    fun enterWebAddress(text: String) {
        replaceText(webAddressField, text)
    }

    fun enterUsername(text: String) {
        replaceText(usernameField, text)
    }

    fun enterPassword(text: String) {
        replaceText(passwordField, text)
    }

    fun enterNotes(text: String) {
        replaceText(notesField, text)
    }

    fun selectRedColor() {
        scrollToView(radioRed)
        clickOnView(radioRed)
        radioRed.matches(ViewMatchers.isChecked())
        radioGray.matches(ViewMatchers.isNotChecked())
    }

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 2_500L)
        waitForViewToNotDisplay(loadingDialog, 10_000L)
    }
}
