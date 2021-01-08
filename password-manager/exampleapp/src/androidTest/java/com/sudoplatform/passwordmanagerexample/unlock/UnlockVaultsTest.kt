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
import com.sudoplatform.passwordmanagerexample.register.login
import com.sudoplatform.passwordmanagerexample.settings.settings
import org.junit.After
import org.junit.Before
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
class UnlockVaultsTest {

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
    fun testMasterPasswordEntryChecks() {

        settings {
            navigateFromLaunchToSettings()
            clickOnDeregister()
            // Cancel prompt
            clickOnNegativeDeregisterAlertDialogButton()
            clickOnDeregister()
            // Perform reset
            clickOnPositiveDeregisterAlertDialogButton()
            waitForLoading()
        }

        login {
            waitForRegisterButton()
            clickOnRegister()
        }

        unlock {
            waitForPasswordView(10_000L)
            checkCreateMasterPasswordItemsDisplayed(timeout = 5_000L)

            // Test password set and no confirmation
            enterMasterPasswordAndEmptyConfirmation()
            checkMissingConfirmationAlertDialog()
            clickPositiveAlertDialogButton()

            // Test confirmation set and no password
            enterEmptyMasterPasswordAndConfirmation()
            checkMissingPasswordAlertDialog()
            clickPositiveAlertDialogButton()

            // Test password and confirmation do not match
            enterMismatchingMasterPasswordAndConfirmation()
            checkMismatchingConfirmationAlertDialog()
            clickPositiveAlertDialogButton()
        }
    }
}
