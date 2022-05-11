/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.driverslicenses

import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.passwordmanagerexample.BaseRobot
import com.sudoplatform.passwordmanagerexample.R

fun createDriversLicense(func: CreateDriversLicenseRobot.() -> Unit) =
    CreateDriversLicenseRobot().apply { func() }

/**
 * Testing robot that manages the create DriversLicense screen.
 *
 * @since 2021-01-21
 */
class CreateDriversLicenseRobot : BaseRobot() {

    private val favoriteSwitch = withId(R.id.add_as_favorite_switch)
    private val driversLicenseNameField = withId(R.id.editText_driversLicenseName)
    private val countryField = withId(R.id.editText_country)
    private val numberField = withId(R.id.editText_number)
    private val firstNameField = withId(R.id.editText_firstName)
    private val lastNameField = withId(R.id.editText_lastName)
    private val genderField = withId(R.id.editText_gender)
    private val dobDatePicker = withId(R.id.date_picker_dateOfBirth)
    private val doiDatePicker = withId(R.id.date_picker_dateOfIssue)
    private val expiresDatePicker = withId(R.id.date_picker_expires)
    private val stateField = withId(R.id.editText_state)
    private val notesField = withId(R.id.editText_notes)
    private val radioGray = withId(R.id.radio_gray)
    private val radioRed = withId(R.id.radio_red)
    private val loadingDialog = withId(R.id.progressBar)
    private val saveButton = withId(R.id.save)

    fun checkCreateDriversLicenseItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(driversLicenseNameField, timeout)
        waitForViewToDisplay(countryField, timeout)
        waitForViewToDisplay(numberField, timeout)
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

    fun enterDriversLicenseName(text: String) {
        replaceText(driversLicenseNameField, text)
    }

    fun enterCountry(text: String) {
        replaceText(countryField, text)
    }

    fun enterNumber(text: String) {
        replaceText(numberField, text)
    }

    fun enterFirstName(text: String) {
        replaceText(firstNameField, text)
    }

    fun enterLastName(text: String) {
        replaceText(lastNameField, text)
    }

    fun enterGender(text: String) {
        replaceText(genderField, text)
    }

    fun setDateOfBirth(year: Int, month: Int, day: Int) {
        closeSoftKeyboard()
        setDate(dobDatePicker, year, month, day)
    }

    fun setDateOfIssue(year: Int, month: Int, day: Int) {
        closeSoftKeyboard()
        scrollToView(doiDatePicker)
        setDate(doiDatePicker, year, month, day)
    }

    fun setExpires(year: Int, month: Int, day: Int) {
        closeSoftKeyboard()
        scrollToView(expiresDatePicker)
        setDate(expiresDatePicker, year, month, day)
    }

    fun enterState(text: String) {
        scrollToView(stateField)
        replaceText(stateField, text)
    }

    fun enterNotes(text: String) {
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
