/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmasks

import androidx.test.espresso.Espresso.pressBack
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

/**
 * Test the external email masks feature: feature gating, switch filtering,
 * and verification flow.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ExternalEmailMasksTest {
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

    /**
     * Test: Verify the feature gate UI state is consistent with the configuration.
     * In the test environment, emailMasksEnabled=true and externalEmailMasksEnabled=true,
     * so the masks section should be visible with the switch displayed.
     */
    @Test
    fun testFeatureGateEnabledState() {
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
        }
        externalEmailMasks {
            // In the test environment, email masks are enabled.
            // Verify the masks section is visible with the switch displayed.
            checkSwitchDisplayed()
            checkDisabledMessageNotDisplayed()
        }
        pressBack()
        sudos {
            waitForLoading()
            waitForRecyclerView()
            checkSudosItemsDisplayed()
            pressBack()
        }
    }

    /**
     * Test: Toggling the Internal/External switch filters the mask list
     * to show the correct mask types.
     */
    @Test
    fun testSwitchFilteringMasks() {
        provisionInternalMaskFlow()
        externalEmailMasks {
            // Switch should be visible if externalEmailMasksEnabled=true
            checkSwitchDisplayed()
            // Default state (off) shows internal masks
            checkCreateButtonTextInternal()
            checkMaskListDisplayed()
            // Toggle to external
            toggleSwitch()
            // Create button should update to external text
            checkCreateButtonTextExternal()
            // Since we only provisioned an internal mask, external list should be empty
            checkEmptyMasksStateDisplayed()
            // Toggle back to internal
            toggleSwitch()
            // Should show internal masks again
            checkCreateButtonTextInternal()
            checkMaskListDisplayed()
            checkMasksRecyclerViewHasItems(1)
        }
        pressBack()
        sudos {
            waitForLoading()
            waitForRecyclerView()
            checkSudosItemsDisplayed()
            pressBack()
        }
    }

    /**
     * Test: Tapping the verify button on an unverified external mask opens
     * the verification code dialog, and confirming with a code triggers
     * the verification completion call.
     */
    @Test
    fun testVerifyExternalMaskFlow() {
        provisionExternalMaskFlow()
        externalEmailMasks {
            // Switch to external view
            checkSwitchDisplayed()
            toggleSwitch()
            // Wait for the external masks to appear
            waitForMasksRecyclerView()
            checkMasksRecyclerViewHasItems(1)
            // Tap verify button on the first external mask
            clickVerifyButtonOnItem(0)
            // Verification code dialog should appear
            checkVerificationCodeDialogDisplayed()
            // Enter a verification code and confirm
            enterVerificationCode("123456")
            clickConfirmVerificationCode()
            // Wait for verification to complete
            waitForVerificationToComplete()
        }
        pressBack()
        sudos {
            waitForLoading()
            waitForRecyclerView()
            checkSudosItemsDisplayed()
            pressBack()
        }
    }

    /**
     * Helper: Registers, creates a sudo, provisions an email address,
     * and provisions an internal email mask. Stays on the email addresses screen.
     */
    private fun provisionInternalMaskFlow() {
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
    }

    /**
     * Helper: Registers, creates a sudo, provisions an email address,
     * and provisions an external email mask via the external mask form.
     * Stays on the email addresses screen.
     */
    private fun provisionExternalMaskFlow() {
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
        }
        externalEmailMasks {
            // Toggle to external to access the external create button
            checkSwitchDisplayed()
            toggleSwitch()
            checkCreateButtonTextExternal()
            clickOnCreateButton()
        }
        provisionExternalEmailMask {
            checkProvisionExternalEmailMaskItemsDisplayed()
            enterExternalEmail("testuser@gmail.com")
            setLocalPart()
            waitForRootFocus()
            checkAvailableAddressMsg()
            clickOnCreateButton()
            waitForLoading()
        }
    }
}
