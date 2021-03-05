package com.sudoplatform.passwordmanagerexample.passwordgenerator

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.passwordmanagerexample.App
import com.sudoplatform.passwordmanagerexample.AppHolder
import com.sudoplatform.passwordmanagerexample.MainActivity
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
class PasswordGeneratorTest {

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
    fun testSwitches() {
        passwordGenerator {
            navigateFromLaunchToPasswordGenerator()
            checkPasswordGeneratorItemsDisplayed()
            Thread.sleep(1000)
            checkPasswordDoesNotMatchTest()
            // test lowercase switch
            setTestPassword()
            checkPasswordMatchesTest()
            toggleLowercase()
            Thread.sleep(1000)
            checkPasswordDoesNotMatchTest()
            // test uppercase switch
            setTestPassword()
            checkPasswordMatchesTest()
            toggleUppercase()
            Thread.sleep(1000)
            checkPasswordDoesNotMatchTest()
            // test numbers switch
            setTestPassword()
            checkPasswordMatchesTest()
            toggleNumbers()
            Thread.sleep(1000)
            checkPasswordDoesNotMatchTest()
            // test symbols switch
            setTestPassword()
            checkPasswordMatchesTest()
            toggleSymbols()
            Thread.sleep(1000)
            checkPasswordDoesNotMatchTest()
        }
    }

    @Test
    fun testSetPasswordLength() {
        passwordGenerator {
            navigateFromLaunchToPasswordGenerator()
            checkPasswordGeneratorItemsDisplayed()
            Thread.sleep(1000)
            checkPasswordLength(20)
            setPasswordLengthField("10")
            checkPasswordLength(10)
        }
    }
}
