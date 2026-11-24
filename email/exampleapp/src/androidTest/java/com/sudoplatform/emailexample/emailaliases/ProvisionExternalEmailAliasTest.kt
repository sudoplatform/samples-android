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

/**
 * Test the provision external email alias flow.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ProvisionExternalEmailAliasTest {
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
    fun testNavigateToExternalAliasFormFromSwitch() {
        provisionEmailAddressAndOpenExternalAliasForm()
        provisionEmailAlias(composeRule) {
            checkProvisionExternalEmailAliasItemsDisplayed()
            navigateBack()
        }
        emailAddresses(composeRule) {
            checkEmailAddressesItemsDisplayed()
            navigateBack()
        }
        sudos(composeRule) {
            checkSudosItemsDisplayed()
        }
    }

    @Test
    fun testExternalFormDisplaysCorrectFields() {
        provisionEmailAddressAndOpenExternalAliasForm()
        provisionEmailAlias(composeRule) {
            // Verify all expected form fields are displayed
            checkProvisionExternalEmailAliasItemsDisplayed()
            navigateBack()
        }
        emailAddresses(composeRule) {
            checkEmailAddressesItemsDisplayed()
            navigateBack()
        }
        sudos(composeRule) {
            checkSudosItemsDisplayed()
        }
    }

    @Test
    fun testInvalidEmailFormatShowsErrorDialog() {
        provisionEmailAddressAndOpenExternalAliasForm()
        provisionEmailAlias(composeRule) {
            checkProvisionExternalEmailAliasItemsDisplayed()
            // Enter an invalid email format
            enterExternalEmail("not-a-valid-email")
            setLocalPart()
            checkAvailableAddressMsg()
            // Attempt to create - should show error dialog for invalid email format
            clickOnCreateButtonExpectingAlert()
            checkInvalidEmailFormatAlertDisplayed()
            clickOnPositiveAlertDialogButton()
            navigateBack()
        }
        emailAddresses(composeRule) {
            checkEmailAddressesItemsDisplayed()
            navigateBack()
        }
        sudos(composeRule) {
            checkSudosItemsDisplayed()
        }
    }

    /**
     * Registers, creates a sudo and provisions an email address, then chooses External and taps
     * the create button, which opens the alias form for an external alias.
     */
    private fun provisionEmailAddressAndOpenExternalAliasForm() {
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
        }
        externalEmailAliases(composeRule) {
            selectExternalAliases()
            clickOnCreateButton()
        }
    }
}
