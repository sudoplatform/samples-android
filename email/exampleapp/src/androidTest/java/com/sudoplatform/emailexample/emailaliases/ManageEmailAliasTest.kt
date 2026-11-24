/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaliases

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.emailexample.MainActivity
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.emailaddresses.emailAddresses
import com.sudoplatform.emailexample.emailaddresses.provisionEmailAddress
import com.sudoplatform.emailexample.sudos.createSudo
import com.sudoplatform.emailexample.sudos.sudos
import com.sudoplatform.emailexample.withNotificationPermission
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test the email alias management flows (deprovision, enable, disable).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ManageEmailAliasTest {
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

    /**
     * Test: Provision an email alias, then swipe left and confirm to delete it.
     * Verifies the swipe-to-delete flow removes the item from the list.
     */
    @Test
    fun testSwipeToDeleteEmailAlias() {
        provisionEmailAliasFlow()
        emailAddresses(composeRule) {
            waitForEmailAddresses()
            checkEmailAliasesItemsDisplayed()
            swipeLeftToDeleteAlias(0)
            waitForActionToComplete()
            checkEmptyAliasesStateDisplayed()
            navigateBack()
        }
        sudos(composeRule) {
            checkSudosItemsDisplayed()
        }
    }

    /**
     * Test: Provision an email alias, tap it, verify action dialog shows,
     * then tap Disable to disable the alias.
     */
    @Test
    fun testTapItemAndDisableEmailAlias() {
        provisionEmailAliasFlow()
        emailAddresses(composeRule) {
            waitForEmailAddresses()
            checkEmailAliasesItemsDisplayed()
            clickOnAliasItem(0)
            checkActionDialogDisplayed()
            clickOnDisableButton()
            waitForActionToComplete()
            waitForAliasStatus(R.string.email_alias_status_disabled)
            checkEmailAliasesItemsDisplayed()
            navigateBack()
        }
        sudos(composeRule) {
            checkSudosItemsDisplayed()
        }
    }

    /**
     * Test: Provision an email alias, disable it, then tap it and enable it.
     * Verifies the tap → action dialog → enable flow.
     */
    @Test
    fun testTapItemAndEnableEmailAlias() {
        provisionEmailAliasFlow()
        emailAddresses(composeRule) {
            waitForEmailAddresses()
            checkEmailAliasesItemsDisplayed()
            // First disable the alias so we can test enabling it
            clickOnAliasItem(0)
            checkActionDialogDisplayed()
            clickOnDisableButton()
            waitForActionToComplete()
            waitForAliasStatus(R.string.email_alias_status_disabled)
            // Now enable the disabled alias
            clickOnAliasItem(0)
            checkActionDialogDisplayed()
            clickOnEnableButton()
            waitForActionToComplete()
            waitForAliasStatus(R.string.email_alias_status_enabled)
            checkEmailAliasesItemsDisplayed()
            navigateBack()
        }
        sudos(composeRule) {
            checkSudosItemsDisplayed()
        }
    }

    /**
     * Registers, creates a sudo, provisions an email address, and provisions an email alias.
     * Stays on the email addresses screen.
     */
    private fun provisionEmailAliasFlow() {
        createSudo(composeRule) {
            registerAndCreateSudoFlow()
        }
        emailAddresses(composeRule) {
            navigateToProvisionEmailAddressScreen()
        }
        provisionEmailAddress(composeRule) {
            setLocalPart()
            checkAvailableAddressMsg()
            clickOnCreateButton()
        }
        emailAddresses(composeRule) {
            waitForEmailAddresses()
            navigateToCreateEmailAliasScreen()
        }
        provisionEmailAlias(composeRule) {
            checkProvisionEmailAliasItemsDisplayed()
            selectRealEmailAddress(0)
            setLocalPart()
            checkAvailableAddressMsg()
            clickOnCreateButton()
        }
    }
}
