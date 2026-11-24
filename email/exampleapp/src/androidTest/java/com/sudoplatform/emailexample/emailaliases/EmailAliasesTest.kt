/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaliases

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.emailexample.MainActivity
import com.sudoplatform.emailexample.emailaddresses.emailAddresses
import com.sudoplatform.emailexample.emailaddresses.provisionEmailAddress
import com.sudoplatform.emailexample.sudos.createSudo
import com.sudoplatform.emailexample.sudos.sudos
import com.sudoplatform.emailexample.withNotificationPermission
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test the email aliases flow.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class EmailAliasesTest {
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
    fun testNavigateToProvisionEmailAliasScreen() {
        createSudo(composeRule) {
            registerAndCreateSudoFlow()
        }
        emailAddresses(composeRule) {
            navigateToProvisionEmailAddressScreen()
        }
        provisionEmailAddress(composeRule) {
            setLocalPart()
            checkAvailableAddressMsg()
            clickOnCreateButton()
        }
        emailAddresses(composeRule) {
            waitForEmailAddresses()
            navigateToCreateEmailAliasScreen()
        }
        provisionEmailAlias(composeRule) {
            checkProvisionEmailAliasItemsDisplayed()
            navigateBack()
        }
        emailAddresses(composeRule) {
            checkEmailAddressesItemsDisplayed()
            navigateBack()
        }
        sudos(composeRule) {
            checkSudosItemsDisplayed()
        }
    }

    @Test
    fun testProvisionEmailAlias() {
        createSudo(composeRule) {
            registerAndCreateSudoFlow()
        }
        emailAddresses(composeRule) {
            navigateToProvisionEmailAddressScreen()
        }
        provisionEmailAddress(composeRule) {
            setLocalPart()
            checkAvailableAddressMsg()
            clickOnCreateButton()
        }
        emailAddresses(composeRule) {
            waitForEmailAddresses()
            navigateToCreateEmailAliasScreen()
        }
        provisionEmailAlias(composeRule) {
            checkProvisionEmailAliasItemsDisplayed()
            selectRealEmailAddress(0)
            setLocalPart()
            checkAvailableAddressMsg()
            clickOnCreateButton()
        }
        emailAddresses(composeRule) {
            waitForEmailAddresses()
            checkEmailAliasesItemsDisplayed()
            navigateBack()
        }
        sudos(composeRule) {
            checkSudosItemsDisplayed()
        }
    }
}
