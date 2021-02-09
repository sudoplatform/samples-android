/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.passwordmanagerexample.bankaccounts

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

/**
 * Test the creating, deleting and editing of bank accounts flow.
 *
 * @since 2021-01-21
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BankAccountTests {

    @get:Rule
    val activityRule = ActivityScenarioRule<MainActivity>(MainActivity::class.java)

    @Before
    fun setup() {
        Timber.plant(Timber.DebugTree())
        activityRule.scenario.onActivity { AppHolder.holdApp(it.application as App) }
    }

    @After
    fun fini() {
        Timber.uprootAll()
    }

    private fun pause() = runBlocking {
        delay(250)
    }

    @Test
    fun testCreateDeleteBankAccount() {

        // Create a bankAccount
        vaultItems {
            navigateFromLaunchToVaultItems()
            waitForLoading()
            waitForRecyclerView()
            checkVaultItemsDisplayed()
            clickOnCreateBankAccountButton()
        }
        createBankAccount {
            checkCreateBankAccountItemsDisplayed()
            enterAccountName("My First Bank Account")
            enterBankName("My Bank")
            enterAccountNumber("12345")
            enterRoutingNumber("54321")
            enterAccountType("Checking")
            closeSoftKeyboard()
            scrollToTop()
            clickOnSave()
            waitForLoading()
        }

        // Delete the bank account just created
        vaultItems {
            waitForRecyclerView()
            swipeLeftToDelete(0)
            waitForLoading()
            clickOnPositiveAlertDialogButton()
        }

        // Create a bank account
        vaultItems {
            waitForLoading()
            waitForRecyclerView()
            clickOnCreateBankAccountButton()
        }
        createBankAccount {
            checkCreateBankAccountItemsDisplayed()
            enterAccountName("My First Bank Account")
            enterBankName("My Bank")
            enterAccountNumber("12345")
            enterRoutingNumber("54321")
            enterAccountType("Checking")
            closeSoftKeyboard()
            scrollToTop()
            clickOnSave()
            waitForLoading()
        }

        // Edit the bank account
        vaultItems {
            waitForLoading()
            waitForRecyclerView()
            clickOn(0)
        }
        editBankAccount {
            checkEditBankAccountItemsDisplayed()
            enterAccountName("My Second Bank Account")
            enterBankName("My 2nd Bank")
            enterAccountNumber("54321")
            enterRoutingNumber("12345")
            enterAccountType("Savings")
            closeSoftKeyboard()
            scrollToTop()
            clickOnSave()
            waitForLoading()
        }
        vaultItems {
            waitForLoading()
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
