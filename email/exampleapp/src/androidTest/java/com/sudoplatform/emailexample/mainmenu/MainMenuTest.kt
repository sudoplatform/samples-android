/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.mainmenu

import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.emailexample.MainActivity
import com.sudoplatform.emailexample.sudos.sudos
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

/**
 * Test navigation from the main menu screen to all the other screens in the app.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MainMenuTest {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setup() {
        Timber.plant(Timber.DebugTree())
    }

    @After
    fun fini() {
        Timber.uprootAll()
    }

    @Test
    fun testNavigationToScreens() {
        mainMenu {
            navigateFromLaunchToMainMenu()
            navigateToSudosScreen()
        }
        sudos {
            pressBack()
        }
    }

    @Test
    fun testDeregister() {
        mainMenu {
            navigateFromLaunchToMainMenu()
            clickOnDeregister()
            // Cancel prompt
            clickOnNegativeDeregisterAlertDialogButton()
            clickOnDeregister()
            // Perform deregister
            clickOnPositiveDeregisterAlertDialogButton()
            waitForLoading()
        }
    }
}
