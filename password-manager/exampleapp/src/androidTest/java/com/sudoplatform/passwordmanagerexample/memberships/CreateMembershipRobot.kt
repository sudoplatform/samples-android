/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.memberships

import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.passwordmanagerexample.BaseRobot
import com.sudoplatform.passwordmanagerexample.R

fun createMembership(func: CreateMembershipRobot.() -> Unit) =
    CreateMembershipRobot().apply { func() }

/**
 * Testing robot that manages the create Membership screen.
 *
 * @since 2021-01-21
 */
class CreateMembershipRobot : BaseRobot() {

    private val favoriteSwitch = withId(R.id.add_as_favorite_switch)
    private val membershipNameField = withId(R.id.editText_membership_name)
    private val addressField = withId(R.id.editText_address)
    private val emailField = withId(R.id.editText_email)
    private val firstNameField = withId(R.id.editText_firstName)
    private val lastNameField = withId(R.id.editText_lastName)
    private val memberIdField = withId(R.id.editText_member_id)
    private val memberSinceDatePicker = withId(R.id.date_picker_member_since)
    private val expiresDatePicker = withId(R.id.date_picker_expires)
    private val phoneField = withId(R.id.editText_phone)
    private val passwordField = withId(R.id.editText_password)
    private val websiteField = withId(R.id.editText_website)
    private val notesField = withId(R.id.editText_notes)
    private val radioGray = withId(R.id.radio_gray)
    private val radioRed = withId(R.id.radio_red)
    private val loadingDialog = withId(R.id.progressBar)
    private val saveButton = withId(R.id.save)

    fun checkCreateMembershipItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(membershipNameField, timeout)
        waitForViewToDisplay(addressField, timeout)
        waitForViewToDisplay(emailField, timeout)
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

    fun enterMembershipName(text: String) {
        replaceText(membershipNameField, text)
    }

    fun enterAddress(text: String) {
        replaceText(addressField, text)
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

    fun enterMemberId(text: String) {
        replaceText(memberIdField, text)
    }

    fun setMemberSince(year: Int, month: Int, day: Int) {
        closeSoftKeyboard()
        setDate(memberSinceDatePicker, year, month, day)
    }

    fun setExpires(year: Int, month: Int, day: Int) {
        scrollToView(expiresDatePicker)
        setDate(expiresDatePicker, year, month, day)
    }

    fun enterPhone(text: String) {
        scrollToView(phoneField)
        replaceText(phoneField, text)
    }

    fun enterPassword(text: String) {
        replaceText(passwordField, text)
    }

    fun enterWebsite(text: String) {
        replaceText(websiteField, text)
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
