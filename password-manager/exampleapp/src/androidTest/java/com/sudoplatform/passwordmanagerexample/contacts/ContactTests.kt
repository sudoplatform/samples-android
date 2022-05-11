/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.passwordmanagerexample.contacts

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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

/**
 * Test the creating, deleting and editing of contacts flow.
 *
 * @since 2021-01-21
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ContactTests {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setup() {
        Timber.plant(Timber.DebugTree())
        activityRule.scenario.onActivity { AppHolder.holdApp(it.application as App) }
        Thread.sleep(1000)
    }

    @After
    fun fini() {
        AppHolder.deleteSudos()
        Timber.uprootAll()
    }

    @Test
    fun testCreateDeleteContact() {

        // Create a contact
        vaultItems {
            navigateFromLaunchToVaultItems()
            waitForLoading(true)
            checkVaultItemsDisplayed()
            clickOnCreateContactButton()
        }

        createContact()

        // Delete the contact just created
        vaultItems {
            waitForRecyclerView()
            swipeLeftToDelete(0)
            waitForLoading()
            clickOnPositiveAlertDialogButton()
        }

        // Create a contact
        vaultItems {
            waitForLoading(true)
            clickOnCreateContactButton()
        }

        createContact()

        // Edit the contact
        vaultItems {
            waitForLoading(true)
            waitForRecyclerView()
            clickOn(0)
        }

        editContact {
            checkEditContactItemsDisplayed()
            toggleFavorite()
            enterContactName("My First Contact 2")
            enterAddress("address 2")
            enterCompany("company 2")
            enterEmail("email 2")
            enterFirstName("first 2")
            enterLastName("last 2")
            enterGender("gender 2")
            setDateOfBirth(1990, 8, 10)
            enterState("state 2")
            enterWebsite("website 2")
            enterPhone("1234567890")
            enterOtherPhone("0123456789")
            enterNotes("Foo bar baz 2")
            closeSoftKeyboard()
            selectYellowColor()
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

    fun createContact() {
        createContact {
            checkCreateContactItemsDisplayed()
            toggleFavorite()
            enterContactName("My First Contact")
            enterAddress("address")
            enterCompany("company")
            enterEmail("email")
            enterFirstName("first")
            enterLastName("last")
            enterGender("gender")
            setDateOfBirth(1990, 7, 25)
            enterState("state")
            enterWebsite("website")
            enterPhone("0123456789")
            enterOtherPhone("9123456780")
            enterNotes("Foo bar baz")
            closeSoftKeyboard()
            selectRedColor()
            scrollToTop()
            clickOnSave()
            waitForLoading()
        }
    }
}
