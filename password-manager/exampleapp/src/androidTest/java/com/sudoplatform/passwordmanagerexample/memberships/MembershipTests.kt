/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.passwordmanagerexample.memberships

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
 * Test the creating, deleting and editing of memberships flow.
 *
 * @since 2021-01-21
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MembershipTests {

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
    fun testCreateDeleteMembership() {

        // Create a membership
        vaultItems {
            navigateFromLaunchToVaultItems()
            waitForLoading(true)
            checkVaultItemsDisplayed()
            clickOnCreateMembershipButton()
        }

        createMembership()

        // Delete the membership just created
        vaultItems {
            waitForRecyclerView()
            swipeLeftToDelete(0)
            waitForLoading()
            clickOnPositiveAlertDialogButton()
        }

        // Create a membership
        vaultItems {
            waitForLoading(true)
            clickOnCreateMembershipButton()
        }

        createMembership()

        // Edit the membership
        vaultItems {
            waitForLoading(true)
            waitForRecyclerView()
            clickOn(0)
        }

        editMembership {
            checkEditMembershipItemsDisplayed()
            toggleFavorite()
            enterMembershipName("My First Membership 2")
            enterAddress("address 2")
            enterEmail("email 2")
            enterFirstName("first name 2")
            enterLastName("last name 2")
            enterMemberId("abc12322")
            setMemberSince(1990, 8, 25)
            setExpires(1990, 9, 25)
            enterPhone("0234567891")
            enterPassword("password2")
            enterWebsite("website2")
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

    fun createMembership() {
        createMembership {
            checkCreateMembershipItemsDisplayed()
            toggleFavorite()
            enterMembershipName("My First Membership")
            enterAddress("address")
            enterEmail("email")
            enterFirstName("first name")
            enterLastName("last name")
            enterMemberId("abc123")
            setMemberSince(1990, 7, 25)
            setExpires(1990, 8, 25)
            enterPhone("1234567890")
            enterPassword("password")
            enterWebsite("website")
            enterNotes("Foo bar baz")
            closeSoftKeyboard()
            selectRedColor()
            scrollToTop()
            clickOnSave()
            waitForLoading()
        }
    }
}
