/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.contacts

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.passwordmanagerexample.BaseRobot
import com.sudoplatform.passwordmanagerexample.R

fun editContact(func: EditContactRobot.() -> Unit) = EditContactRobot().apply { func() }

private const val CREATED_AT_LENGTH = 28
private const val UPDATED_AT_LENGTH = 28

/**
 * Testing robot that manages the edit bank account screen.
 *
 * @since 2021-01-21
 */
class EditContactRobot : BaseRobot() {

    private val favoriteSwitch = withId(R.id.add_as_favorite_switch)
    private val contactNameField = withId(R.id.editText_contactName)
    private val addressField = withId(R.id.editText_address)
    private val companyField = withId(R.id.editText_company)
    private val emailField = withId(R.id.editText_email)
    private val firstNameField = withId(R.id.editText_firstName)
    private val lastNameField = withId(R.id.editText_lastName)
    private val genderField = withId(R.id.editText_gender)
    private val dobDatePicker = withId(R.id.date_picker_dateOfBirth)
    private val stateField = withId(R.id.editText_state)
    private val websiteField = withId(R.id.editText_website)
    private val phoneField = withId(R.id.editText_phone)
    private val otherPhoneField = withId(R.id.editText_otherPhone)
    private val notesField = withId(R.id.editText_notes)
    private val radioGray = withId(R.id.radio_gray)
    private val radioRed = withId(R.id.radio_red)
    private val loadingDialog = withId(R.id.progressBar)
    private val saveButton = withId(R.id.save)
    private val createdAtField = withId(R.id.label_createdAt)
    private val updatedAtField = withId(R.id.label_updatedAt)

    fun checkEditContactItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(contactNameField, timeout)
        waitForViewToDisplay(addressField, timeout)
        waitForViewToDisplay(companyField, timeout)
        waitForViewToDisplay(emailField, timeout)
    }

    fun checkDatesAreDisplayed() {
        Espresso.onView(createdAtField)
            .check(ViewAssertions.matches(withTextLength(com.sudoplatform.passwordmanagerexample.contacts.CREATED_AT_LENGTH)))
        Espresso.onView(updatedAtField)
            .check(ViewAssertions.matches(withTextLength(com.sudoplatform.passwordmanagerexample.contacts.UPDATED_AT_LENGTH)))
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

    fun enterContactName(text: String) {
        replaceText(contactNameField, text)
    }

    fun enterAddress(text: String) {
        replaceText(addressField, text)
    }

    fun enterCompany(text: String) {
        replaceText(companyField, text)
    }

    fun enterEmail(text: String) {
        replaceText(emailField, text)
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
        setDate(dobDatePicker, year, month, day)
        Espresso.closeSoftKeyboard()
    }

    fun enterState(text: String) {
        scrollToView(stateField)
        replaceText(stateField, text)
    }

    fun enterWebsite(text: String) {
        replaceText(websiteField, text)
    }

    fun enterPhone(text: String) {
        replaceText(phoneField, text)
    }

    fun enterOtherPhone(text: String) {
        replaceText(otherPhoneField, text)
    }

    fun enterNotes(text: String) {
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
