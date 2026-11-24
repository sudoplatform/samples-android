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
import com.sudoplatform.emailexample.emailaddresses.emailAddresses
import com.sudoplatform.emailexample.emailaddresses.provisionEmailAddress
import com.sudoplatform.emailexample.sudos.createSudo
import com.sudoplatform.emailexample.sudos.sudos
import com.sudoplatform.emailexample.withNotificationPermission
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Test the update email alias flows.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class UpdateEmailAliasTest {
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules = withNotificationPermission(composeRule)

    /** An expiry date that is always in the future: the 15th of next month. */
    private val expiryDate = LocalDate.now().plusMonths(1).withDayOfMonth(15)

    @After
    fun fini() {
        sudos(composeRule) {
            pressBackUntilSudosAreDisplayed()
            deregister()
        }
    }

    /**
     * Test: Provision an alias, tap item, tap "Update", verify the update dialog appears.
     */
    @Test
    fun testUpdateDialogDisplay() {
        provisionEmailAliasFlow()
        emailAddresses(composeRule) {
            waitForEmailAddresses()
            checkEmailAliasesItemsDisplayed()
            clickOnAliasItem(0)
            checkActionDialogDisplayed()
            clickOnUpdateButton()
            checkUpdateDialogDisplayed()
            clickOnCancelButton()
            navigateBack()
        }
        sudos(composeRule) {
            checkSudosItemsDisplayed()
        }
    }

    /**
     * Test: Open update dialog, tap Cancel, verify list unchanged.
     */
    @Test
    fun testCancelDismissesWithoutAction() {
        provisionEmailAliasFlow()
        emailAddresses(composeRule) {
            waitForEmailAddresses()
            checkEmailAliasesItemsDisplayed()
            clickOnAliasItem(0)
            checkActionDialogDisplayed()
            clickOnUpdateButton()
            checkUpdateDialogDisplayed()
            clickOnCancelButton()
            // Verify list is still displayed and unchanged
            checkEmailAliasesItemsDisplayed()
            checkDashDisplayedForMetadata()
            checkDashDisplayedForExpiresAt()
            navigateBack()
        }
        sudos(composeRule) {
            checkSudosItemsDisplayed()
        }
    }

    /**
     * Test: Enter valid JSON metadata, tap Save, verify the list refreshes with metadata
     * displayed.
     */
    @Test
    fun testSaveWithValidMetadata() {
        provisionEmailAliasFlow()
        emailAddresses(composeRule) {
            waitForEmailAddresses()
            checkEmailAliasesItemsDisplayed()
            clickOnAliasItem(0)
            checkActionDialogDisplayed()
            clickOnUpdateButton()
            checkUpdateDialogDisplayed()
            enterMetadataJson("{\"key1\":\"value1\"}")
            clickSaveButton()
            checkEmailAliasesItemsDisplayed()
            checkMetadataDisplayed("key1")
            navigateBack()
        }
        sudos(composeRule) {
            checkSudosItemsDisplayed()
        }
    }

    /**
     * Test: Leave metadata empty, tap Save, verify "-" shown for metadata.
     */
    @Test
    fun testSaveWithEmptyMetadata() {
        provisionEmailAliasFlow()
        emailAddresses(composeRule) {
            waitForEmailAddresses()
            checkEmailAliasesItemsDisplayed()
            clickOnAliasItem(0)
            checkActionDialogDisplayed()
            clickOnUpdateButton()
            checkUpdateDialogDisplayed()
            // Leave metadata empty (default state)
            clickSaveButton()
            checkEmailAliasesItemsDisplayed()
            checkDashDisplayedForMetadata()
            navigateBack()
        }
        sudos(composeRule) {
            checkSudosItemsDisplayed()
        }
    }

    /**
     * Test: Pick a date, tap Save, verify formatted date in list.
     */
    @Test
    fun testSaveWithExpirationSet() {
        provisionEmailAliasFlow()
        emailAddresses(composeRule) {
            waitForEmailAddresses()
            checkEmailAliasesItemsDisplayed()
            clickOnAliasItem(0)
            checkActionDialogDisplayed()
            clickOnUpdateButton()
            checkUpdateDialogDisplayed()
            clickPickDate()
            selectDateInNextMonth(expiryDate)
            clickSaveButton()
            checkEmailAliasesItemsDisplayed()
            checkExpiresAtDisplayed(expiryDate)
            navigateBack()
        }
        sudos(composeRule) {
            checkSudosItemsDisplayed()
        }
    }

    /**
     * Test: Clear expiration date using the Clear button, tap Save, verify "-" shown for expiration.
     */
    @Test
    fun testSaveWithExpirationCleared() {
        provisionEmailAliasFlow()
        emailAddresses(composeRule) {
            waitForEmailAddresses()
            checkEmailAliasesItemsDisplayed()
            // First set an expiration so we can clear it
            clickOnAliasItem(0)
            checkActionDialogDisplayed()
            clickOnUpdateButton()
            checkUpdateDialogDisplayed()
            clickPickDate()
            selectDateInNextMonth(expiryDate)
            clickSaveButton()
            checkEmailAliasesItemsDisplayed()
            checkExpiresAtDisplayed(expiryDate)
            // Now clear the expiration
            clickOnAliasItem(0)
            checkActionDialogDisplayed()
            clickOnUpdateButton()
            checkUpdateDialogDisplayed()
            clickClearDate()
            clickSaveButton()
            checkEmailAliasesItemsDisplayed()
            checkDashDisplayedForExpiresAt()
            navigateBack()
        }
        sudos(composeRule) {
            checkSudosItemsDisplayed()
        }
    }

    /**
     * Test: Verify "-" is shown when metadata is null/empty and expiresAt is null/epoch-zero.
     * A freshly provisioned alias should display dashes for both fields.
     */
    @Test
    fun testDashDisplayForAbsentValues() {
        provisionEmailAliasFlow()
        emailAddresses(composeRule) {
            waitForEmailAddresses()
            checkEmailAliasesItemsDisplayed()
            checkDashDisplayedForMetadata()
            checkDashDisplayedForExpiresAt()
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
