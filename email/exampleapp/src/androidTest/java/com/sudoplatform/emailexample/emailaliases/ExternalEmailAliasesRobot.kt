/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaliases

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.ui.TestTags

fun externalEmailAliases(
    composeRule: ComposeTestRule,
    func: ExternalEmailAliasesRobot.() -> Unit,
) = ExternalEmailAliasesRobot(composeRule).apply { func() }

/**
 * Testing robot that manages the external email aliases interactions on the email addresses
 * screen: the Internal/External choice, filtering, the verify button and the verification code
 * dialog.
 */
class ExternalEmailAliasesRobot(
    composeRule: ComposeTestRule,
) : BaseRobot(composeRule) {
    // --- Internal/External choice ---

    /** The Internal/External buttons show when the configuration enables external aliases. */
    fun checkSwitchDisplayed() {
        scrollToAliases(hasTestTag(TestTags.EXTERNAL_ALIASES_BUTTON))
        waitForTag(TestTags.INTERNAL_ALIASES_BUTTON)
    }

    fun selectExternalAliases() {
        checkSwitchDisplayed()
        clickOnTag(TestTags.EXTERNAL_ALIASES_BUTTON)
        composeRule.onNodeWithTag(TestTags.EXTERNAL_ALIASES_BUTTON).assertIsSelected()
    }

    fun selectInternalAliases() {
        checkSwitchDisplayed()
        clickOnTag(TestTags.INTERNAL_ALIASES_BUTTON)
        composeRule.onNodeWithTag(TestTags.INTERNAL_ALIASES_BUTTON).assertIsSelected()
    }

    // --- Feature gate / disabled state ---

    fun checkDisabledMessageNotDisplayed() {
        composeRule.onNodeWithTag(TestTags.ALIASES_DISABLED_TEXT).assertDoesNotExist()
    }

    // --- Create button text ---

    fun checkCreateButtonTextInternal() {
        checkCreateButtonText(string(R.string.create_email_alias_button))
    }

    fun checkCreateButtonTextExternal() {
        checkCreateButtonText(string(R.string.create_external_email_alias_button))
    }

    fun clickOnCreateButton() {
        scrollToAliases(hasTestTag(TestTags.CREATE_EMAIL_ALIAS_BUTTON))
        clickOnTag(TestTags.CREATE_EMAIL_ALIAS_BUTTON)
    }

    // --- Alias list filtering ---

    /** Wait for the list to show at least one alias of the selected type. */
    fun checkAliasListDisplayed() {
        scrollToAliases(hasTestTag(TestTags.EMAIL_ALIAS_ROW))
    }

    fun checkEmptyAliasesStateDisplayed() {
        scrollToAliases(hasTestTag(TestTags.EMPTY_ALIASES_TEXT))
    }

    fun checkAliasesHaveItems(minimumCount: Int = 1) {
        checkAliasListDisplayed()
        waitForTagCount(TestTags.EMAIL_ALIAS_ROW, minimumCount)
    }

    // --- Verify button ---

    fun clickVerifyButtonOnItem(position: Int) {
        checkAliasListDisplayed()
        clickOnTagAtPosition(TestTags.VERIFY_BUTTON, position)
    }

    // --- Verification code dialog ---

    fun checkVerificationCodeDialogDisplayed() {
        waitForTag(TestTags.VERIFICATION_CODE_FIELD)
    }

    fun enterVerificationCode(code: String) {
        replaceText(TestTags.VERIFICATION_CODE_FIELD, code)
    }

    fun clickConfirmVerificationCode() {
        clickOnTag(TestTags.DIALOG_CONFIRM)
        waitForTagToDisappear(TestTags.VERIFICATION_CODE_FIELD)
    }

    /**
     * Wait for the code to be checked. A code the service doesn't accept is reported with
     * **TRY AGAIN** and **CANCEL**, so cancel that report if it shows.
     */
    fun waitForVerificationToComplete() {
        waitForLoading()
        if (composeRule.onAllNodes(hasTestTag(TestTags.DIALOG_DISMISS)).fetchSemanticsNodes().isNotEmpty()) {
            clickOnTag(TestTags.DIALOG_DISMISS)
            waitForTagToDisappear(TestTags.DIALOG_DISMISS)
        }
    }

    /** Check the create button's label, which the button shows in upper case. */
    private fun checkCreateButtonText(text: String) {
        scrollToAliases(hasTestTag(TestTags.CREATE_EMAIL_ALIAS_BUTTON))
        waitForNode(hasTestTag(TestTags.CREATE_EMAIL_ALIAS_BUTTON) and hasText(text.uppercase()))
    }

    private fun scrollToAliases(matcher: SemanticsMatcher) {
        scrollListTo(TestTags.EMAIL_ADDRESSES_LIST, matcher)
    }
}
