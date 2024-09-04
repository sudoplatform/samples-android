/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.fundingsources

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.sudoplatform.virtualcardsexample.BaseRobot
import com.sudoplatform.virtualcardsexample.R
import org.hamcrest.CoreMatchers

fun createFundingSource(func: CreateFundingSourceRobot.() -> Unit) = CreateFundingSourceRobot().apply { func() }

/**
 * Testing robot that manages the Create Funding Source screen.
 */
class CreateFundingSourceRobot : BaseRobot() {

    private val inputForm = R.id.formRecyclerView
    private val toolbarCreateButton = withId(R.id.create)
    private val loadingDialog = withId(R.id.progressBar)
    private val positiveAlertButton = withId(android.R.id.button1)
    private val negativeAlertButton = withId(android.R.id.button2)
    private val launchPlaidLinkButton = withId(R.id.launchPlaidLinkButton)

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 5_000L, true)
        waitForViewToNotDisplay(loadingDialog, 60_000L)
    }

    fun checkCreateCreditCardFundingSourceItemsDisplayed() {
        waitForViewToDisplay(withId(inputForm))
        waitForViewToDisplay(toolbarCreateButton)
    }

    fun checkCreateBankAccountFundingSourceItemsDisplayed() {
        waitForViewToDisplay(launchPlaidLinkButton)
        waitForViewToDisplay(toolbarCreateButton)
    }

    fun clickOnCreateButton() {
        clickOnView(toolbarCreateButton)
    }

    fun clickOnPositiveAlertDialogButton() {
        checkAlertDialog()
        Thread.sleep(1000)
        clickOnView(positiveAlertButton)
    }

    fun clickOnNegativeErrorAlertDialogButton() {
        checkErrorAlertDialog()
        Thread.sleep(1000)
        clickOnView(negativeAlertButton)
    }

    fun setCardNumber(cardNumber: String) {
        setCellValue("Card Number", cardNumber)
    }

    fun setExpiryMonth(month: String) {
        setCellValue("Month", month)
    }

    fun setExpiryYear(year: String) {
        setCellValue("Year", year)
    }

    fun setSecurityCode(securityCode: String) {
        setCellValue("Security Code", securityCode)
    }

    fun setAddressLine1(addressLine1: String) {
        setCellValue("Address Line 1", addressLine1)
    }

    fun setCity(city: String) {
        setCellValue("City", city)
    }

    fun setState(state: String) {
        setCellValue("State", state)
    }

    fun setPostalCode(postalCode: String) {
        setCellValue("Postal Code", postalCode)
    }

    fun setCountry(country: String) {
        setCellValue("Country", country)
    }

    private fun setCellValue(cellTitle: String, inputText: String) {
        scrollToCellWithTitle(cellTitle)
        replaceText(
            CoreMatchers.allOf(
                withId(R.id.inputField),
                withHint("Enter $cellTitle"),
            ),
            inputText,
        )
    }

    private fun scrollToCellWithTitle(title: String) {
        scrollToViewInRecyclerView(
            inputForm,
            hasDescendant(
                CoreMatchers.allOf(
                    withId(R.id.title),
                    withText(title),
                ),
            ),
        )
    }

    private fun checkAlertDialog() {
        waitForViewToDisplay(positiveAlertButton, 60_000L)
        Thread.sleep(1000)
        onView(positiveAlertButton)
            .check(matches(withText(android.R.string.ok)))
    }

    private fun checkErrorAlertDialog() {
        waitForViewToDisplay(positiveAlertButton, 15_000L)
        waitForViewToDisplay(negativeAlertButton, 15_000L)
        onView(positiveAlertButton)
            .check(matches(withText(R.string.try_again)))
        onView(negativeAlertButton)
            .check(matches(withText(android.R.string.cancel)))
    }
}
