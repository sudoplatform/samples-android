/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaddressblocklist

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.emailexample.MainActivity
import com.sudoplatform.emailexample.emailaddresses.emailAddresses
import com.sudoplatform.emailexample.emailaddresses.provisionEmailAddress
import com.sudoplatform.emailexample.emailmessages.emailMessages
import com.sudoplatform.emailexample.emailmessages.readEmailMessage
import com.sudoplatform.emailexample.emailmessages.sendEmailMessage
import com.sudoplatform.emailexample.sudos.sudos
import com.sudoplatform.emailexample.withNotificationPermission
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test the email address blocklist flow.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class EmailAddressBlocklistTest {
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
    fun testNavigateToBlocklistScreen() {
        registerAndProvisionEmailAddress()
        emailAddresses(composeRule) {
            navigateToEmailMessagesScreen(0)
        }
        emailMessages(composeRule) {
            navigateToBlocklistScreen()
        }
        emailAddressBlocklist(composeRule) {
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
    fun testSendAndBlockThenUnblockEmailAddress() {
        val address = registerAndProvisionEmailAddress()
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
            navigateToReadEmailMessageScreen(0)
        }
        readEmailMessage(composeRule) {
            // Block the email address
            clickOnBlockEmailAddressButton()
            clickOnPositiveAlertDialogButton()
        }
        emailMessages(composeRule) {
            navigateToBlocklistScreen()
        }
        emailAddressBlocklist(composeRule) {
            // Unblock the email address
            clickCheckboxOnItem(0)
            clickOnDeleteButton()
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
}
