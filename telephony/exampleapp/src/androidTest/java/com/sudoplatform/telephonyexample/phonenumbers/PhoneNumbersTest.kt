/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.phonenumbers

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sudoplatform.telephonyexample.MainActivity
import com.sudoplatform.telephonyexample.optionalArgument
import com.sudoplatform.telephonyexample.sudos.sudos
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test searching for phone numbers for a Sudo. Provisioning orders a real number from the
 * carrier, so the test stops at the confirmation and cancels it.
 *
 * The provider's inventory differs between instances, so the area code to search can be set
 * with the `AREA_CODE` instrumentation argument. When the instance offers no numbers there, the
 * steps that need one are skipped rather than failed.
 */
@RunWith(AndroidJUnit4::class)
class PhoneNumbersTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val areaCode = optionalArgument("AREA_CODE", "415")

    @After
    fun fini() {
        sudos(composeRule) {
            navigateBackToSudos()
            deregister()
        }
    }

    @Test
    fun testSearchForPhoneNumbers() {
        sudos(composeRule) {
            navigateFromLaunchToSudos()
            createSudo("Telephony")
            openSudo("Telephony")
        }
        phoneNumbers(composeRule) {
            waitForPhoneNumbers()
            waitForText("No phone numbers yet. Get one to get started.")
            navigateToProvision()

            search(areaCode = "")
            waitForText("Enter a three-digit area code")

            // 999 is not assigned anywhere in the North American Numbering Plan.
            search(areaCode = "999")
            waitForText("That area code isn't in use in United States")

            search(areaCode = areaCode)
            assumeTrue("No numbers offered in US-$areaCode on this instance", waitForSearchOutcome())
            selectFirstSearchResult()
            cancelDialog()

            pressBack()
            waitForPhoneNumbers()
            pressBack()
        }
        sudos(composeRule) { waitForSudos() }
    }
}
