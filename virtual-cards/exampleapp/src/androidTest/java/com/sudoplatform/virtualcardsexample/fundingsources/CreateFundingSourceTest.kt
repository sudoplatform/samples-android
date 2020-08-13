/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.fundingsources

import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
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
 *
 * @since 2020-07-29
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class CreateFundingSourceTest {

    @get:Rule
    val activityRule = ActivityTestRule<MainActivity>(MainActivity::class.java)

    @Before
    fun setup() {
        Timber.plant(Timber.DebugTree())
    }

    @After
    fun fini() {
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
        mainMenu {
            checkMainMenuItemsDisplayed()
            clickOnDeregister()
            clickOnPositiveDeregisterAlertDialogButton()
            waitForLoading()
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
        mainMenu {
            checkMainMenuItemsDisplayed()
            clickOnDeregister()
            clickOnPositiveDeregisterAlertDialogButton()
            waitForLoading()
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
        mainMenu {
            checkMainMenuItemsDisplayed()
            clickOnDeregister()
            clickOnPositiveDeregisterAlertDialogButton()
            waitForLoading()
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
