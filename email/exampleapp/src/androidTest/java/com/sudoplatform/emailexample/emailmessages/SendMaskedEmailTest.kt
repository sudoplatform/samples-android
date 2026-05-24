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
import com.sudoplatform.emailexample.emailmasks.provisionEmailMask
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
 * Test the send masked email message flow.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SendMaskedEmailTest {
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
    fun testEmailMaskSpinnerIsDisplayed() {
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
        }
        emailAddresses {
            waitForRecyclerView()
            checkEmailAddressesItemsDisplayed()
            navigateToEmailMessagesScreen(0)
        }
        emailMessages {
            navigateToSendEmailMessageScreen()
        }
        sendMaskedEmail {
            checkEmailMaskSpinnerDisplayed()
            checkEmailMaskSpinnerDefaultSelected()
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
    fun testSendMaskedEmailMessage() {
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
            navigateToCreateEmailMaskScreen()
        }
        provisionEmailMask {
            checkProvisionEmailMaskItemsDisplayed()
            selectRealEmailAddress(1)
            setLocalPart()
            waitForRootFocus()
            checkAvailableAddressMsg()
            clickOnCreateButton()
            waitForLoading()
        }
        emailAddresses {
            waitForLoading()
            waitForRecyclerView()
            checkEmailAddressesItemsDisplayed()
            navigateToEmailMessagesScreen(0)
        }
        emailMessages {
            navigateToSendEmailMessageScreen()
        }
        sendMaskedEmail {
            checkEmailMaskSpinnerDisplayed()
            selectEmailMask(1)
        }
        sendEmailMessage {
            setToField(address)
            setSubjectField("Espresso Masked Email Test")
            setContentBodyField("This is a test email sent from an email mask.")
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
    fun testSendEmailWithoutMask() {
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
        sendMaskedEmail {
            checkEmailMaskSpinnerDisplayed()
            checkEmailMaskSpinnerDefaultSelected()
        }
        sendEmailMessage {
            setToField(address)
            setSubjectField("Espresso No Mask Email Test")
            setContentBodyField("This is a test email sent without an email mask.")
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
}
