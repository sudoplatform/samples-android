/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.emailaddressblocklist.emailAddressBlocklist
import com.sudoplatform.emailexample.ui.TestTags

fun emailMessages(
    composeRule: ComposeTestRule,
    func: EmailMessagesRobot.() -> Unit,
) = EmailMessagesRobot(composeRule).apply { func() }

/**
 * Testing robot that manages the email messages screen.
 */
class EmailMessagesRobot(
    composeRule: ComposeTestRule,
) : BaseRobot(composeRule) {
    fun checkEmailMessagesItemsDisplayed() {
        waitForTag(TestTags.COMPOSE_BUTTON)
        waitForTag(TestTags.FOLDER_DROPDOWN)
    }

    /** Wait for the selected folder to show at least one message. */
    fun waitForMessages() {
        waitForTag(TestTags.EMAIL_MESSAGE_ROW)
    }

    fun navigateToSendEmailMessageScreen() {
        clickOnTag(TestTags.COMPOSE_BUTTON)
        sendEmailMessage(composeRule) {
            checkSendEmailMessageItemsDisplayed()
        }
    }

    /** Open the draft at [position], with the Drafts folder selected. */
    fun navigateToEditDraftEmailMessageScreen(position: Int) {
        clickOnTagAtPosition(TestTags.EMAIL_MESSAGE_ROW, position)
        sendEmailMessage(composeRule) {
            checkSendEmailMessageItemsDisplayed()
        }
    }

    fun navigateToReadEmailMessageScreen(position: Int) {
        clickOnTagAtPosition(TestTags.EMAIL_MESSAGE_ROW, position)
        readEmailMessage(composeRule) {
            checkReadEmailMessageItemsDisplayed()
        }
    }

    /** Select the folder named [folderName], such as "SENT", or "DRAFTS" for the drafts. */
    fun selectEmailFolder(folderName: String) {
        selectDropdownOption(TestTags.FOLDER_DROPDOWN, folderName)
    }

    fun selectDraftsFolder() {
        selectEmailFolder(string(R.string.drafts_folder))
    }

    fun navigateToBlocklistScreen() {
        clickOnTag(TestTags.MORE_ACTIONS)
        clickOnTag(TestTags.BLOCKLIST_MENU_ITEM)
        emailAddressBlocklist(composeRule) {
            checkEmailAddressBlocklistItemsDisplayed()
        }
    }

    /**
     * Swipe the message at [position] away. Outside the trash and drafts this moves it to the
     * trash; in the trash, [confirmDeleteMessage] must follow.
     */
    fun swipeLeftToDelete(position: Int) {
        swipeLeftOnTagAtPosition(TestTags.EMAIL_MESSAGE_ROW, position)
    }

    /** Confirm permanently deleting a message swiped away in the trash. */
    fun confirmDeleteMessage() {
        waitForText(string(R.string.delete_email_message_title))
        clickOnTag(TestTags.DIALOG_CONFIRM)
    }

    /** Wait for the selected folder to show no messages, once a delete has finished. */
    fun waitForNoMessages() {
        waitForLoading()
        composeRule.waitUntil(DEFAULT_TIMEOUT_MS) {
            composeRule.onAllNodes(hasTestTag(TestTags.EMAIL_MESSAGE_ROW)).fetchSemanticsNodes().isEmpty()
        }
    }
}
