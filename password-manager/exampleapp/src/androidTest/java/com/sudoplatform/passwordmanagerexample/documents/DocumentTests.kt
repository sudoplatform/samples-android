/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.passwordmanagerexample.documents

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
 * Test the creating, deleting and editing of documents flow.
 *
 * @since 2021-01-21
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class DocumentTests {

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
    fun testCreateDeleteDocument() {

        // Create a document
        vaultItems {
            navigateFromLaunchToVaultItems()
            waitForLoading(true)
            checkVaultItemsDisplayed()
            clickOnCreateDocumentButton()
        }

        createDocument()

        // Delete the document just created
        vaultItems {
            waitForRecyclerView()
            swipeLeftToDelete(0)
            waitForLoading()
            clickOnPositiveAlertDialogButton()
        }

        // Create a document
        vaultItems {
            waitForLoading(true)
            clickOnCreateDocumentButton()
        }

        createDocument()

        // Edit the document
        vaultItems {
            waitForLoading(true)
            waitForRecyclerView()
            clickOn(0)
        }

        editDocument {
            checkEditDocumentItemsDisplayed()
            toggleFavorite()
            enterDocumentName("My First Document 2")
            enterContentType("content type 2")
            enterData("data 2")
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

    fun createDocument() {
        createDocument {
            checkCreateDocumentItemsDisplayed()
            toggleFavorite()
            enterDocumentName("My First Document")
            enterContentType("content type")
            enterData("data")
            enterNotes("Foo bar baz")
            closeSoftKeyboard()
            selectRedColor()
            scrollToTop()
            clickOnSave()
            waitForLoading()
        }
    }
}
