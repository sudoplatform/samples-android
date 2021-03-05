/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.passwordmanagerexample.unlock

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.passwordmanagerexample.App
import com.sudoplatform.passwordmanagerexample.AppHolder
import com.sudoplatform.passwordmanagerexample.MainActivity
import com.sudoplatform.passwordmanagerexample.settings.settings
import com.sudoplatform.passwordmanagerexample.sudos.sudos
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

/**
 * Test the error handling on the unlock vaults screen.
 *
 * @since 2020-10-14
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@Ignore
class UnlockVaultsTest {

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
    fun testLockVaults() {

        settings {
            navigateFromLaunchToSettings()
            clickOnDeregister()
            // Cancel prompt
            clickOnNegativeDeregisterAlertDialogButton()
            clickOnLockVaults()
        }
        unlock {
            waitForPasswordView(20_000L)
            checkEnterMasterPasswordItemsDisplayed(5_000L)
            enterMasterPassword()
        }
        sudos {
            checkSudosItemsDisplayed(20_000L)
        }
    }
}
