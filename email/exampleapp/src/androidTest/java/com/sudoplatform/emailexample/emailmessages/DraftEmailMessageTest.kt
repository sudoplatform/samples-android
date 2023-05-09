/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import androidx.test.espresso.Espresso
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.emailexample.MainActivity
import com.sudoplatform.emailexample.emailaddresses.emailAddresses
import com.sudoplatform.emailexample.emailaddresses.provisionEmailAddress
import com.sudoplatform.emailexample.mainmenu.mainMenu
import com.sudoplatform.emailexample.sudos.createSudo
import com.sudoplatform.emailexample.sudos.sudos
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

@RunWith(AndroidJUnit4::class)
@LargeTest
class DraftEmailMessageTest {

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
    fun testSaveEmptyDraftEmailMessage() {
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
            navigateToSendEmailMessageScreen()
        }
        sendEmailMessage {
            clickOnSaveDraftEmailButton()
            waitForLoading()
            clickOnPositiveAlertDialogButton()
        }
    }

    @Test
    fun testSaveRetrieveAndSendDraftEmailMessage() {
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
            setSubjectField("Espresso Test Draft Email")
            setContentBodyField("This is a test of a draft email")
            clickOnSaveDraftEmailButton()
            waitForLoading()
            clickOnPositiveAlertDialogButton()
        }
        emailMessages {
            waitForLoading()
            waitForRecyclerView()
            checkEmailMessagesItemsDisplayed()

            // Navigate to DRAFT folder
            clickEmailFolderSpinner("DRAFTS")
            waitForRecyclerView()
            checkEmailMessagesItemsDisplayed()
            navigateToEditDraftEmailMessageScreen(0)
        }
        sendEmailMessage {
            setCcField(address)
            clickOnSendEmailButton()
            waitForLoading()
            clickOnPositiveAlertDialogButton()
        }
        emailMessages {
            waitForLoading()
            waitForRecyclerView()
            checkEmailMessagesItemsDisplayed()
            Espresso.pressBack()
        }
        emailAddresses {
            waitForLoading()
            waitForRecyclerView()
            checkEmailAddressesItemsDisplayed()
            Espresso.pressBack()
        }
        sudos {
            waitForLoading()
            waitForRecyclerView()
            checkSudosItemsDisplayed()
            Espresso.pressBack()
        }
    }

    @Test
    fun testDeleteDraftEmailMessage() {
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
            navigateToSendEmailMessageScreen()
        }
        sendEmailMessage {
            setToField("test-recipient@test.com")
            setSubjectField("Espresso test draft email")
            setContentBodyField("This is a test of a draft email")
            clickOnSaveDraftEmailButton()
            waitForLoading()
            clickOnPositiveAlertDialogButton()
        }
        emailMessages {
            waitForLoading()
            waitForRecyclerView()
            checkEmailMessagesItemsDisplayed()

            // Navigate to DRAFTS folder and swipe to delete draft message
            clickEmailFolderSpinner("DRAFTS")
            waitForRecyclerView()
            checkEmailMessagesItemsDisplayed()
            swipeLeftToDelete(0)
            waitForLoading()
            clickOnPositiveAlertDialogButton()
        }
        emailAddresses {
            waitForLoading()
            waitForRecyclerView()
            checkEmailAddressesItemsDisplayed()
            Espresso.pressBack()
        }
        sudos {
            waitForLoading()
            waitForRecyclerView()
            checkSudosItemsDisplayed()
            Espresso.pressBack()
        }
    }
}
