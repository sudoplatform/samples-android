/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.creditcards

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.passwordmanagerexample.BaseRobot
import com.sudoplatform.passwordmanagerexample.R

fun editCreditCard(func: EditCreditCardRobot.() -> Unit) = EditCreditCardRobot().apply { func() }

private const val CREATED_AT_LENGTH = 28
private const val UPDATED_AT_LENGTH = 28

/**
 * Testing robot that manages the edit credit card screen.
 *
 * @since 2021-01-19
 */
class EditCreditCardRobot : BaseRobot() {

    private val createdAtField = withId(R.id.label_createdAt)
    private val updatedAtField = withId(R.id.label_updatedAt)
    private val cardNameField = withId(R.id.editText_cardName)
    private val cardHolderField = withId(R.id.editText_cardHolder)
    private val cardTypeField = withId(R.id.editText_cardType)
    private val cardNumberField = withId(R.id.editText_cardNumber)
    private val expiryField = withId(R.id.editText_expiry)
    private val securityCodeField = withId(R.id.editText_securityCode)
    private val notesField = withId(R.id.editText_notes)
    private val saveButton = withId(R.id.save)
    private val loadingDialog = withId(R.id.progressBar)

    fun checkEditCreditCardItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(createdAtField, timeout)
        waitForViewToDisplay(updatedAtField, timeout)
        waitForViewToDisplay(cardNameField, timeout)
        waitForViewToDisplay(cardHolderField, timeout)
        waitForViewToDisplay(cardTypeField, timeout)
        waitForViewToDisplay(cardNumberField, timeout)
        waitForViewToDisplay(expiryField, timeout)
        waitForViewToDisplay(securityCodeField, timeout)
        waitForViewToDisplay(notesField, timeout)
    }

    fun checkDatesAreDisplayed() {
        Espresso.onView(createdAtField)
            .check(ViewAssertions.matches(withTextLength(com.sudoplatform.passwordmanagerexample.creditcards.CREATED_AT_LENGTH)))
        Espresso.onView(updatedAtField)
            .check(ViewAssertions.matches(withTextLength(com.sudoplatform.passwordmanagerexample.creditcards.UPDATED_AT_LENGTH)))
    }

    fun clickOnSave() {
        clickOnView(saveButton)
    }

    fun scrollToTop() {
        Espresso.onView(saveButton).perform(ViewActions.scrollTo())
    }

    fun enterCardName(text: String) {
        replaceText(cardNameField, text)
    }

    fun enterCardHolder(text: String) {
        replaceText(cardHolderField, text)
    }

    fun enterCardType(text: String) {
        replaceText(cardTypeField, text)
    }

    fun enterCardNumber(text: String) {
        replaceText(cardNumberField, text)
    }

    fun enterCardExpiration(text: String) {
        replaceText(expiryField, text)
    }

    fun enterSecurityCode(text: String) {
        replaceText(securityCodeField, text)
    }

    fun enterNotes(text: String) {
        replaceText(notesField, text)
    }

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 2_500L)
        waitForViewToNotDisplay(loadingDialog, 2_500L)
    }
}
