/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.emailexample.MainActivity
import com.sudoplatform.emailexample.emailaddresses.emailAddresses
import com.sudoplatform.emailexample.emailaddresses.provisionEmailAddress
import com.sudoplatform.emailexample.sudos.sudos
import com.sudoplatform.emailexample.withNotificationPermission
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test saving, editing, sending and deleting draft email messages.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class DraftEmailMessageTest {
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules = withNotificationPermission(composeRule)

    @After
    fun fini() {
        sudos(composeRule) {
            pressBackUntilSudosAreDisplayed()
            deregister()
        }
    }

    @Test
    fun testSaveEmptyDraftEmailMessage() {
        registerAndProvisionEmailAddress()
        emailAddresses(composeRule) {
            navigateToEmailMessagesScreen(0)
        }
        emailMessages(composeRule) {
            navigateToSendEmailMessageScreen()
        }
        sendEmailMessage(composeRule) {
            clickOnSaveDraftEmailButton()
        }
    }

    @Test
    fun testSaveRetrieveAndSendDraftEmailMessage() {
        val address = registerAndProvisionEmailAddress()
        emailAddresses(composeRule) {
            navigateToEmailMessagesScreen(0)
        }
        emailMessages(composeRule) {
            navigateToSendEmailMessageScreen()
        }
        sendEmailMessage(composeRule) {
            setToField(address)
            setSubjectField("Espresso Test Draft Email")
            setContentBodyField("This is a test of a draft email")
            clickOnSaveDraftEmailButton()
        }
        emailMessages(composeRule) {
            // Navigate to DRAFTS folder
            selectDraftsFolder()
            waitForMessages()
            navigateToEditDraftEmailMessageScreen(0)
        }
        sendEmailMessage(composeRule) {
            setCcField(address)
            sendAndWaitForMessages()
        }
        emailMessages(composeRule) {
            navigateBack()
        }
        emailAddresses(composeRule) {
            waitForEmailAddresses()
            navigateBack()
        }
        sudos(composeRule) {
            checkSudosItemsDisplayed()
        }
    }

    @Test
    fun testDeleteDraftEmailMessage() {
        registerAndProvisionEmailAddress()
        emailAddresses(composeRule) {
            navigateToEmailMessagesScreen(0)
        }
        emailMessages(composeRule) {
            navigateToSendEmailMessageScreen()
        }
        sendEmailMessage(composeRule) {
            setToField("test-recipient@test.com")
            setSubjectField("Espresso test draft email")
            setContentBodyField("This is a test of a draft email")
            clickOnSaveDraftEmailButton()
        }
        emailMessages(composeRule) {
            // Navigate to DRAFTS folder and swipe to delete draft message
            selectDraftsFolder()
            waitForMessages()
            swipeLeftToDelete(0)
            waitForNoMessages()
            navigateBack()
        }
        emailAddresses(composeRule) {
            waitForEmailAddresses()
            navigateBack()
        }
        sudos(composeRule) {
            checkSudosItemsDisplayed()
        }
    }

    /**
     * Registers, creates a Sudo and provisions an email address, ending on the email addresses.
     *
     * @return the provisioned address.
     */
    private fun registerAndProvisionEmailAddress(): String {
        var address = ""
        provisionEmailAddress(composeRule) {
            address = provisionEmailAddressFlow()
        }
        return address
    }
}
