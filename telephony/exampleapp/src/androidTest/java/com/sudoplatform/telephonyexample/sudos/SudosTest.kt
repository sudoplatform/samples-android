/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.sudos

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sudoplatform.sudoprofiles.EncryptionKey
import com.sudoplatform.telephonyexample.App
import com.sudoplatform.telephonyexample.MainActivity
import com.sudoplatform.telephonyexample.register.register
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Base64

/**
 * Test the Sudo create, edit and delete flows.
 */
@RunWith(AndroidJUnit4::class)
class SudosTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @After
    fun fini() {
        sudos(composeRule) {
            navigateBackToSudos()
            deregister()
        }
        ApplicationProvider
            .getApplicationContext<App>()
            .getSharedPreferences(App.SIGN_IN_PREFERENCES, Context.MODE_PRIVATE)
            .edit { remove(App.ENFORCE_SINGLETON_KEY_PAIR_PREFERENCE) }
    }

    @Test
    fun testCreateEditAndDeleteSudo() {
        sudos(composeRule) {
            navigateFromLaunchToSudos()
            createSudo("Work")
            waitForSudo("Work")
            editSudo("Work", "Personal")
            waitForSudo("Personal")
            waitForSudoToDisappear("Work")
            deleteSudo("Personal")
        }
    }

    @Test
    fun testEntitlements() {
        sudos(composeRule) {
            navigateFromLaunchToSudos()
            checkEntitlements()
        }
    }

    @Test
    fun testUserDetails() {
        sudos(composeRule) {
            navigateFromLaunchToSudos()
            checkUserDetails()
        }
    }

    @Test
    fun testKeyBackupNeedsFederatedSignIn() {
        sudos(composeRule) {
            navigateFromLaunchToSudos()
            checkKeyBackupNeedsFederatedSignIn()
        }
    }

    @Test
    fun testMissingSudoKeysShowImportNotice() {
        sudos(composeRule) {
            navigateFromLaunchToSudos()
            createSudo("Work")
            waitForSudo("Work")
            replaceSudoKeys()
            refreshByOpeningSudo("Work")
            waitForKeyImportNotice()
        }
    }

    @Test
    fun testSignInWithOneKeyForAllNumbersCreatesTheSharedKey() {
        register(composeRule) {
            waitForRegisterScreen()
            clickOnEnforceSingletonKeyPair()
            clickOnRegister()
        }
        sudos(composeRule) {
            waitForSudos()
            waitForTelephonyKeyBackupNotice()
        }
        val app = ApplicationProvider.getApplicationContext<App>()
        // Sign in created the key, so asking again finds it.
        assertFalse(runBlocking { app.sudoTelephonyClient.createSingletonKeysIfAbsent() }.keyPair.created)
    }

    /**
     * Stands in for a Sudo created on another device: this device's only Sudo key becomes one
     * that didn't encrypt it.
     */
    private fun replaceSudoKeys() {
        val app = ApplicationProvider.getApplicationContext<App>()
        val otherKey = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })
        app.sudoProfilesClient.importEncryptionKeys(
            listOf(EncryptionKey("OTHER-DEVICE-KEY", "ss", otherKey, "AES/CBC/PKCS7Padding", 1)),
            "OTHER-DEVICE-KEY",
        )
    }
}
