package com.sudoplatform.passwordmanagerexample.settings

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.passwordmanagerexample.App
import com.sudoplatform.passwordmanagerexample.AppHolder
import com.sudoplatform.passwordmanagerexample.MainActivity
import com.sudoplatform.passwordmanagerexample.passwordgenerator.passwordGenerator
import com.sudoplatform.passwordmanagerexample.sudos.sudos
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

@RunWith(AndroidJUnit4::class)
@LargeTest
@Ignore
class SettingsTest {

    @get:Rule
    val activityRule = ActivityScenarioRule<MainActivity>(MainActivity::class.java)

    @Before
    fun setup() {
        Timber.plant(Timber.DebugTree())
        activityRule.scenario.onActivity { AppHolder.holdApp(it.application as App) }
        Thread.sleep(1000)
    }

    @After
    fun fini() {
        Timber.uprootAll()
    }

    @Test
    fun testChangeMasterPassword() {
        settings {
            navigateFromLaunchToSettings()
            clickOnChangeMasterPassword()
        }
        changeMasterPassword {
            checkChangeMasterPasswordItemsDisplayed()
        }
    }

    @Test
    fun testViewSecretCode() {
        settings {
            navigateFromLaunchToSettings()
            clickOnSecretCode()
        }
        secretCode {
            checkSecretCodeItemsDisplayed()
        }
    }

    @Test
    fun testCopySecretCode() {
        secretCode {
            navigateFromLaunchToSecretCode()
            clickOnCopy()
            checkSecretCodeInClipboard()
        }
    }

    @Test
    fun testViewPasswordGenerator() {
        settings {
            navigateFromLaunchToSettings()
            clickOnPasswordGenerator()
        }
        passwordGenerator {
            checkPasswordGeneratorItemsDisplayed()
        }
    }

    @Test
    fun testViewEntitlements() {
        sudos {
            navigateFromLaunchToSudos()
            waitForLoading(true)
            checkSudosItemsDisplayed()
            clickCreateSudo()
            waitForLoading()
            waitForRecyclerView()
            clickOnSettings()
        }
        settings {
            clickOnViewEntitlements()
        }
        viewEntitlements {
            waitForLoading()
            waitForRecyclerView()
        }
    }
}
