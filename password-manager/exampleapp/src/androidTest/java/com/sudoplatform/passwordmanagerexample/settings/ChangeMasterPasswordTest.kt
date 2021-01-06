/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.passwordmanagerexample.settings

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.passwordmanagerexample.App
import com.sudoplatform.passwordmanagerexample.AppHolder
import com.sudoplatform.passwordmanagerexample.MainActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

/**
 * Test that the master password can be changed and that the error handling operates correctly
 * on the change master password screen.
 *
 * @since 2020-10-28
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ChangeMasterPasswordTest {

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
    fun testChangeMasterPasswordEntryChecks() {

        settings {
            navigateFromLaunchToSettings()
            clickOnChangeMasterPassword()
        }

        changeMasterPassword {
            checkChangeMasterPasswordItemsDisplayed()

            // Test current password set and not the new password
            enterCurrentPasswordAndEmptyNewPassword()
            clickOnSave()
            checkMissingNewPasswordAlertDialog()
            clickOnPositiveAlertDialogButton()

            // Test new password set and not current password
            enterEmptyCurrentPasswordAndNewPassword()
            clickOnSave()
            checkMissingCurrentPasswordAlertDialog()
            clickOnPositiveAlertDialogButton()

            // Test current and new passwords are the same
            enterSameCurrentAndNewPasswords()
            clickOnSave()
            checkSamePasswordsAlertDialog()
            clickOnPositiveAlertDialogButton()
        }
    }

    @Test
    fun testChangeMasterPassword() {

        settings {
            navigateFromLaunchToSettings()
            clickOnChangeMasterPassword()
        }

        changeMasterPassword {
            checkChangeMasterPasswordItemsDisplayed()

            // Change the master password to something new
            enterCurrentPasswordAndNewPassword()
            clickOnSave()
            waitForLoading()
            clickOnPositiveAlertDialogButton()
        }

        settings {
            clickOnChangeMasterPassword()
        }

        changeMasterPassword {
            checkChangeMasterPasswordItemsDisplayed()

            // Change the master password back to its original value
            enterNewPasswordAndOldPassword()
            clickOnSave()
            waitForLoading()
            clickOnPositiveAlertDialogButton()
        }
    }
}
