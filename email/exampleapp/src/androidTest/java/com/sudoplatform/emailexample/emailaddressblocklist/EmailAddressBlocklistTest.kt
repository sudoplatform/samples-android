/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaddressblocklist

import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.emailexample.MainActivity
import com.sudoplatform.emailexample.emailaddresses.emailAddresses
import com.sudoplatform.emailexample.emailaddresses.provisionEmailAddress
import com.sudoplatform.emailexample.emailmessages.emailMessages
import com.sudoplatform.emailexample.emailmessages.readEmailMessage
import com.sudoplatform.emailexample.emailmessages.sendEmailMessage
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
 * Test the email address blocklist flow.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class EmailAddressBlocklistTest {

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
    fun testNavigateToBlocklistScreen() {
        createSudo {
            registerAndCreateSudoFlow()
        }
        emailAddresses {
            waitForLoading()
            checkEmailAddressesItemsDisplayed()
            navigateToProvisionEmailAddressScreen()
        }
        provisionEmailAddress {
            setLocalPart()
            waitForRootFocus()
            checkAvailableAddressMsg()
            clickOnCreateButton()
            waitForLoading()
            clickOnPositiveAlertDialogButton()
        }
        emailAddresses {
            waitForRecyclerView()
            checkEmailAddressesItemsDisplayed()
            navigateToEmailMessagesScreen(0)
        }
        emailMessages {
            navigateToBlocklistScreen()
        }
        emailAddressBlocklist {
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
    fun testSendAndBlockThenUnblockEmailAddress() {
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
            clickOnPositiveAlertDialogButton()
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
            setToField(address)
            setSubjectField("Espresso Test Email")
            setContentBodyField("This is test email from an Espresso test.")
            clickOnSendEmailButton()
            waitForLoading()
            clickOnPositiveAlertDialogButton()
        }
        emailMessages {
            waitForLoading()
            waitForRecyclerView()
            checkEmailMessagesItemsDisplayed()
            clickEmailFolderSpinner("SENT")
            waitForRecyclerView()
            checkEmailMessagesItemsDisplayed()
        }
        emailMessages {
            navigateToReadEmailMessageScreen(0)
        }
        readEmailMessage {
            // Block the email address
            clickOnBlockEmailAddressButton()
            waitForLoading()
            clickOnPositiveAlertDialogButton()
        }
        emailMessages {
            navigateToBlocklistScreen()
        }
        emailAddressBlocklist {
            // Unblock the email address
            waitForLoading()
            checkEmailAddressBlocklistItemsDisplayed()
            clickCheckboxOnRecyclerViewItem(0)
            clickOnDeleteButton()
            waitForLoading()
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
