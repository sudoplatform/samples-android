/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.passwordmanagerexample.vaults

import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.passwordmanagerexample.App
import com.sudoplatform.passwordmanagerexample.AppHolder
import com.sudoplatform.passwordmanagerexample.MainActivity
import com.sudoplatform.passwordmanagerexample.sudos.sudos
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

/**
 * Test the creating and deleting of vaults flow.
 *
 * @since 2020-09-25
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class VaultsTest {

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

    @Test
    fun testCreateVault() {
        vaults {
            navigateFromLaunchToVaults()

            waitForLoading()
            waitForRecyclerView()
            checkVaultsItemsDisplayed()

            clickOnCreateVaultButton()
            waitForLoading()
            waitForRecyclerView()

            checkNewVaultIsDisplayed()
            pressBack()
        }
        sudos {
            checkSudosItemsDisplayed()
        }
    }
}
