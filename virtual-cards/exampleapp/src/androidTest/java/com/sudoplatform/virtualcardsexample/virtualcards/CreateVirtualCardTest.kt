/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.virtualcards

import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.virtualcardsexample.MainActivity
import com.sudoplatform.virtualcardsexample.fundingsources.createFundingSource
import com.sudoplatform.virtualcardsexample.fundingsources.fundingSources
import com.sudoplatform.virtualcardsexample.identityverification.identityVerification
import com.sudoplatform.virtualcardsexample.mainmenu.mainMenu
import com.sudoplatform.virtualcardsexample.sudos.createSudo
import com.sudoplatform.virtualcardsexample.sudos.sudos
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

/**
 * Test the virtual card creation flow.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class CreateVirtualCardTest {

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
    fun testCreateAndCancelVirtualCard() {
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
            clickOnPositiveAlertDialogButton()
        }
        fundingSources {
            waitForRecyclerView()
            checkFundingSourcesItemsDisplayed()
            pressBack()
        }
        mainMenu {
            checkMainMenuItemsDisplayed()
            navigateToSudosScreen()
        }
        sudos {
            navigateToCreateSudoScreen()
        }
        createSudo {
            setSudoName("Shopping")
            clickOnCreateButton()
            waitForLoading()
            clickPositiveAlertDialogButton()
        }
        virtualCards {
            waitForLoading()
            checkVirtualCardsItemsDisplayed()
            navigateToCreateVirtualCardScreen()
        }
        createVirtualCard {
            waitForLoading()
            clickCreateButton()
            waitForLoading()
            clickPositiveAlertDialogButton()
        }
        virtualCardDetail {
            checkVirtualCardDetailItemsDisplayed()
            pressBack()
        }
        virtualCards {
            waitForRecyclerView()
            checkVirtualCardsItemsDisplayed()
            pressBack()
        }
        sudos {
            waitForLoading()
            waitForRecyclerView()
            checkSudosItemsDisplayed()
            navigateToVirtualCardsScreen(0)
        }
        virtualCards {
            waitForRecyclerView()
            navigateToVirtualCardDetailScreen(0)
        }
        virtualCardDetail {
            checkVirtualCardDetailItemsDisplayed()
            pressBack()
        }
        virtualCards {
            waitForRecyclerView()
            checkVirtualCardsItemsDisplayed()
            swipeLeftToCancel(0)
            waitForLoading()
            clickOnPositiveAlertDialogButton()
            pressBack()
        }
        sudos {
            waitForLoading()
            checkSudosItemsDisplayed()
            pressBack()
        }
    }

    @Test
    fun testCreateVirtualCardAndDeleteSudo() {
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
                checkIdentityVerificationItemsDisplayed()
            }
            pressBack()
        }
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
            clickOnPositiveAlertDialogButton()
        }
        fundingSources {
            waitForRecyclerView()
            checkFundingSourcesItemsDisplayed()
            pressBack()
        }
        mainMenu {
            checkMainMenuItemsDisplayed()
            navigateToSudosScreen()
        }
        sudos {
            navigateToCreateSudoScreen()
        }
        createSudo {
            setSudoName("Shopping")
            clickOnCreateButton()
            waitForLoading()
            clickPositiveAlertDialogButton()
        }
        virtualCards {
            waitForLoading()
            checkVirtualCardsItemsDisplayed()
            navigateToCreateVirtualCardScreen()
        }
        createVirtualCard {
            waitForLoading()
            clickCreateButton()
            waitForLoading()
            clickPositiveAlertDialogButton()
        }
        virtualCardDetail {
            checkVirtualCardDetailItemsDisplayed()
            pressBack()
        }
        virtualCards {
            waitForRecyclerView()
            checkVirtualCardsItemsDisplayed()
            pressBack()
        }
        sudos {
            waitForRecyclerView()
            checkSudosItemsDisplayed()
            swipeLeftToDelete(0)
            waitForLoading()
            clickOnPositiveAlertDialogButton()
            pressBack()
        }
        mainMenu {
            checkMainMenuItemsDisplayed()
            navigateToOrphanVirtualCardsScreen()
        }
        orphanVirtualCards {
            waitForRecyclerView()
            navigateToVirtualCardDetailScreen(0)
        }
        virtualCardDetail {
            checkVirtualCardDetailItemsDisplayed()
            pressBack()
        }
        orphanVirtualCards {
            pressBack()
        }
    }
}
