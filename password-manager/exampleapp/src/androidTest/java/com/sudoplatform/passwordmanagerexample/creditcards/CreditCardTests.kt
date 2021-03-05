/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.passwordmanagerexample.creditcards

import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.passwordmanagerexample.App
import com.sudoplatform.passwordmanagerexample.AppHolder
import com.sudoplatform.passwordmanagerexample.MainActivity
import com.sudoplatform.passwordmanagerexample.sudos.sudos
import com.sudoplatform.passwordmanagerexample.vaultItems.vaultItems
import com.sudoplatform.passwordmanagerexample.vaults.vaults
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

/**
 * Test the creating, deleting and editing of credit cards flow.
 *
 * @since 2021-01-19
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@Ignore
class CreditCardTests {

    @get:Rule
    val activityRule = ActivityScenarioRule<MainActivity>(MainActivity::class.java)

    @Before
    fun setup() {
        Timber.plant(Timber.DebugTree())
        activityRule.scenario.onActivity { AppHolder.holdApp(it.application as App) }
        Thread.sleep(1000)
    }

    @After
    fun fini() {
        Timber.uprootAll()
    }

    @Test
    fun testCreateDeleteCreditCard() {

        // Create a creditCard
        vaultItems {
            navigateFromLaunchToVaultItems()
            waitForLoading(true)
            checkVaultItemsDisplayed()
            clickOnCreateCreditCardButton()
        }
        createCreditCard {
            checkCreateCreditCardItemsDisplayed()
            enterCardName("My First Card")
            enterCardHolder("Bugs Bunny")
            enterCardType("VISA")
            enterCardNumber("4111 1111 1111 1111")
            enterCardExpiration("01/22")
            enterSecurityCode("123")
            closeSoftKeyboard()
            scrollToTop()
            clickOnSave()
            waitForLoading()
        }

        // Delete the credit card just created
        vaultItems {
            waitForRecyclerView()
            swipeLeftToDelete(0)
            waitForLoading()
            clickOnPositiveAlertDialogButton()
        }

        // Create a card
        vaultItems {
            waitForLoading(true)
            clickOnCreateCreditCardButton()
        }
        createCreditCard {
            checkCreateCreditCardItemsDisplayed()
            enterCardName("My First Card")
            enterCardHolder("Bugs Bunny")
            enterCardType("VISA")
            enterCardNumber("4111 1111 1111 1111")
            enterCardExpiration("01/22")
            enterSecurityCode("123")
            closeSoftKeyboard()
            scrollToTop()
            clickOnSave()
            waitForLoading()
        }

        // Edit the card
        vaultItems {
            waitForLoading(true)
            waitForRecyclerView()
            clickOn(0)
        }
        editCreditCard {
            checkEditCreditCardItemsDisplayed()
            checkDatesAreDisplayed()
            enterCardName("Not My First Card")
            enterCardHolder("Babs Bunny")
            enterCardType("Mastercard")
            enterCardNumber("2111 1111 1111 1111")
            enterCardExpiration("01/24")
            closeSoftKeyboard()
            scrollToTop()
            clickOnSave()
            waitForLoading()
        }
        vaultItems {
            waitForLoading(true)
            waitForRecyclerView()
            pressBack()
        }

        // go back and delete new vault and sudo
        vaults {
            waitForRecyclerView()
            swipeLeftToDelete(0)
            waitForLoading()
            clickOnPositiveAlertDialogButton()
            pressBack()
        }
        sudos {
            waitForRecyclerView()
            swipeLeftToDelete(0)
            waitForLoading()
            clickOnPositiveAlertDialogButton()
        }
    }
}
