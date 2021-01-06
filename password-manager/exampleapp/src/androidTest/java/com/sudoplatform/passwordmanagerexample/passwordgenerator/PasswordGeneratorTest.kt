package com.sudoplatform.passwordmanagerexample.passwordgenerator

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.passwordmanagerexample.App
import com.sudoplatform.passwordmanagerexample.AppHolder
import com.sudoplatform.passwordmanagerexample.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

@RunWith(AndroidJUnit4::class)
@LargeTest
class PasswordGeneratorTest {

    @get:Rule
    val activityRule = ActivityScenarioRule<MainActivity>(MainActivity::class.java)

    @Before
    fun setup() {
        Timber.plant(Timber.DebugTree())
        activityRule.scenario.onActivity { AppHolder.holdApp(it.application as App) }
    }

    @After
    fun fini() {
        Timber.uprootAll()
    }

    private fun pause() = runBlocking<Unit> {
        delay(250L)
    }

    @Test
    fun testSwitches() {
        passwordGenerator {
            navigateFromLaunchToPasswordGenerator()
            checkPasswordGeneratorItemsDisplayed()
            pause()
            checkPasswordDoesNotMatchTest()
            // test lowercase switch
            setTestPassword()
            checkPasswordMatchesTest()
            toggleLowercase()
            pause()
            checkPasswordDoesNotMatchTest()
            // test uppercase switch
            setTestPassword()
            checkPasswordMatchesTest()
            toggleUppercase()
            pause()
            checkPasswordDoesNotMatchTest()
            // test numbers switch
            setTestPassword()
            checkPasswordMatchesTest()
            toggleNumbers()
            pause()
            checkPasswordDoesNotMatchTest()
            // test symbols switch
            setTestPassword()
            checkPasswordMatchesTest()
            toggleSymbols()
            pause()
            checkPasswordDoesNotMatchTest()
        }
    }

    @Test
    fun testSetPasswordLength() {
        passwordGenerator {
            navigateFromLaunchToPasswordGenerator()
            checkPasswordLength(20)
            setPasswordLengthField("10")
            checkPasswordLength(10)
        }
    }
}
