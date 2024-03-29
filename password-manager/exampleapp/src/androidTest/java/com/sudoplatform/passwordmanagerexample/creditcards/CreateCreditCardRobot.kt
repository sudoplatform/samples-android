/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.creditcards

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.passwordmanagerexample.BaseRobot
import com.sudoplatform.passwordmanagerexample.R

fun createCreditCard(func: CreateCreditCardRobot.() -> Unit) = CreateCreditCardRobot().apply { func() }

/**
 * Testing robot that manages the create credit card screen.
 *
 * @since 2021-01-19
 */
class CreateCreditCardRobot : BaseRobot() {

    private val favoriteSwitch = withId(R.id.add_as_favorite_switch)
    private val cardNameField = withId(R.id.editText_cardName)
    private val cardHolderField = withId(R.id.editText_cardHolder)
    private val cardTypeField = withId(R.id.editText_cardType)
    private val cardNumberField = withId(R.id.editText_cardNumber)
    private val expiryField = withId(R.id.editText_expiry)
    private val securityCodeField = withId(R.id.editText_securityCode)
    private val notesField = withId(R.id.editText_notes)
    private val radioRed = withId(R.id.radio_red)
    private val radioGray = withId(R.id.radio_gray)
    private val saveButton = withId(R.id.save)
    private val loadingDialog = withId(R.id.progressBar)

    fun checkCreateCreditCardItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(cardNameField, timeout)
        waitForViewToDisplay(cardHolderField, timeout)
        waitForViewToDisplay(cardTypeField, timeout)
        waitForViewToDisplay(cardNumberField, timeout)
        waitForViewToDisplay(expiryField, timeout)
        waitForViewToDisplay(securityCodeField, timeout)
        waitForViewToDisplay(notesField, timeout)
    }

    fun clickOnSave() {
        Thread.sleep(1000)
        clickOnView(saveButton)
    }

    fun toggleFavorite() {
        clickOnView(favoriteSwitch)
    }

    fun scrollToTop() {
        onView(saveButton).perform(ViewActions.scrollTo())
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
