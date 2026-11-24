/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaddresses

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.emailexample.MainActivity
import com.sudoplatform.emailexample.sudos.createSudo
import com.sudoplatform.emailexample.sudos.sudos
import com.sudoplatform.emailexample.withNotificationPermission
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Test the provision email address flow.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ProvisionEmailAddressTest {
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules = withNotificationPermission(composeRule)

    @After
    fun fini() {
        sudos(composeRule) {
            pressBackUntilSudosAreDisplayed()
            deregister()
        }
    }

    @Test
    fun testProvisionAndDeprovisionEmailAddress() {
        provisionEmailAddress(composeRule) {
            provisionEmailAddressFlow()
        }
        emailAddresses(composeRule) {
            waitForEmailAddresses()
            swipeLeftToDelete(0)
            navigateBack()
        }
        sudos(composeRule) {
            checkSudosItemsDisplayed()
        }
    }

    @Test
    fun testAttemptToProvisionUnavailableEmailAddress() {
        val localPart = UUID.randomUUID().toString().substring(0, 8)
        createSudo(composeRule) {
            registerAndCreateSudoFlow()
        }
        emailAddresses(composeRule) {
            navigateToProvisionEmailAddressScreen()
        }
        provisionEmailAddress(composeRule) {
            setLocalPart(localPart)
            checkAvailableAddressMsg()
            clickOnCreateButton()
        }
        emailAddresses(composeRule) {
            waitForEmailAddresses()
            swipeLeftToDelete(0)
            navigateToProvisionEmailAddressScreen()
        }
        provisionEmailAddress(composeRule) {
            setLocalPart(localPart)
            checkUnavailableAddressMsg()
            navigateBack()
        }
        emailAddresses(composeRule) {
            checkEmailAddressesItemsDisplayed()
            navigateBack()
        }
    }

    @Test
    fun testProvisionEmailAddressAndDeleteSudo() {
        provisionEmailAddress(composeRule) {
            provisionEmailAddressFlow()
        }
        emailAddresses(composeRule) {
            waitForEmailAddresses()
            navigateBack()
        }
        sudos(composeRule) {
            checkSudoCount(1)
            swipeLeftToDelete(0)
            checkSudoCount(0)
        }
    }

    @Test
    fun testProvisionMultipleEmailAddresses() {
        provisionEmailAddress(composeRule) {
            provisionEmailAddressFlow()
        }
        emailAddresses(composeRule) {
            waitForEmailAddresses()
            navigateToProvisionEmailAddressScreen()
        }
        provisionEmailAddress(composeRule) {
            setLocalPart()
            checkAvailableAddressMsg()
            clickOnCreateButton()
        }
        emailAddresses(composeRule) {
            waitForEmailAddresses(2)
            navigateBack()
        }
    }

    @Test
    fun testAttemptToProvisionInvalidEmailAddress() {
        createSudo(composeRule) {
            registerAndCreateSudoFlow()
        }
        emailAddresses(composeRule) {
            navigateToProvisionEmailAddressScreen()
        }
        provisionEmailAddress(composeRule) {
            setLocalPart("_)(**")
            checkInvalidAddressMsg()
            navigateBack()
        }
        emailAddresses(composeRule) {
            checkEmailAddressesItemsDisplayed()
            navigateBack()
        }
    }
}
