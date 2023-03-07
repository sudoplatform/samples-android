/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaddresses

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.sudos.createSudo
import java.util.UUID

fun provisionEmailAddress(func: ProvisionEmailAddressRobot.() -> Unit) = ProvisionEmailAddressRobot().apply { func() }

/**
 * Testing robot that manages the provision email address screen.
 */
class ProvisionEmailAddressRobot : BaseRobot() {

    private val toolbarCreateButton = withId(R.id.create)
    private val addressEditText = withId(R.id.addressField)
    private val addressHolder = withId(R.id.addressHolder)
    private val availabilityLabel = withId(R.id.availabilityLabel)
    private val sudoTextView = withId(R.id.sudoText)
    private val learnMoreTextView = withId(R.id.learnMoreTextView)
    private val learnMoreButton = withId(R.id.learnMoreButton)
    private val loadingDialog = withId(R.id.progressBar)
    private val positiveAlertButton = withId(android.R.id.button1)
    private val negativeAlertButton = withId(android.R.id.button2)

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 5_000L)
        waitForViewToNotDisplay(loadingDialog, 60_000L)
    }

    fun waitForRootFocus() {
        onView(isRoot()).perform(waitFor(5_000L))
    }

    fun checkProvisionEmailAddressItemsDisplayed() {
        waitForViewToDisplay(toolbarCreateButton)
        waitForViewToDisplay(addressEditText)
        waitForViewToDisplay(sudoTextView)
        waitForViewToDisplay(learnMoreTextView)
        waitForViewToDisplay(learnMoreButton)
    }

    fun getAddressFromTextView(): String {
        return getTextFromTextView(onView(addressHolder))
    }

    fun checkAvailableAddressMsg() {
        onView(availabilityLabel).check(matches(withText("This email address is available")))
    }

    fun checkUnavailableAddressMsg() {
        onView(availabilityLabel).check(matches(withText("This email address is not available")))
    }

    fun checkInvalidAddressMsg() {
        onView(availabilityLabel).check(matches(withText("Invalid email address")))
    }

    fun clickOnCreateButton() {
        onView(toolbarCreateButton).check(matches(isEnabled()))
        clickOnView(toolbarCreateButton)
    }

    fun clickOnPositiveAlertDialogButton() {
        checkAlertDialog()
        Thread.sleep(1_000L)
        clickOnView(positiveAlertButton)
    }

    fun clickOnNegativeErrorAlertDialogButton() {
        checkErrorAlertDialog()
        Thread.sleep(1_000L)
        clickOnView(negativeAlertButton)
    }

    fun setLocalPart(localPart: String? = "") {
        var input = localPart
        if (input.isNullOrBlank()) {
            input = UUID.randomUUID().toString().substring(0, 8)
        }
        setTextFieldValue(input)
    }

    fun provisionEmailAddressFlow() {
        createSudo {
            registerAndCreateSudoFlow()
        }
        emailAddresses {
            waitForLoading()
            checkEmailAddressesItemsDisplayed()
            navigateToProvisionEmailAddressScreen()
        }
        setLocalPart()
        waitForRootFocus()
        checkAvailableAddressMsg()
        clickOnCreateButton()
        waitForLoading()
    }

    private fun setTextFieldValue(inputText: String) {
        replaceText(addressEditText, inputText)
    }

    private fun checkAlertDialog() {
        waitForViewToDisplay(positiveAlertButton, 60_000L)
        Thread.sleep(1_000L)
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
