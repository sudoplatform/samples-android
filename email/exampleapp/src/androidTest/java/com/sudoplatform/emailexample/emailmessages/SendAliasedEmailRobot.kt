/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.ui.TestTags

fun sendAliasedEmail(
    composeRule: ComposeTestRule,
    func: SendAliasedEmailRobot.() -> Unit,
) = SendAliasedEmailRobot(composeRule).apply { func() }

/**
 * Testing robot that manages the compose screen's choice of sending from the email address or
 * from one of its aliases.
 */
class SendAliasedEmailRobot(
    composeRule: ComposeTestRule,
) : BaseRobot(composeRule) {
    fun checkSendFromDropdownDisplayed() {
        waitForTag(TestTags.SEND_FROM_FIELD)
    }

    /**
     * Choose the option at [position]: 0 sends from the email address, and the aliases that
     * forward to it follow once they have loaded.
     */
    fun selectEmailAlias(position: Int) {
        selectDropdownOption(TestTags.SEND_FROM_FIELD, position)
    }

    fun checkSendFromDropdownDefaultSelected() {
        waitForNode(hasTestTag(TestTags.SEND_FROM_FIELD) and hasText(string(R.string.send_aliased_email_none_option)))
    }
}
