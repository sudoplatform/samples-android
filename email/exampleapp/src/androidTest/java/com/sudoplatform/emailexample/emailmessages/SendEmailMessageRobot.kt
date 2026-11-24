/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.ui.TestTags

fun sendEmailMessage(
    composeRule: ComposeTestRule,
    func: SendEmailMessageRobot.() -> Unit,
) = SendEmailMessageRobot(composeRule).apply { func() }

/**
 * Testing robot that manages the compose screen, which sends new messages, replies, forwards
 * and drafts.
 */
class SendEmailMessageRobot(
    composeRule: ComposeTestRule,
) : BaseRobot(composeRule) {
    fun checkSendEmailMessageItemsDisplayed() {
        waitForTag(TestTags.SEND_ACTION)
        waitForTag(TestTags.TO_FIELD)
        waitForTag(TestTags.CC_FIELD)
        waitForTag(TestTags.BCC_FIELD)
        waitForTag(TestTags.SUBJECT_FIELD)
        waitForTag(TestTags.BODY_FIELD)
    }

    /** Tap Send, which is enabled once the sending address has loaded. */
    fun clickOnSendEmailButton() {
        clickOnEnabledTag(TestTags.SEND_ACTION)
    }

    /** Send the message and wait for the messages, which the app returns to once it is sent. */
    fun sendAndWaitForMessages() {
        clickOnSendEmailButton()
        waitForMessages()
    }

    /** Send a reply or forward and wait for the message it came from, which the app returns to once it is sent. */
    fun sendAndWaitForReadMessage() {
        clickOnSendEmailButton()
        waitForTagToDisappear(TestTags.SEND_ACTION)
        readEmailMessage(composeRule) {
            checkReadEmailMessageItemsDisplayed()
        }
    }

    /** Save the draft and wait for the messages, which the app returns to once it is saved. */
    fun clickOnSaveDraftEmailButton() {
        clickOnEnabledTag(TestTags.SAVE_DRAFT_ACTION)
        waitForMessages()
    }

    /** Close the alert asking for a To address. */
    fun clickOnPositiveAlertDialogButton() {
        waitForText(string(R.string.validate_to_address))
        clickOnTag(TestTags.DIALOG_CONFIRM)
        waitForTagToDisappear(TestTags.DIALOG_CONFIRM)
    }

    /** Cancel the alert reporting that the message couldn't be sent, which offers to try again. */
    fun clickOnNegativeErrorAlertDialogButton() {
        waitForText(string(R.string.send_email_message_failure))
        composeRule.onNodeWithTag(TestTags.DIALOG_CONFIRM).assert(hasText(string(R.string.try_again).uppercase()))
        composeRule.onNodeWithTag(TestTags.DIALOG_DISMISS).assert(hasText(string(R.string.cancel).uppercase()))
        clickOnTag(TestTags.DIALOG_DISMISS)
        waitForTagToDisappear(TestTags.DIALOG_DISMISS)
    }

    fun setToField(to: String) {
        replaceText(TestTags.TO_FIELD, to)
    }

    fun setCcField(cc: String) {
        replaceText(TestTags.CC_FIELD, cc)
    }

    fun setBccField(bcc: String) {
        replaceText(TestTags.BCC_FIELD, bcc)
    }

    fun setSubjectField(subject: String) {
        replaceText(TestTags.SUBJECT_FIELD, subject)
    }

    fun setContentBodyField(body: String) {
        replaceText(TestTags.BODY_FIELD, body)
    }

    /** Wait for To to be filled in, as it is for a reply. */
    fun checkToFieldFilled() {
        composeRule.waitUntil(DEFAULT_TIMEOUT_MS) { textOf(TestTags.TO_FIELD).isNotBlank() }
    }

    fun checkSubjectFieldContainsReply() {
        waitForNode(hasTestTag(TestTags.SUBJECT_FIELD) and hasText("Re:", substring = true))
    }

    fun checkSubjectFieldContainsForward() {
        waitForNode(hasTestTag(TestTags.SUBJECT_FIELD) and hasText("Fwd:", substring = true))
    }

    /** Wait for the end-to-end encrypted indicator, which follows the recipients a moment after they change. */
    fun checkEncryptedIndicatorVisible() {
        waitForTag(TestTags.ENCRYPTED_INDICATOR)
    }

    fun checkEncryptedIndicatorNotVisible() {
        waitForTagToDisappear(TestTags.ENCRYPTED_INDICATOR)
    }

    private fun waitForMessages() {
        waitForTagToDisappear(TestTags.SEND_ACTION)
        emailMessages(composeRule) {
            checkEmailMessagesItemsDisplayed()
        }
    }
}
