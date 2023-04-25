/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.fundingsources

import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.sudoplatform.virtualcardsexample.MainActivity
import com.sudoplatform.virtualcardsexample.mainmenu.mainMenu
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber

/**
 * Test the bank account funding source creation flow.
 */
class CreateBankAccountFundingSourceTest {

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
    fun testNavigationToCreateBankAccountFundingSourceScreen() {
        mainMenu {
            navigateFromLaunchToMainMenu()
        }
        mainMenu {
            navigateToFundingSourcesScreen()
        }
        fundingSources {
            waitForLoading()
            navigateToCreateFundingSourceMenuScreen()
        }
        createFundingSourceMenu {
            checkMenuItemsDisplayed()
            navigateToAddCheckoutBankAccountScreen()
        }
        createFundingSource {
            pressBack()
        }
        createFundingSourceMenu {
            pressBack()
        }
        fundingSources {
            pressBack()
        }
    }
}
