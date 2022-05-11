/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.passports

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.passwordmanagerexample.BaseRobot
import com.sudoplatform.passwordmanagerexample.R

fun editPassport(func: EditPassportRobot.() -> Unit) = EditPassportRobot().apply { func() }

private const val CREATED_AT_LENGTH = 28
private const val UPDATED_AT_LENGTH = 28

/**
 * Testing robot that manages the edit bank account screen.
 *
 * @since 2021-01-21
 */
class EditPassportRobot : BaseRobot() {

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
    private val createdAtField = withId(R.id.label_createdAt)
    private val updatedAtField = withId(R.id.label_updatedAt)

    fun checkEditPassportItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(passportNameField, timeout)
        waitForViewToDisplay(genderField, timeout)
        waitForViewToDisplay(issuingCountryField, timeout)
        waitForViewToDisplay(firstNameField, timeout)
    }

    fun checkDatesAreDisplayed() {
        Espresso.onView(createdAtField)
            .check(ViewAssertions.matches(withTextLength(com.sudoplatform.passwordmanagerexample.passports.CREATED_AT_LENGTH)))
        Espresso.onView(updatedAtField)
            .check(ViewAssertions.matches(withTextLength(com.sudoplatform.passwordmanagerexample.passports.UPDATED_AT_LENGTH)))
    }

    fun clickOnSave() {
        Thread.sleep(1000)
        clickOnView(saveButton)
    }

    fun scrollToTop() {
        Espresso.onView(saveButton).perform(ViewActions.scrollTo())
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
        Espresso.closeSoftKeyboard()
        scrollToView(dateOfBirthDatePicker)
        setDate(dateOfBirthDatePicker, year, month, day)
    }

    fun setDateOfIssue(year: Int, month: Int, day: Int) {
        Espresso.closeSoftKeyboard()
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
        radioRed.matches(ViewMatchers.isChecked())
        radioGray.matches(ViewMatchers.isNotChecked())
    }

    fun selectYellowColor() {
        clickOnView(withId(R.id.radio_yellow))
    }

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 2_500L)
        waitForViewToNotDisplay(loadingDialog, 10_000L)
    }
}
