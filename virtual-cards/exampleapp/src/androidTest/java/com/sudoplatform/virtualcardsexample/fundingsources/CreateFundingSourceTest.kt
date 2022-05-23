/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
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
 * Test the funding source creation flow.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class CreateFundingSourceTest {

    @get:Rule
    val activityRule = ActivityScenarioRule<MainActivity>(MainActivity::class.java)

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
        createFundingSource()
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
        createFundingSource()
        createFundingSource {
            clickOnPositiveAlertDialogButton()
            pressBack()
        }
        createFundingSource()
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
        createFundingSource()
        createFundingSource {
            clickOnNegativeErrorAlertDialogButton()
            pressBack()
        }
        fundingSources {
            pressBack()
        }
    }

    private fun createFundingSource() {
        mainMenu {
            navigateToFundingSourcesScreen()
        }
        fundingSources {
            waitForLoading()
            navigateToCreateFundingSourcesScreen()
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
