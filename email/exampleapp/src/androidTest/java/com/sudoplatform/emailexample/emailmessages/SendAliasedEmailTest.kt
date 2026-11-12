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
import com.sudoplatform.emailexample.emailaliases.provisionEmailAlias
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
 * Test the send aliased email message flow.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SendAliasedEmailTest {
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
    fun testEmailAliasSpinnerIsDisplayed() {
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
        sendAliasedEmail {
            checkEmailAliasSpinnerDisplayed()
            checkEmailAliasSpinnerDefaultSelected()
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
    fun testSendAliasedEmailMessage() {
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
            navigateToCreateEmailAliasScreen()
        }
        provisionEmailAlias {
            checkProvisionEmailAliasItemsDisplayed()
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
        sendAliasedEmail {
            checkEmailAliasSpinnerDisplayed()
            selectEmailAlias(1)
        }
        sendEmailMessage {
            setToField(address)
            setSubjectField("Espresso Aliased Email Test")
            setContentBodyField("This is a test email sent from an email alias.")
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
    fun testSendEmailWithoutAlias() {
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
        sendAliasedEmail {
            checkEmailAliasSpinnerDisplayed()
            checkEmailAliasSpinnerDefaultSelected()
        }
        sendEmailMessage {
            setToField(address)
            setSubjectField("Espresso No Alias Email Test")
            setContentBodyField("This is a test email sent without an email alias.")
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
