/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.register

import android.content.Context
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.core.content.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sudoplatform.emailexample.App
import com.sudoplatform.emailexample.MainActivity
import com.sudoplatform.emailexample.withNotificationPermission
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test the register screen's "Single key for all email" toggle keeps its state when the app is
 * launched again. Doesn't sign in, so it needs no backend, but expects to start signed out, as the
 * other tests leave it.
 */
@RunWith(AndroidJUnit4::class)
class RegisterKeyModeTest {
    val composeRule = createEmptyComposeRule()

    @get:Rule
    val rules = withNotificationPermission(composeRule)

    private val preferences
        get() =
            ApplicationProvider
                .getApplicationContext<Context>()
                .getSharedPreferences(App.SIGN_IN_PREFERENCES, Context.MODE_PRIVATE)

    @Before
    fun init() {
        preferences.edit { remove(App.ENFORCE_SINGLETON_KEY_PAIR_PREFERENCE) }
    }

    @After
    fun fini() {
        preferences.edit { remove(App.ENFORCE_SINGLETON_KEY_PAIR_PREFERENCE) }
    }

    @Test
    fun testEnforceSingletonKeyPairIsPersisted() {
        ActivityScenario.launch(MainActivity::class.java).use {
            login(composeRule) {
                waitForRegisterScreen()
                assertEnforceSingletonKeyPair(isOn = false)
                clickOnEnforceSingletonKeyPair()
                assertEnforceSingletonKeyPair(isOn = true)
            }
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            login(composeRule) {
                waitForRegisterScreen()
                assertEnforceSingletonKeyPair(isOn = true)
                clickOnEnforceSingletonKeyPair()
                assertEnforceSingletonKeyPair(isOn = false)
            }
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            login(composeRule) {
                waitForRegisterScreen()
                assertEnforceSingletonKeyPair(isOn = false)
            }
        }
    }
}
