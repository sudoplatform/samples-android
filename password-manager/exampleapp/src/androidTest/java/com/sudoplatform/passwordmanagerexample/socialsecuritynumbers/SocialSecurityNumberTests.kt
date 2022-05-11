/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.passwordmanagerexample.socialsecuritynumbers

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
 * Test the creating, deleting and editing of socialSecurityNumbers flow.
 *
 * @since 2021-01-21
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SocialSecurityNumberTests {

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
        AppHolder.deleteSudos()
        Timber.uprootAll()
    }

    @Test
    fun testCreateDeleteSocialSecurityNumber() {

        // Create a socialSecurityNumber
        vaultItems {
            navigateFromLaunchToVaultItems()
            waitForLoading(true)
            checkVaultItemsDisplayed()
            clickOnCreateSSNButton()
        }

        createSocialSecurityNumber()

        // Delete the socialSecurityNumber just created
        vaultItems {
            waitForRecyclerView()
            swipeLeftToDelete(0)
            waitForLoading()
            clickOnPositiveAlertDialogButton()
        }

        // Create a socialSecurityNumber
        vaultItems {
            waitForLoading(true)
            clickOnCreateSSNButton()
        }

        createSocialSecurityNumber()

        // Edit the socialSecurityNumber
        vaultItems {
            waitForLoading(true)
            waitForRecyclerView()
            clickOn(0)
        }

        editSocialSecurityNumber {
            checkEditSocialSecurityNumberItemsDisplayed()
            toggleFavorite()
            enterSocialSecurityNumberName("My First SocialSecurityNumber 2")
            enterFirstName("first name 2")
            enterLastName("last name 2")
            enterNumber("123454321")
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

    fun createSocialSecurityNumber() {
        createSocialSecurityNumber {
            checkCreateSocialSecurityNumberItemsDisplayed()
            toggleFavorite()
            enterSocialSecurityNumberName("My First SocialSecurityNumber")
            enterFirstName("first name")
            enterLastName("last name")
            enterNumber("12345")
            enterNotes("Foo bar baz")
            closeSoftKeyboard()
            selectRedColor()
            scrollToTop()
            clickOnSave()
            waitForLoading()
        }
    }
}
