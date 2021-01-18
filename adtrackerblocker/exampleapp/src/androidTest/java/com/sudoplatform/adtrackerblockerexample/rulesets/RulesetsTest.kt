package com.sudoplatform.adtrackerblockerexample.rulesets

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.adtrackerblockerexample.MainActivity
import com.sudoplatform.adtrackerblockerexample.settings.settings
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

@RunWith(AndroidJUnit4::class)
@LargeTest
class RulesetsTest {
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
    fun testSelectRuleset() {
        rulesets {
            navigateFromLaunchToRulesets()
            try {
                // something went wrong, check for alert and sign out
                checkListFailedDialogDisplayed()
                clickOnCancel()
                clickOnSettings()
                settings {
                    clickOnSignOut()
                    navigateFromLaunchToRulesets()
                    waitForRecyclerView()
                }
            } catch (e: Exception) {
                // this is the expected behavior, click on a ruleset
                waitForRecyclerView()
            }
        }
    }
}
