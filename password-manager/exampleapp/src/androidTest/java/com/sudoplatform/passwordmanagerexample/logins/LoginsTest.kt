/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.passwordmanagerexample.logins

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.passwordmanagerexample.App
import com.sudoplatform.passwordmanagerexample.AppHolder
import com.sudoplatform.passwordmanagerexample.MainActivity
import com.sudoplatform.passwordmanagerexample.passwordgenerator.passwordGenerator
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

/**
 * Test the creating, deleting and editing of logins flow.
 *
 * @since 2020-11-06
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@Ignore
class LoginsTest {

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

    @Test
    fun testCreateDeleteLogin() {

        // Create a login and enter the password manually
        logins {
            navigateFromLaunchToLogins()
            waitForLoading()
            waitForRecyclerView()
            checkLoginsItemsDisplayed()
            clickOnCreateLoginButton()
        }
        createLogin {
            checkCreateLoginItemsDisplayed()
            enterWebAddress("http://abc.net.au/news")
            enterUsername("slartibartfast")
            enterPassword("SuperSecret0!")
            // This is done last to get the view to scroll up and make the Save button clickable
            enterLoginName("ABC News")
            clickOnSave()
        }

        // Delete the login just created
        logins {
            waitForRecyclerView()
            swipeLeftToDelete(0)
            waitForLoading()
            clickOnPositiveAlertDialogButton()
        }

        // Create a login and generate the password
        logins {
            waitForLoading()
            waitForRecyclerView()
            clickOnCreateLoginButton()
        }
        createLogin {
            checkCreateLoginItemsDisplayed()
            enterWebAddress("http://abc.net.au/news")
            enterUsername("slartibartfast")
            clickOnGeneratePassword()
            passwordGenerator {
                clickOnOkButton()
            }
            checkPasswordIsNotBlank()
            // This is done last to get the view to scroll up and make the Save button clickable
            enterLoginName("ABC News")
            clickOnSave()
            waitForLoading()
        }

        // Edit the login
        logins {
            waitForRecyclerView()
            clickOn(0)
        }
        editLogin {
            checkEditLoginItemsDisplayed()
            checkDatesAreDisplayed()
            enterWebAddress("http://sbs.com.au/news")
            enterUsername("zaphodbeeblebrox")
            clickOnGeneratePassword()
            passwordGenerator {
                clickOnOkButton()
            }
            checkPasswordIsNotBlank()
            // This is done last to get the view to scroll up and make the Save button clickable
            enterLoginName("SBS News")
            clickOnSave()
            waitForLoading()
        }
        logins {
            waitForRecyclerView()
        }
    }
}
