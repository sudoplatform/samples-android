/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import androidx.compose.ui.test.junit4.ComposeTestRule
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.ui.TestTags

fun readEmailMessage(
    composeRule: ComposeTestRule,
    func: ReadEmailMessageRobot.() -> Unit,
) = ReadEmailMessageRobot(composeRule).apply { func() }

/**
 * Testing robot that manages the read email message screen.
 */
class ReadEmailMessageRobot(
    composeRule: ComposeTestRule,
) : BaseRobot(composeRule) {
    fun checkReadEmailMessageItemsDisplayed() {
        waitForTag(TestTags.REPLY_ACTION)
        waitForTag(TestTags.FORWARD_ACTION)
        waitForTag(TestTags.MESSAGE_DATE_TEXT)
        waitForTag(TestTags.MESSAGE_FROM_TEXT)
        waitForTag(TestTags.MESSAGE_TO_TEXT)
        waitForTag(TestTags.MESSAGE_CC_TEXT)
        waitForTag(TestTags.MESSAGE_SUBJECT_TEXT)
        waitForTag(TestTags.MESSAGE_BODY_TEXT)
    }

    fun navigateToReplyScreen() {
        clickOnTag(TestTags.REPLY_ACTION)
        sendEmailMessage(composeRule) {
            checkSendEmailMessageItemsDisplayed()
        }
    }

    fun navigateToForwardScreen() {
        clickOnTag(TestTags.FORWARD_ACTION)
        sendEmailMessage(composeRule) {
            checkSendEmailMessageItemsDisplayed()
        }
    }

    fun clickOnBlockEmailAddressButton() {
        clickOnTag(TestTags.BLOCK_ACTION)
    }

    /** Close the alert reporting the sender was blocked, which returns to the messages. */
    fun clickOnPositiveAlertDialogButton() {
        waitForText(string(R.string.success))
        clickOnTag(TestTags.DIALOG_CONFIRM)
        emailMessages(composeRule) {
            checkEmailMessagesItemsDisplayed()
        }
    }
}
