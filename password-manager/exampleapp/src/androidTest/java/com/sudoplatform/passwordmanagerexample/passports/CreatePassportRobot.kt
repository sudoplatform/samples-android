/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.passports

import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.passwordmanagerexample.BaseRobot
import com.sudoplatform.passwordmanagerexample.R

fun createPassport(func: CreatePassportRobot.() -> Unit) =
    CreatePassportRobot().apply { func() }

/**
 * Testing robot that manages the create Passport screen.
 *
 * @since 2021-01-21
 */
class CreatePassportRobot : BaseRobot() {

    private val favoriteSwitch = withId(R.id.add_as_favorite_switch)
    private val passportNameField = withId(R.id.editText_passport_name)
    private val genderField = withId(R.id.editText_gender)
    private val issuingCountryField = withId(R.id.editText_issuingCountry)
    private val firstNameField = withId(R.id.editText_firstName)
    private val lastNameField = withId(R.id.editText_lastName)
    private val passportNumberField = withId(R.id.editText_passport_number)
    private val placeOfBirthField = withId(R.id.editText_placeOfBirth)
    private val dateOfBirthDatePicker = withId(R.id.date_picker_dateOfBirth)
    private val dateOfIssueDatePicker = withId(R.id.date_picker_dateOfIssue)
    private val expiresDatePicker = withId(R.id.date_picker_expires)
    private val notesField = withId(R.id.editText_notes)
    private val radioGray = withId(R.id.radio_gray)
    private val radioRed = withId(R.id.radio_red)
    private val loadingDialog = withId(R.id.progressBar)
    private val saveButton = withId(R.id.save)

    fun checkCreatePassportItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(passportNameField, timeout)
        waitForViewToDisplay(genderField, timeout)
        waitForViewToDisplay(issuingCountryField, timeout)
        waitForViewToDisplay(firstNameField, timeout)
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

    fun enterPassportName(text: String) {
        replaceText(passportNameField, text)
    }

    fun enterGender(text: String) {
        replaceText(genderField, text)
    }

    fun enterIssuingCountry(text: String) {
        replaceText(issuingCountryField, text)
    }

    fun enterFirstName(text: String) {
        replaceText(firstNameField, text)
    }

    fun enterLastName(text: String) {
        replaceText(lastNameField, text)
    }

    fun enterPassportNumber(text: String) {
        replaceText(passportNumberField, text)
    }

    fun enterPlaceOfBirth(text: String) {
        replaceText(placeOfBirthField, text)
    }

    fun setDateOfBirth(year: Int, month: Int, day: Int) {
        closeSoftKeyboard()
        scrollToView(dateOfBirthDatePicker)
        setDate(dateOfBirthDatePicker, year, month, day)
    }

    fun setDateOfIssue(year: Int, month: Int, day: Int) {
        closeSoftKeyboard()
        scrollToView(dateOfIssueDatePicker)
        setDate(dateOfIssueDatePicker, year, month, day)
    }

    fun setExpires(year: Int, month: Int, day: Int) {
        scrollToView(expiresDatePicker)
        setDate(expiresDatePicker, year, month, day)
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
