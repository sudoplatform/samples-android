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
import com.sudoplatform.emailexample.emailaliases.externalEmailAliases
import com.sudoplatform.emailexample.emailaliases.provisionEmailAlias
import com.sudoplatform.emailexample.sudos.sudos
import com.sudoplatform.emailexample.withNotificationPermission
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test the send email message flow.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SendEmailMessageTest {
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
    fun testSendAndDeleteEmailMessage() {
        val address = registerAndProvisionEmailAddress()
        emailAddresses(composeRule) {
            navigateToEmailMessagesScreen(0)
        }
        emailMessages(composeRule) {
            navigateToSendEmailMessageScreen()
        }
        sendEmailMessage(composeRule) {
            // Attempt to send without filling out any fields
            clickOnSendEmailButton()
            clickOnPositiveAlertDialogButton()

            // Attempt to send with fields filled out
            setToField(address)
            setSubjectField("Espresso Test Email")
            setContentBodyField("This is test email from an Espresso test.")
            sendAndWaitForMessages()
        }
        emailMessages(composeRule) {
            // Navigate to SENT folder and swipe to move message to TRASH
            selectEmailFolder("SENT")
            waitForMessages()
            swipeLeftToDelete(0)
            waitForNoMessages()

            // Navigate to TRASH folder and swipe to permanently delete message
            selectEmailFolder("TRASH")
            waitForMessages()
            swipeLeftToDelete(0)
            confirmDeleteMessage()
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

    @Test
    fun testAttemptToSendEmailMessageWithInvalidAddress() {
        provisionEmailAddress(composeRule) {
            provisionEmailAddressFlow()
        }
        emailAddresses(composeRule) {
            waitForEmailAddresses()
            navigateToEmailMessagesScreen(0)
        }
        emailMessages(composeRule) {
            navigateToSendEmailMessageScreen()
        }
        sendEmailMessage(composeRule) {
            setToField("fooBar123")
            clickOnSendEmailButton()
            clickOnNegativeErrorAlertDialogButton()
            navigateBack()
        }
        emailMessages(composeRule) {
            checkEmailMessagesItemsDisplayed()
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
    fun testSendReceiveForwardEmailMessage() {
        val address = registerAndProvisionEmailAddress()
        sendMessageToSelfAndCheckSent(address)
        emailAddresses(composeRule) {
            waitForEmailAddresses()
            navigateToEmailMessagesScreen(0)
        }
        emailMessages(composeRule) {
            navigateToReadEmailMessageScreen(0)
        }
        readEmailMessage(composeRule) {
            navigateToForwardScreen()
        }
        sendEmailMessage(composeRule) {
            setToField(address)
            checkSubjectFieldContainsForward()
            setContentBodyField("This is a forwarded email sent from an Espresso test.")
            sendAndWaitForReadMessage()
        }
        readEmailMessage(composeRule) {
            navigateBack()
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
    fun testSendReceiveReplyEmailMessage() {
        val address = registerAndProvisionEmailAddress()
        sendMessageToSelfAndCheckSent(address)
        emailAddresses(composeRule) {
            waitForEmailAddresses()
            navigateToEmailMessagesScreen(0)
        }
        emailMessages(composeRule) {
            navigateToReadEmailMessageScreen(0)
        }
        readEmailMessage(composeRule) {
            navigateToReplyScreen()
        }
        sendEmailMessage(composeRule) {
            checkToFieldFilled()
            checkSubjectFieldContainsReply()
            setContentBodyField("This is a reply email sent from an Espresso test.")
            sendAndWaitForReadMessage()
        }
        readEmailMessage(composeRule) {
            navigateBack()
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
    fun testEncryptedEmailAddressHandlesEncryptedIndicator() {
        val address = registerAndProvisionEmailAddress()
        emailAddresses(composeRule) {
            navigateToEmailMessagesScreen(0)
        }
        emailMessages(composeRule) {
            navigateToSendEmailMessageScreen()
        }
        sendEmailMessage(composeRule) {
            checkEncryptedIndicatorNotVisible()
            setToField("$address,$address")
            checkEncryptedIndicatorVisible()
            setCcField(address)
            checkEncryptedIndicatorVisible()
            setToField("not-an-address")
            checkEncryptedIndicatorNotVisible()
            navigateBack()
        }
        emailMessages(composeRule) {
            checkEmailMessagesItemsDisplayed()
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
    fun testExternalAliasAddressDoesNotShowEncryptedIndicator() {
        val address = registerAndProvisionEmailAddress()
        // Provision an external email alias
        val externalAliasAddress = "testuser@gmail.com"
        externalEmailAliases(composeRule) {
            selectExternalAliases()
            checkCreateButtonTextExternal()
            clickOnCreateButton()
        }
        provisionEmailAlias(composeRule) {
            checkProvisionExternalEmailAliasItemsDisplayed()
            enterExternalEmail(externalAliasAddress)
            setLocalPart()
            checkAvailableAddressMsg()
            clickOnCreateButton()
        }
        emailAddresses(composeRule) {
            waitForEmailAddresses()
            navigateToEmailMessagesScreen(0)
        }
        emailMessages(composeRule) {
            navigateToSendEmailMessageScreen()
        }
        sendEmailMessage(composeRule) {
            // External alias address should NOT show encrypted indicator
            // because enableEncryption is false for external aliases
            checkEncryptedIndicatorNotVisible()
            setToField(externalAliasAddress)
            checkEncryptedIndicatorNotVisible()

            // Internal platform address should still show encrypted indicator
            setToField(address)
            checkEncryptedIndicatorVisible()

            // Mixing internal and external should NOT show encrypted indicator
            setToField("$address,$externalAliasAddress")
            checkEncryptedIndicatorNotVisible()
            navigateBack()
        }
        emailMessages(composeRule) {
            checkEmailMessagesItemsDisplayed()
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

    /**
     * Sends a message from the email addresses screen to [address], checks it is in SENT, and
     * goes back to the email addresses.
     */
    private fun sendMessageToSelfAndCheckSent(address: String) {
        emailAddresses(composeRule) {
            navigateToEmailMessagesScreen(0)
        }
        emailMessages(composeRule) {
            navigateToSendEmailMessageScreen()
        }
        sendEmailMessage(composeRule) {
            setToField(address)
            setSubjectField("Espresso Test Email")
            setContentBodyField("This is test email from an Espresso test.")
            sendAndWaitForMessages()
        }
        emailMessages(composeRule) {
            selectEmailFolder("SENT")
            waitForMessages()
            navigateBack()
        }
    }
}
