/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.emailexample.MainActivity
import com.sudoplatform.emailexample.emailaddresses.emailAddresses
import com.sudoplatform.emailexample.emailaddresses.provisionEmailAddress
import com.sudoplatform.emailexample.emailaliases.externalEmailAliases
import com.sudoplatform.emailexample.emailaliases.provisionExternalEmailAlias
import com.sudoplatform.emailexample.mainmenu.mainMenu
import com.sudoplatform.emailexample.sudos.createSudo
import com.sudoplatform.emailexample.sudos.sudos
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

/**
 * Test the send email message flow.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SendEmailMessageTest {
    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setup() {
        Timber.plant(Timber.DebugTree())
    }

    @After
    fun fini() {
        mainMenu {
            pressBackUntilDeregisterToolbarButtonIsDisplayed()
            clickOnDeregister()
            clickOnPositiveDeregisterAlertDialogButton()
            waitForLoading()
        }
        Timber.uprootAll()
    }

    @Test
    fun testSendAndDeleteEmailMessage() {
        createSudo {
            registerAndCreateSudoFlow()
        }
        emailAddresses {
            waitForLoading()
            checkEmailAddressesItemsDisplayed()
            navigateToProvisionEmailAddressScreen()
        }
        var address = ""
        provisionEmailAddress {
            setLocalPart()
            waitForRootFocus()
            checkAvailableAddressMsg()
            address = getAddressFromTextView()
            clickOnCreateButton()
            waitForLoading()
        }
        emailAddresses {
            waitForRecyclerView()
            checkEmailAddressesItemsDisplayed()
            navigateToEmailMessagesScreen(0)
        }
        emailMessages {
            navigateToSendEmailMessageScreen()
        }
        sendEmailMessage {
            // Attempt to send without filling out any fields
            clickOnSendEmailButton()
            waitForLoading()
            clickOnPositiveAlertDialogButton()

            // Attempt to send with fields filled out
            setToField(address)
            setSubjectField("Espresso Test Email")
            setContentBodyField("This is test email from an Espresso test.")
            clickOnSendEmailButton()
            waitForLoading()
        }
        emailMessages {
            waitForRecyclerView()
            checkEmailMessagesItemsDisplayed()

            // Navigate to SENT folder and swipe to move message to TRASH
            clickEmailFolderSpinner("SENT")
            waitForRecyclerView()
            checkEmailMessagesItemsDisplayed()
            swipeLeftToDelete(0)
            waitForLoading()

            // Navigate to TRASH folder and swipe to permanently delete message
            clickEmailFolderSpinner("TRASH")
            waitForRecyclerView()
            checkEmailMessagesItemsDisplayed()
            swipeLeftToDelete(0)
            waitForLoading()
            pressBack()
        }
        emailAddresses {
            waitForLoading()
            waitForRecyclerView()
            checkEmailAddressesItemsDisplayed()
            pressBack()
        }
        sudos {
            waitForLoading()
            waitForRecyclerView()
            checkSudosItemsDisplayed()
            pressBack()
        }
    }

    @Test
    fun testAttemptToSendEmailMessageWithInvalidAddress() {
        provisionEmailAddress {
            provisionEmailAddressFlow()
        }
        emailAddresses {
            waitForRecyclerView()
            checkEmailAddressesItemsDisplayed()
            navigateToEmailMessagesScreen(0)
        }
        emailMessages {
            navigateToSendEmailMessageScreen()
        }
        sendEmailMessage {
            setToField("fooBar123")
            clickOnSendEmailButton()
            waitForLoading()
            clickOnNegativeErrorAlertDialogButton()
            pressBack()
        }
        emailMessages {
            waitForLoading()
            waitForRecyclerView()
            checkEmailMessagesItemsDisplayed()
            pressBack()
        }
        emailAddresses {
            waitForLoading()
            waitForRecyclerView()
            checkEmailAddressesItemsDisplayed()
            pressBack()
        }
        sudos {
            waitForLoading()
            waitForRecyclerView()
            checkSudosItemsDisplayed()
            pressBack()
        }
    }

    @Test
    fun testSendReceiveForwardEmailMessage() {
        createSudo {
            registerAndCreateSudoFlow()
        }
        emailAddresses {
            waitForLoading()
            checkEmailAddressesItemsDisplayed()
            navigateToProvisionEmailAddressScreen()
        }
        var address = ""
        provisionEmailAddress {
            setLocalPart()
            waitForRootFocus()
            checkAvailableAddressMsg()
            address = getAddressFromTextView()
            clickOnCreateButton()
            waitForLoading()
        }
        emailAddresses {
            waitForRecyclerView()
            checkEmailAddressesItemsDisplayed()
            navigateToEmailMessagesScreen(0)
        }
        emailMessages {
            navigateToSendEmailMessageScreen()
        }
        sendEmailMessage {
            // Attempt to send with fields filled out
            setToField(address)
            setSubjectField("Espresso Test Email")
            setContentBodyField("This is test email from an Espresso test.")
            clickOnSendEmailButton()
            waitForLoading()
        }
        emailMessages {
            waitForRecyclerView()
            checkEmailMessagesItemsDisplayed()
            clickEmailFolderSpinner("SENT")
            waitForRecyclerView()
            checkEmailMessagesItemsDisplayed()
            pressBack()
        }
        emailAddresses {
            waitForLoading()
            waitForRecyclerView()
            navigateToEmailMessagesScreen(0)
        }
        emailMessages {
            navigateToReadEmailMessageScreen(0)
        }
        readEmailMessage {
            navigateToForwardScreen()
        }
        sendEmailMessage {
            setToField(address)
            checkSubjectFieldContainsForward()
            setContentBodyField("This is a forwarded email sent from an Espresso test.")
            clickOnSendEmailButton()
            waitForLoading()
        }
        emailMessages {
            waitForRecyclerView()
            checkEmailMessagesItemsDisplayed()
            pressBack()
        }
        emailAddresses {
            waitForLoading()
            waitForRecyclerView()
            checkEmailAddressesItemsDisplayed()
            pressBack()
        }
        sudos {
            waitForLoading()
            waitForRecyclerView()
            checkSudosItemsDisplayed()
            pressBack()
        }
    }

    @Test
    fun testSendReceiveReplyEmailMessage() {
        createSudo {
            registerAndCreateSudoFlow()
        }
        emailAddresses {
            waitForLoading()
            checkEmailAddressesItemsDisplayed()
            navigateToProvisionEmailAddressScreen()
        }
        var address = ""
        provisionEmailAddress {
            setLocalPart()
            waitForRootFocus()
            checkAvailableAddressMsg()
            address = getAddressFromTextView()
            clickOnCreateButton()
            waitForLoading()
        }
        emailAddresses {
            waitForRecyclerView()
            checkEmailAddressesItemsDisplayed()
            navigateToEmailMessagesScreen(0)
        }
        emailMessages {
            navigateToSendEmailMessageScreen()
        }
        sendEmailMessage {
            // Attempt to send with fields filled out
            setToField(address)
            setSubjectField("Espresso Test Email")
            setContentBodyField("This is test email from an Espresso test.")
            clickOnSendEmailButton()
            waitForLoading()
        }
        emailMessages {
            waitForRecyclerView()
            checkEmailMessagesItemsDisplayed()
            clickEmailFolderSpinner("SENT")
            waitForRecyclerView()
            checkEmailMessagesItemsDisplayed()
            pressBack()
        }
        emailAddresses {
            waitForLoading()
            waitForRecyclerView()
            navigateToEmailMessagesScreen(0)
        }
        emailMessages {
            navigateToReadEmailMessageScreen(0)
        }
        readEmailMessage {
            navigateToReplyScreen()
        }
        sendEmailMessage {
            checkToFieldFilled()
            checkSubjectFieldContainsReply()
            setContentBodyField("This is a reply email sent from an Espresso test.")
            clickOnSendEmailButton()
            waitForLoading()
        }
        emailMessages {
            waitForRecyclerView()
            checkEmailMessagesItemsDisplayed()
            pressBack()
        }
        emailAddresses {
            waitForLoading()
            waitForRecyclerView()
            checkEmailAddressesItemsDisplayed()
            pressBack()
        }
        sudos {
            waitForLoading()
            waitForRecyclerView()
            checkSudosItemsDisplayed()
            pressBack()
        }
    }

    @Test
    fun testEncryptedEmailAddressHandlesEncryptedIndicator() {
        createSudo {
            registerAndCreateSudoFlow()
        }
        emailAddresses {
            waitForLoading()
            checkEmailAddressesItemsDisplayed()
            navigateToProvisionEmailAddressScreen()
        }
        var address = ""
        provisionEmailAddress {
            setLocalPart()
            waitForRootFocus()
            checkAvailableAddressMsg()
            address = getAddressFromTextView()
            clickOnCreateButton()
            waitForLoading()
        }
        emailAddresses {
            waitForRecyclerView()
            checkEmailAddressesItemsDisplayed()
            navigateToEmailMessagesScreen(0)
        }
        emailMessages {
            navigateToSendEmailMessageScreen()
        }
        sendEmailMessage {
            checkEncryptedIndicatorNotVisible()
            setToField("$address,$address")
            checkEncryptedIndicatorVisible()
            setCcField(address)
            checkEncryptedIndicatorVisible()
            setToField("not-an-address")
            checkEncryptedIndicatorNotVisible()
            pressBack()
        }
        emailMessages {
            waitForLoading()
            waitForRecyclerView()
            checkEmailMessagesItemsDisplayed()
            pressBack()
        }
        emailAddresses {
            waitForLoading()
            waitForRecyclerView()
            checkEmailAddressesItemsDisplayed()
            pressBack()
        }
        sudos {
            waitForLoading()
            waitForRecyclerView()
            checkSudosItemsDisplayed()
            pressBack()
        }
    }

    @Test
    fun testExternalAliasAddressDoesNotShowEncryptedIndicator() {
        createSudo {
            registerAndCreateSudoFlow()
        }
        emailAddresses {
            waitForLoading()
            checkEmailAddressesItemsDisplayed()
            navigateToProvisionEmailAddressScreen()
        }
        var address = ""
        provisionEmailAddress {
            setLocalPart()
            waitForRootFocus()
            checkAvailableAddressMsg()
            address = getAddressFromTextView()
            clickOnCreateButton()
            waitForLoading()
        }
        emailAddresses {
            waitForRecyclerView()
            checkEmailAddressesItemsDisplayed()
        }
        // Provision an external email alias
        var externalAliasAddress = ""
        externalEmailAliases {
            checkSwitchDisplayed()
            toggleSwitch()
            checkCreateButtonTextExternal()
            clickOnCreateButton()
        }
        provisionExternalEmailAlias {
            checkProvisionExternalEmailAliasItemsDisplayed()
            enterExternalEmail("testuser@gmail.com")
            setLocalPart()
            waitForRootFocus()
            checkAvailableAddressMsg()
            externalAliasAddress = "testuser@gmail.com"
            clickOnCreateButton()
            waitForLoading()
        }
        emailAddresses {
            waitForRecyclerView()
            checkEmailAddressesItemsDisplayed()
            navigateToEmailMessagesScreen(0)
        }
        emailMessages {
            navigateToSendEmailMessageScreen()
        }
        sendEmailMessage {
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
            pressBack()
        }
        emailMessages {
            waitForLoading()
            waitForRecyclerView()
            checkEmailMessagesItemsDisplayed()
            pressBack()
        }
        emailAddresses {
            waitForLoading()
            waitForRecyclerView()
            checkEmailAddressesItemsDisplayed()
            pressBack()
        }
        sudos {
            waitForLoading()
            waitForRecyclerView()
            checkSudosItemsDisplayed()
            pressBack()
        }
    }
}
