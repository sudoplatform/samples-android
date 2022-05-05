/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.adtrackerblockerexample.explore

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.adtrackerblockerexample.MainActivity
import com.sudoplatform.sudoadtrackerblocker.types.Ruleset
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

private const val ALLOWED = "Allowed"
private const val BLOCKED = "Blocked"

/**
 * Test the explore fragment that allows the user to enter a URL and see if it is blocked.
 *
 * @since 2020-12-14
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
    fun testCheckingOfUrls() {

        // Navigate to the Explore fragment
        explore {
            navigateFromLaunchToExplore()
            checkExploreItemsDisplayed()
        }

        // Enable all the rulesets and check that a bad privacy URL is blocked
        explore {
            clickOnRuleset(Ruleset.Type.AD_BLOCKING)
            clickOnRuleset(Ruleset.Type.PRIVACY)
            clickOnRuleset(Ruleset.Type.SOCIAL)
            enterCheckedUrl("example.com/ads?name=shiny")
            clickOnCheckButton()
            waitForLoading(timeout = 40_000L)
            checkResultHasText(BLOCKED)
        }

        // Disable the privacy ruleset and check that the same URL is allowed
        explore {
            clickOnRuleset(Ruleset.Type.PRIVACY)
            enterCheckedUrl("google-analytics.com/collect")
            clickOnCheckButton()
            waitForLoading(timeout = 10_000L)
            checkResultHasText(ALLOWED)
        }

        // Check an advertising URL is blocked
        explore {
            enterCheckedUrl("gadsabz.com?ad=shinything")
            clickOnCheckButton()
            checkResultHasText(BLOCKED)
        }

        // Disable the advertising ruleset and check that the same URL is allowed
        explore {
            clickOnRuleset(Ruleset.Type.AD_BLOCKING)
            enterCheckedUrl("gadsabz.com?ad=shinything")
            clickOnCheckButton()
            waitForLoading(timeout = 10_000L)
            checkResultHasText(ALLOWED)
        }

        // Clear the URL and check that an error is displayed when Check is clicked
        explore {
            enterCheckedUrl("")
            clickOnCheckButton()
            clickOnPositiveAlertDialogButton()
        }
    }
}
