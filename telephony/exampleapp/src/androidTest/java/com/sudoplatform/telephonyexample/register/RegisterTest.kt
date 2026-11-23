/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.register

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sudoplatform.telephonyexample.MainActivity
import com.sudoplatform.telephonyexample.sudos.sudos
import com.sudoplatform.telephonyexample.withNotificationPermission
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test registering, which lands on the Sudos screen, and deregistering from there.
 */
@RunWith(AndroidJUnit4::class)
class RegisterTest {
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules = withNotificationPermission(composeRule)

    @Test
    fun testRegisterAndDeregister() {
        sudos(composeRule) {
            navigateFromLaunchToSudos()
            deregister()
        }
    }
}
