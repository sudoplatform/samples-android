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
import com.sudoplatform.emailexample.emailaliases.provisionEmailAlias
import com.sudoplatform.emailexample.sudos.sudos
import com.sudoplatform.emailexample.withNotificationPermission
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test the send aliased email message flow.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SendAliasedEmailTest {
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
    fun testSendFromDropdownIsDisplayed() {
        registerAndProvisionEmailAddress()
        emailAddresses(composeRule) {
            navigateToEmailMessagesScreen(0)
        }
        emailMessages(composeRule) {
            navigateToSendEmailMessageScreen()
        }
        sendAliasedEmail(composeRule) {
            checkSendFromDropdownDisplayed()
            checkSendFromDropdownDefaultSelected()
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
    fun testSendAliasedEmailMessage() {
        val address = registerAndProvisionEmailAddress()
        emailAddresses(composeRule) {
            navigateToCreateEmailAliasScreen()
        }
        provisionEmailAlias(composeRule) {
            checkProvisionEmailAliasItemsDisplayed()
            selectRealEmailAddress(0)
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
        sendAliasedEmail(composeRule) {
            checkSendFromDropdownDisplayed()
            // The first option sends from the email address; the alias follows it.
            selectEmailAlias(1)
        }
        sendEmailMessage(composeRule) {
            setToField(address)
            setSubjectField("Espresso Aliased Email Test")
            setContentBodyField("This is a test email sent from an email alias.")
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
    fun testSendEmailWithoutAlias() {
        val address = registerAndProvisionEmailAddress()
        emailAddresses(composeRule) {
            navigateToEmailMessagesScreen(0)
        }
        emailMessages(composeRule) {
            navigateToSendEmailMessageScreen()
        }
        sendAliasedEmail(composeRule) {
            checkSendFromDropdownDisplayed()
            checkSendFromDropdownDefaultSelected()
        }
        sendEmailMessage(composeRule) {
            setToField(address)
            setSubjectField("Espresso No Alias Email Test")
            setContentBodyField("This is a test email sent without an email alias.")
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
