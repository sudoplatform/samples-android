/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.passwordmanagerexample.driverslicenses

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
 * Test the creating, deleting and editing of driversLicenses flow.
 *
 * @since 2021-01-21
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class DriversLicenseTests {

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
    fun testCreateDeleteDriversLicense() {

        // Create a driversLicense
        vaultItems {
            navigateFromLaunchToVaultItems()
            waitForLoading(true)
            checkVaultItemsDisplayed()
            clickOnCreateDriversLicenseButton()
        }

        createDriversLicense()

        // Delete the driversLicense just created
        vaultItems {
            waitForRecyclerView()
            swipeLeftToDelete(0)
            waitForLoading()
            Thread.sleep(1000)
            clickOnPositiveAlertDialogButton()
        }

        // Create a driversLicense
        vaultItems {
            waitForLoading(true)
            clickOnCreateDriversLicenseButton()
        }

        createDriversLicense()

        // Edit the driversLicense
        vaultItems {
            waitForLoading(true)
            waitForRecyclerView()
            clickOn(0)
        }

        editDriversLicense {
            checkEditDriversLicenseItemsDisplayed()
            toggleFavorite()
            enterDriversLicenseName("My First DriversLicense 2")
            enterCountry("country 2")
            enterNumber("12342")
            enterFirstName("first 2")
            enterLastName("last 2")
            enterGender("gender 2")
            setDateOfBirth(1990, 8, 25)
            setDateOfIssue(1991, 9, 25)
            setExpires(1992, 10, 25)
            enterState("state 2")
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
            Thread.sleep(1000)
            clickOnPositiveAlertDialogButton()
            pressBack()
        }
        sudos {
            waitForRecyclerView()
            swipeLeftToDelete(0)
            waitForLoading()
            Thread.sleep(1000)
            clickOnPositiveAlertDialogButton()
        }
    }

    fun createDriversLicense() {
        createDriversLicense {
            checkCreateDriversLicenseItemsDisplayed()
            toggleFavorite()
            enterDriversLicenseName("My First Drivers License")
            enterCountry("country")
            enterNumber("1234")
            enterFirstName("first")
            enterLastName("last")
            enterGender("gender")
            setDateOfBirth(1990, 7, 25)
            setDateOfIssue(1991, 7, 25)
            setExpires(1992, 7, 25)
            enterState("state")
            enterNotes("Foo bar baz")
            closeSoftKeyboard()
            selectRedColor()
            scrollToTop()
            clickOnSave()
            waitForLoading()
        }
    }
}
