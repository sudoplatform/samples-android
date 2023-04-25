/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.fundingsources

import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.virtualcardsexample.MainActivity
import com.sudoplatform.virtualcardsexample.identityverification.identityVerification
import com.sudoplatform.virtualcardsexample.mainmenu.mainMenu
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

/**
 * Test the credit card funding source creation flow.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class CreateCreditCardFundingSourceTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

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
    fun testCreateAndCancelFundingSource() {
        mainMenu {
            navigateFromLaunchToMainMenu()
            navigateToIdentityVerificationScreen()
        }
        identityVerification {
            waitForLoading()
            checkIdentityVerificationItemsDisplayed()
            if (!isVerified()) {
                clickOnVerifyButton()
                waitForLoading()
                clickOnPositiveAlertDialogButton()
            }
            pressBack()
        }
        createCreditCardFundingSource()
        createFundingSource {
            clickOnPositiveAlertDialogButton()
        }
        fundingSources {
            waitForRecyclerView()
            checkFundingSourcesItemsDisplayed()
            cancelFundingSource()
            pressBack()
        }
    }

    @Test
    fun testAttemptToCreateMultipleFundingSources() {
        mainMenu {
            navigateFromLaunchToMainMenu()
            navigateToIdentityVerificationScreen()
        }
        identityVerification {
            waitForLoading()
            checkIdentityVerificationItemsDisplayed()
            if (!isVerified()) {
                clickOnVerifyButton()
                waitForLoading()
                clickOnPositiveAlertDialogButton()
            }
            pressBack()
        }
        createCreditCardFundingSource()
        createFundingSource {
            clickOnPositiveAlertDialogButton()
            pressBack()
        }
        createCreditCardFundingSource()
        createFundingSource {
            clickOnNegativeErrorAlertDialogButton()
            pressBack()
        }
        fundingSources {
            pressBack()
        }
    }

    @Test
    fun testAttemptToCreateFundingSourceBeforeVerifyingIdentity() {
        mainMenu {
            navigateFromLaunchToMainMenu()
        }
        createCreditCardFundingSource()
        createFundingSource {
            clickOnNegativeErrorAlertDialogButton()
            pressBack()
        }
        fundingSources {
            pressBack()
        }
    }

    private fun createCreditCardFundingSource() {
        mainMenu {
            navigateToFundingSourcesScreen()
        }
        fundingSources {
            waitForLoading()
            navigateToCreateFundingSourceMenuScreen()
        }
        createFundingSourceMenu {
            checkMenuItemsDisplayed()
            navigateToAddStripeCreditCardScreen()
        }
        createFundingSource {
            clickOnCreateButton()
            waitForLoading()
        }
    }

    private fun cancelFundingSource() {
        fundingSources {
            swipeLeftToCancel(0)
            waitForLoading()
            clickOnPositiveAlertDialogButton()
        }
    }
}
