/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaliases

import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.R
import org.hamcrest.CoreMatchers.instanceOf
import java.util.UUID

fun provisionEmailAlias(func: ProvisionEmailAliasRobot.() -> Unit) = ProvisionEmailAliasRobot().apply { func() }

/**
 * Testing robot that manages the provision email alias screen.
 */
class ProvisionEmailAliasRobot : BaseRobot() {
    private val toolbarCreateButton = withId(R.id.create)
    private val aliasLocalPartField = withId(R.id.aliasLocalPartField)
    private val realEmailAddressSpinner = withId(R.id.realEmailAddressSpinner)
    private val aliasDomainSpinner = withId(R.id.aliasDomainSpinner)
    private val availabilityLabel = withId(R.id.availabilityLabel)
    private val sudoText = withId(R.id.sudoText)
    private val sudoLabelText = withId(R.id.sudoLabelText)
    private val loadingDialog = withId(R.id.progressBar)
    private val positiveAlertButton = withId(android.R.id.button1)

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 5_000L)
        waitForViewToNotDisplay(loadingDialog, 60_000L)
    }

    fun waitForRootFocus() {
        onView(isRoot()).perform(waitFor(5_000L))
    }

    fun checkProvisionEmailAliasItemsDisplayed() {
        waitForViewToDisplay(toolbarCreateButton)
        waitForViewToDisplay(aliasLocalPartField)
        waitForViewToDisplay(realEmailAddressSpinner)
        waitForViewToDisplay(aliasDomainSpinner)
        waitForViewToDisplay(sudoText)
        waitForViewToDisplay(sudoLabelText)
    }

    fun checkAvailableAddressMsg() {
        waitForViewToDisplay(availabilityLabel, 10_000L)
        onView(availabilityLabel).check(matches(withText("This email address is available")))
    }

    fun checkUnavailableAddressMsg() {
        waitForViewToDisplay(availabilityLabel, 10_000L)
        onView(availabilityLabel).check(matches(withText("This email address is not available")))
    }

    fun clickOnCreateButton() {
        onView(toolbarCreateButton).check(matches(isEnabled()))
        clickOnView(toolbarCreateButton)
    }

    fun clickOnPositiveAlertDialogButton() {
        waitForViewToDisplay(positiveAlertButton, 60_000L)
        onView(isRoot()).perform(waitFor(1_000L))
        clickOnView(positiveAlertButton)
    }

    fun setLocalPart(localPart: String? = "") {
        var input = localPart
        if (input.isNullOrBlank()) {
            input = UUID.randomUUID().toString().substring(0, 8)
        }
        replaceText(aliasLocalPartField, input)
    }

    fun selectRealEmailAddress(position: Int) {
        clickOnView(realEmailAddressSpinner)
        onView(isRoot()).perform(waitFor(500L))
        onData(instanceOf(String::class.java))
            .atPosition(position)
            .perform(click())
    }
}
