/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.bankaccounts

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.passwordmanagerexample.BaseRobot
import com.sudoplatform.passwordmanagerexample.R

fun editBankAccount(func: EditBankAccountRobot.() -> Unit) = EditBankAccountRobot().apply { func() }

private const val CREATED_AT_LENGTH = 28
private const val UPDATED_AT_LENGTH = 28

/**
 * Testing robot that manages the edit bank account screen.
 *
 * @since 2021-01-21
 */
class EditBankAccountRobot : BaseRobot() {

    private val favoriteSwitch = withId(R.id.switch_favorite)
    private val createdAtField = withId(R.id.label_createdAt)
    private val updatedAtField = withId(R.id.label_updatedAt)
    private val accountNameField = withId(R.id.editText_accountName)
    private val bankNameField = withId(R.id.editText_bankName)
    private val accountNumberField = withId(R.id.editText_accountNumber)
    private val routingNumberField = withId(R.id.editText_routingNumber)
    private val accountTypeField = withId(R.id.editText_accountType)
    private val notesField = withId(R.id.editText_notes)
    private val radioRed = withId(R.id.radio_red)
    private val radioGray = withId(R.id.radio_gray)
    private val saveButton = withId(R.id.save)
    private val loadingDialog = withId(R.id.progressBar)

    fun checkEditBankAccountItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(accountNameField, timeout)
        waitForViewToDisplay(bankNameField, timeout)
        waitForViewToDisplay(accountNumberField, timeout)
        waitForViewToDisplay(routingNumberField, timeout)
        waitForViewToDisplay(accountTypeField, timeout)
        scrollToView(updatedAtField)
        waitForViewToDisplay(notesField, timeout)
        waitForViewToDisplay(createdAtField, timeout)
        waitForViewToDisplay(updatedAtField, timeout)
    }

    fun checkDatesAreDisplayed() {
        Espresso.onView(createdAtField)
            .check(ViewAssertions.matches(withTextLength(com.sudoplatform.passwordmanagerexample.bankaccounts.CREATED_AT_LENGTH)))
        Espresso.onView(updatedAtField)
            .check(ViewAssertions.matches(withTextLength(com.sudoplatform.passwordmanagerexample.bankaccounts.UPDATED_AT_LENGTH)))
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

    fun enterAccountName(text: String) {
        replaceText(accountNameField, text)
    }

    fun enterBankName(text: String) {
        replaceText(bankNameField, text)
    }

    fun enterAccountNumber(text: String) {
        replaceText(accountNumberField, text)
    }

    fun enterRoutingNumber(text: String) {
        replaceText(routingNumberField, text)
    }

    fun enterAccountType(text: String) {
        replaceText(accountTypeField, text)
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
