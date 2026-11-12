/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaliases

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.R

fun externalEmailAliases(func: ExternalEmailAliasesRobot.() -> Unit) = ExternalEmailAliasesRobot().apply { func() }

/**
 * Testing robot that manages the external email aliases interactions on the
 * EmailAddresses screen — switch visibility, toggle filtering, verify button,
 * and verification code dialog.
 */
class ExternalEmailAliasesRobot : BaseRobot() {
    private val internalExternalSwitch = withId(R.id.internalExternalSwitch)
    private val aliasesDisabledText = withId(R.id.aliasesDisabledText)
    private val aliasesRecyclerView = withId(R.id.aliasesRecyclerView)
    private val createEmailAliasButton = withId(R.id.createEmailAliasButton)
    private val emptyAliasesStateText = withId(R.id.emptyAliasesStateText)
    private val verifyButton = withId(R.id.verifyButton)
    private val loadingProgress = withId(R.id.progressBar)
    private val positiveAlertButton = withId(android.R.id.button1)
    private val negativeAlertButton = withId(android.R.id.button2)

    fun waitForLoading() {
        waitForViewToDisplay(loadingProgress, 5_000L)
        waitForViewToNotDisplay(loadingProgress, 60_000L)
    }

    fun waitForRootFocus() {
        onView(isRoot()).perform(waitFor(5_000L))
    }

    // --- Switch visibility ---

    fun checkSwitchDisplayed() {
        waitForViewToDisplay(internalExternalSwitch, 10_000L)
    }

    fun checkSwitchNotDisplayed() {
        waitForViewToNotDisplay(internalExternalSwitch, 10_000L)
    }

    fun toggleSwitch() {
        clickOnView(internalExternalSwitch)
    }

    // --- Feature gate / disabled state ---

    fun checkDisabledMessageDisplayed() {
        waitForViewToDisplay(aliasesDisabledText, 10_000L)
    }

    fun checkDisabledMessageNotDisplayed() {
        waitForViewToNotDisplay(aliasesDisabledText, 10_000L)
    }

    fun checkAliasListHidden() {
        waitForViewToNotDisplay(aliasesRecyclerView, 10_000L)
    }

    fun checkCreateButtonHidden() {
        waitForViewToNotDisplay(createEmailAliasButton, 10_000L)
    }

    // --- Create button text ---

    fun checkCreateButtonText(expectedText: String) {
        waitForViewToDisplay(createEmailAliasButton, 10_000L)
        onView(createEmailAliasButton).check(matches(withText(expectedText)))
    }

    fun checkCreateButtonTextInternal() {
        waitForViewToDisplay(createEmailAliasButton, 10_000L)
        onView(createEmailAliasButton).check(matches(withText(R.string.create_email_alias_button)))
    }

    fun checkCreateButtonTextExternal() {
        waitForViewToDisplay(createEmailAliasButton, 10_000L)
        onView(createEmailAliasButton).check(matches(withText(R.string.create_external_email_alias_button)))
    }

    fun clickOnCreateButton() {
        waitForViewToDisplay(createEmailAliasButton, 10_000L)
        clickOnView(createEmailAliasButton)
    }

    // --- Alias list filtering ---

    fun checkAliasListDisplayed() {
        waitForViewToDisplay(aliasesRecyclerView, 10_000L)
    }

    fun checkEmptyAliasesStateDisplayed() {
        waitForViewToDisplay(emptyAliasesStateText, 10_000L)
    }

    fun waitForAliasesRecyclerView() {
        waitForViewToDisplay(aliasesRecyclerView, 10_000L)
    }

    fun checkAliasesRecyclerViewHasItems(minimumCount: Int = 1) {
        checkRecyclerViewHasMinimumItemAmount(aliasesRecyclerView, minimumCount, 10_000L)
    }

    // --- Verify button ---

    fun clickVerifyButtonOnItem(position: Int) {
        checkRecyclerViewHasMinimumItemAmount(aliasesRecyclerView, position + 1, 10_000L)
        onView(aliasesRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<EmailAliasViewHolder>(
                position,
                clickChildViewWithId(R.id.verifyButton),
            ),
        )
    }

    fun checkVerifyButtonDisplayedOnItem(position: Int) {
        checkRecyclerViewHasMinimumItemAmount(aliasesRecyclerView, position + 1, 10_000L)
        onView(aliasesRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<EmailAliasViewHolder>(
                position,
                checkChildViewDisplayed(R.id.verifyButton),
            ),
        )
    }

    // --- Verification code dialog ---

    fun checkVerificationCodeDialogDisplayed() {
        waitForViewToDisplay(positiveAlertButton, 60_000L)
    }

    fun enterVerificationCode(code: String) {
        // The verification code dialog uses a custom EditText with a hint
        onView(withHint(R.string.verification_code_prompt))
            .perform(click())
            .perform(replaceText(code))
            .perform(closeSoftKeyboard())
    }

    fun clickConfirmVerificationCode() {
        waitForViewToDisplay(positiveAlertButton, 10_000L)
        clickOnView(positiveAlertButton)
    }

    fun clickCancelVerificationDialog() {
        waitForViewToDisplay(negativeAlertButton, 10_000L)
        clickOnView(negativeAlertButton)
    }

    fun waitForVerificationToComplete() {
        waitForViewToNotDisplay(loadingProgress, 60_000L)
    }

    // --- Helper ViewActions for RecyclerView child views ---

    private fun clickChildViewWithId(id: Int): androidx.test.espresso.ViewAction =
        object : androidx.test.espresso.ViewAction {
            override fun getConstraints(): org.hamcrest.Matcher<android.view.View> = isDisplayed()

            override fun getDescription(): String = "Click on a child view with specified id."

            override fun perform(
                uiController: androidx.test.espresso.UiController,
                view: android.view.View,
            ) {
                val childView = view.findViewById<android.view.View>(id)
                childView?.performClick()
            }
        }

    private fun checkChildViewDisplayed(id: Int): androidx.test.espresso.ViewAction =
        object : androidx.test.espresso.ViewAction {
            override fun getConstraints(): org.hamcrest.Matcher<android.view.View> = isDisplayed()

            override fun getDescription(): String = "Check that a child view with specified id is displayed."

            override fun perform(
                uiController: androidx.test.espresso.UiController,
                view: android.view.View,
            ) {
                val childView = view.findViewById<android.view.View>(id)
                if (childView == null || childView.visibility != android.view.View.VISIBLE) {
                    throw AssertionError("View with id $id is not visible")
                }
            }
        }
}
