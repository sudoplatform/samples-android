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
 * Test the external email aliases feature: feature gating, Internal/External filtering, and
 * verification flow.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ExternalEmailAliasesTest {
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
     * Test: Verify the feature gate UI state is consistent with the configuration.
     * In the test environment, emailAliasesEnabled=true and externalEmailAliasesEnabled=true,
     * so the aliases section should be visible with the Internal/External buttons displayed.
     */
    @Test
    fun testFeatureGateEnabledState() {
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
            checkSwitchDisplayed()
            checkDisabledMessageNotDisplayed()
            navigateBack()
        }
        sudos(composeRule) {
            checkSudosItemsDisplayed()
        }
    }

    /**
     * Test: Choosing Internal or External filters the alias list to show the correct alias types.
     */
    @Test
    fun testSwitchFilteringAliases() {
        provisionInternalAliasFlow()
        externalEmailAliases(composeRule) {
            checkSwitchDisplayed()
            // Internal aliases show by default
            checkCreateButtonTextInternal()
            checkAliasListDisplayed()
            selectExternalAliases()
            // Create button should update to external text
            checkCreateButtonTextExternal()
            // Since we only provisioned an internal alias, external list should be empty
            checkEmptyAliasesStateDisplayed()
            selectInternalAliases()
            // Should show internal aliases again
            checkCreateButtonTextInternal()
            checkAliasesHaveItems(1)
            navigateBack()
        }
        sudos(composeRule) {
            checkSudosItemsDisplayed()
        }
    }

    /**
     * Test: Tapping the verify button on an unverified external alias opens the verification code
     * dialog, and confirming with a code triggers the verification completion call.
     */
    @Test
    fun testVerifyExternalAliasFlow() {
        provisionExternalAliasFlow()
        externalEmailAliases(composeRule) {
            selectExternalAliases()
            checkAliasesHaveItems(1)
            clickVerifyButtonOnItem(0)
            checkVerificationCodeDialogDisplayed()
            enterVerificationCode("123456")
            clickConfirmVerificationCode()
            waitForVerificationToComplete()
            navigateBack()
        }
        sudos(composeRule) {
            checkSudosItemsDisplayed()
        }
    }

    /**
     * Registers, creates a sudo, provisions an email address, and provisions an internal email
     * alias. Stays on the email addresses screen.
     */
    private fun provisionInternalAliasFlow() {
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

    /**
     * Registers, creates a sudo, provisions an email address, and provisions an external email
     * alias. Stays on the email addresses screen.
     */
    private fun provisionExternalAliasFlow() {
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
            checkCreateButtonTextExternal()
            clickOnCreateButton()
        }
        provisionEmailAlias(composeRule) {
            checkProvisionExternalEmailAliasItemsDisplayed()
            enterExternalEmail("testuser@gmail.com")
            setLocalPart()
            checkAvailableAddressMsg()
            clickOnCreateButton()
        }
    }
}
