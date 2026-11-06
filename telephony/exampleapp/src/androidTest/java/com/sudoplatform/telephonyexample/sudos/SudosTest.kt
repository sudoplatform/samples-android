/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.sudos

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sudoplatform.sudoprofiles.EncryptionKey
import com.sudoplatform.telephonyexample.App
import com.sudoplatform.telephonyexample.MainActivity
import org.junit.After
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
