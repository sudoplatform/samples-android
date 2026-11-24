/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaddressblocklist

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.ui.TestTags

fun emailAddressBlocklist(
    composeRule: ComposeTestRule,
    func: EmailAddressBlocklistRobot.() -> Unit,
) = EmailAddressBlocklistRobot(composeRule).apply { func() }

/**
 * Testing robot that manages the email address blocklist screen.
 */
class EmailAddressBlocklistRobot(
    composeRule: ComposeTestRule,
) : BaseRobot(composeRule) {
    fun checkEmailAddressBlocklistItemsDisplayed() {
        waitForTag(TestTags.UNBLOCK_ACTION)
        waitForTag(TestTags.BLOCKED_ADDRESSES_LIST)
    }

    fun clickCheckboxOnItem(position: Int) {
        clickOnTagAtPosition(TestTags.BLOCKED_ADDRESS_CHECKBOX, position)
    }

    /** Unblock the ticked addresses and wait for them to leave the list. */
    fun clickOnDeleteButton() {
        val count = composeRule.onAllNodes(hasTestTag(TestTags.BLOCKED_ADDRESS_ROW)).fetchSemanticsNodes().size
        clickOnEnabledTag(TestTags.UNBLOCK_ACTION)
        waitForLoading()
        composeRule.waitUntil(DEFAULT_TIMEOUT_MS) {
            composeRule.onAllNodes(hasTestTag(TestTags.BLOCKED_ADDRESS_ROW)).fetchSemanticsNodes().size < count
        }
    }
}
