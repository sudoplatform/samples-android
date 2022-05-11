/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.bankaccounts

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.passwordmanagerexample.BaseRobot
import com.sudoplatform.passwordmanagerexample.R

fun createBankAccount(func: CreateBankAccountRobot.() -> Unit) =
    CreateBankAccountRobot().apply { func() }

/**
 * Testing robot that manages the create Bank Account screen.
 *
 * @since 2021-01-21
 */
class CreateBankAccountRobot : BaseRobot() {

    private val favoriteSwitch = withId(R.id.add_as_favorite_switch)
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

    fun checkCreateBankAccountItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(accountNameField, timeout)
        waitForViewToDisplay(bankNameField, timeout)
        waitForViewToDisplay(accountNumberField, timeout)
        waitForViewToDisplay(routingNumberField, timeout)
        waitForViewToDisplay(accountTypeField, timeout)
        waitForViewToDisplay(notesField, timeout)
        waitForViewToDisplay(radioRed, timeout)
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
        radioRed.matches(isChecked())
        radioGray.matches(isNotChecked())
    }

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 2_500L)
        waitForViewToNotDisplay(loadingDialog, 10_000L)
    }
}
