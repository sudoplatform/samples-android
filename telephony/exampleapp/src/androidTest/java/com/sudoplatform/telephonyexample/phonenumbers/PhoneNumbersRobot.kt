/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.phonenumbers

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.sudoplatform.telephonyexample.BaseRobot
import com.sudoplatform.telephonyexample.ui.TestTags

fun phoneNumbers(
    composeRule: ComposeTestRule,
    func: PhoneNumbersRobot.() -> Unit,
) = PhoneNumbersRobot(composeRule).apply { func() }

/**
 * Testing robot that manages the Phone Numbers and Provision Phone Number screens.
 */
@OptIn(ExperimentalTestApi::class)
class PhoneNumbersRobot(
    composeRule: ComposeTestRule,
) : BaseRobot(composeRule) {
    fun waitForPhoneNumbers() {
        waitForTag(TestTags.PROVISION_PHONE_NUMBER_BUTTON)
    }

    /** Open Provision Phone Number and wait for the supported countries to load. */
    fun navigateToProvision() {
        clickOnTag(TestTags.PROVISION_PHONE_NUMBER_BUTTON)
        waitForTag(TestTags.AREA_CODE_FIELD)
    }

    fun search(areaCode: String) {
        composeRule.onNodeWithTag(TestTags.AREA_CODE_FIELD).performTextClearance()
        if (areaCode.isNotEmpty()) {
            composeRule.onNodeWithTag(TestTags.AREA_CODE_FIELD).performTextInput(areaCode)
        }
        clickOnTag(TestTags.SEARCH_ACTION)
    }

    /**
     * Wait for a search to finish.
     *
     * @return true if it offered numbers, false if the instance has none in that area code.
     */
    fun waitForSearchOutcome(): Boolean {
        val results = hasTestTag(TestTags.AVAILABLE_PHONE_NUMBER_ROW)
        val noResults = hasText("No numbers are available in this area code.")
        composeRule.waitUntil(DEFAULT_TIMEOUT_MS) {
            composeRule.onAllNodes(results).fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(noResults).fetchSemanticsNodes().isNotEmpty()
        }
        return composeRule.onAllNodes(results).fetchSemanticsNodes().isNotEmpty()
    }

    /** Tap the first available number, which asks the user to confirm provisioning it. */
    fun selectFirstSearchResult() {
        composeRule.onAllNodesWithTag(TestTags.AVAILABLE_PHONE_NUMBER_ROW).onFirst().performClick()
        waitForTag(TestTags.DIALOG_CONFIRM)
    }

    fun cancelDialog() {
        clickOnTag(TestTags.DIALOG_DISMISS)
        waitForTagToDisappear(TestTags.DIALOG_DISMISS)
    }
}
