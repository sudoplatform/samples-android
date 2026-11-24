/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.sudos

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.emailexample.App
import com.sudoplatform.emailexample.MainActivity
import com.sudoplatform.emailexample.register.login
import com.sudoplatform.emailexample.withNotificationPermission
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test the Sudos home screen and its account section with a TEST registration.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SudosTest {
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules = withNotificationPermission(composeRule)

    @After
    fun fini() {
        ApplicationProvider
            .getApplicationContext<App>()
            .getSharedPreferences(App.SIGN_IN_PREFERENCES, Context.MODE_PRIVATE)
            .edit { remove(App.ENFORCE_SINGLETON_KEY_PAIR_PREFERENCE) }
    }

    @Test
    fun testAccountSectionAndScreens() {
        sudos(composeRule) {
            navigateFromLaunchToSudos()
            checkSignedInWithTestRegistration()
            checkUserDetails()
            checkEntitlements()
            clickOnKeyBackup()
            checkKeyBackupNeedsFssoAndDismiss()
            deregister()
        }
    }

    @Test
    fun testDeleteSudoAsksForConfirmation() {
        createSudo(composeRule) {
            registerAndCreateSudoFlow()
        }
        sudos(composeRule) {
            pressBackUntilSudosAreDisplayed()
            checkSudoCount(1)
            swipeLeftAndCancelDelete(0)
            checkSudoCount(1)
            swipeLeftToDelete(0)
            checkSudoCount(0)
            deregister()
        }
    }

    @Test
    fun testSignInWithOneKeyForAllEmailCreatesTheSharedKey() {
        login(composeRule) {
            waitForRegisterScreen()
            clickOnEnforceSingletonKeyPair()
            clickOnRegister()
        }
        sudos(composeRule) {
            checkSudosItemsDisplayed()
            waitForEmailKeyBackupNotice()
        }
        val app = ApplicationProvider.getApplicationContext<App>()
        // Sign in created the key, so asking again finds it.
        assertFalse(runBlocking { app.sudoEmailClient.createSingletonKeysIfAbsent() }.keyPair.created)
        sudos(composeRule) {
            deregister()
        }
    }

    @Test
    fun testDeregister() {
        sudos(composeRule) {
            navigateFromLaunchToSudos()
            clickOnDeregister()
            // Cancel prompt
            clickOnNegativeDeregisterAlertDialogButton()
            checkSudosItemsDisplayed()
            deregister()
        }
        login(composeRule) {
            waitForRegisterScreen()
        }
    }
}
