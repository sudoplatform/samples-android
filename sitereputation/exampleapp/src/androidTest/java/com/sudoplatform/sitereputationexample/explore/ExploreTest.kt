/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-Licensee-Identifier: Apache-2.0
 */

package com.sudoplatform.sitereputationexample.explore

import androidx.test.espresso.Espresso
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.sitereputationexample.MainActivity
import com.sudoplatform.sitereputationexample.settings.settings
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

private const val MALICIOUS = "Malicious"
private const val SAFE = "Safe"
private const val UPDATE_REQUIRED = "Update Required"

/**
 * Test the explore fragment that allows the user to enter a URL and see if it is blocked.
 *
 * @since 2021-01-06
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ExploreTest {

    @get:Rule
    val activityRule = ActivityScenarioRule<MainActivity>(MainActivity::class.java)

    @Before
    fun setup() {
        Timber.plant(Timber.DebugTree())
    }

    @After
    fun fini() {
        Timber.uprootAll()
    }

    @Test
    fun testUpdate() {

        // Navigate to the Explore fragment
        explore {
            navigateFromLaunchToExplore()
            checkExploreItemsDisplayed()
            clickOnSettings()
        }

        settings {
            checkItemsDisplayed()
            clickOnClearStorage()
            clickOnPositiveAlertDialogButton()
            Espresso.pressBack()
        }

        // Check call to update changes the last updated text
        explore {
            checkUpdatedHasText(UPDATE_REQUIRED)
            clickOnUpdateButton()
            waitForLoading()
            checkUpdatedDoesNotHaveText(UPDATE_REQUIRED)
        }
    }

    @Test
    fun testCheckingOfUrls() {

        // Navigate to the Explore fragment
        explore {
            navigateFromLaunchToExplore()
            checkExploreItemsDisplayed()
            clickOnUpdateButton()
            waitForLoading()
            checkUpdatedDoesNotHaveText(UPDATE_REQUIRED)
        }

        // Check top suggested URL is MALICIOUS
        explore {
            clickOnSpinner()
            clickOnSpinnerItemWithText("aboveandbelow.com.au")
            clickOnCheckButton()
            waitForLoading()
            checkResultHasText(MALICIOUS)
        }

        // Check an entered malicious URL is marked as MALICIOUS
        explore {
            enterCheckedUrl("wildnights.co.uk")
            clickOnCheckButton()
            waitForLoading()
            checkResultHasText(MALICIOUS)
        }

        // Check a safe URL is marked as SAFE
        explore {
            enterCheckedUrl("anonyome.com/about.js")
            clickOnCheckButton()
            waitForLoading()
            checkResultHasText(SAFE)
        }

        // Clear the URL and check that an error is displayed when Check is clicked
        explore {
            enterCheckedUrl("")
            clickOnCheckButton()
            waitForLoading()
            clickOnPositiveAlertDialogButton()
        }
    }
}
