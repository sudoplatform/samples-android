/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.socialsecuritynumbers

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.passwordmanagerexample.BaseRobot
import com.sudoplatform.passwordmanagerexample.R

fun createSocialSecurityNumber(func: CreateSocialSecurityNumberRobot.() -> Unit) =
    CreateSocialSecurityNumberRobot().apply { func() }

/**
 * Testing robot that manages the create SocialSecurityNumber screen.
 *
 * @since 2021-01-21
 */
class CreateSocialSecurityNumberRobot : BaseRobot() {

    private val favoriteSwitch = withId(R.id.add_as_favorite_switch)
    private val socialSecurityNumberNameField = withId(R.id.editText_ssnName)
    private val firstNameField = withId(R.id.editText_firstName)
    private val lastNameField = withId(R.id.editText_lastName)
    private val numberField = withId(R.id.editText_number)
    private val notesField = withId(R.id.editText_notes)
    private val radioGray = withId(R.id.radio_gray)
    private val radioRed = withId(R.id.radio_red)
    private val loadingDialog = withId(R.id.progressBar)
    private val saveButton = withId(R.id.save)

    fun checkCreateSocialSecurityNumberItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(socialSecurityNumberNameField, timeout)
        waitForViewToDisplay(firstNameField, timeout)
        waitForViewToDisplay(lastNameField, timeout)
        waitForViewToDisplay(numberField, timeout)
    }

    fun clickOnSave() {
        Thread.sleep(1000)
        clickOnView(saveButton)
    }

    fun scrollToTop() {
        onView(saveButton).perform(ViewActions.scrollTo())
    }

    fun toggleFavorite() {
        clickOnView(favoriteSwitch)
    }

    fun enterSocialSecurityNumberName(text: String) {
        replaceText(socialSecurityNumberNameField, text)
    }

    fun enterFirstName(text: String) {
        replaceText(firstNameField, text)
    }

    fun enterLastName(text: String) {
        replaceText(lastNameField, text)
    }

    fun enterNumber(text: String) {
        replaceText(numberField, text)
    }

    fun enterNotes(text: String) {
        scrollToView(notesField)
        replaceText(notesField, text)
    }

    fun selectRedColor() {
        clickOnView(radioRed)
        radioRed.matches(isChecked())
        radioGray.matches(isNotChecked())
    }

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 2_500L)
        waitForViewToNotDisplay(loadingDialog, 10_000L)
    }
}
