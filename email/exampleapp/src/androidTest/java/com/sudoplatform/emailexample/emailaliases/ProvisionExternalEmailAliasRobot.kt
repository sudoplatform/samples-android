/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaliases

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.R
import java.util.UUID

fun provisionExternalEmailAlias(func: ProvisionExternalEmailAliasRobot.() -> Unit) = ProvisionExternalEmailAliasRobot().apply { func() }

/**
 * Testing robot that manages the provision external email alias screen.
 */
class ProvisionExternalEmailAliasRobot : BaseRobot() {
    private val toolbarCreateButton = withId(R.id.create)
    private val externalEmailField = withId(R.id.externalEmailField)
    private val aliasLocalPartField = withId(R.id.aliasLocalPartField)
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

    fun checkProvisionExternalEmailAliasItemsDisplayed() {
        waitForViewToDisplay(toolbarCreateButton, 10_000L)
        waitForViewToDisplay(externalEmailField, 10_000L)
        waitForViewToDisplay(aliasLocalPartField, 10_000L)
        waitForViewToDisplay(aliasDomainSpinner, 10_000L)
        waitForViewToDisplay(sudoText, 10_000L)
        waitForViewToDisplay(sudoLabelText, 10_000L)
    }

    fun checkAvailableAddressMsg() {
        waitForViewToDisplay(availabilityLabel, 10_000L)
        onView(availabilityLabel).check(matches(withText("This email address is available")))
    }

    fun checkUnavailableAddressMsg() {
        waitForViewToDisplay(availabilityLabel, 10_000L)
        onView(availabilityLabel).check(matches(withText("This email address is not available")))
    }

    fun enterExternalEmail(email: String) {
        replaceText(externalEmailField, email)
    }

    fun setLocalPart(localPart: String? = "") {
        var input = localPart
        if (input.isNullOrBlank()) {
            input = UUID.randomUUID().toString().substring(0, 8)
        }
        replaceText(aliasLocalPartField, input)
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

    fun checkAlertDialogDisplayed() {
        waitForViewToDisplay(positiveAlertButton, 10_000L)
    }
}
