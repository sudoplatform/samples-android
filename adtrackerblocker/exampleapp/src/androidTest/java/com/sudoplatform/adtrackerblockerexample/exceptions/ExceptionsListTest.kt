/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.adtrackerblockerexample.exceptions

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.adtrackerblockerexample.MainActivity
import com.sudoplatform.sudoadtrackerblocker.types.BlockingException
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

/**
 * Test the adding and removing of blocking exceptions.
 *
 * @since 2020-12-10
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ExceptionsListTest {

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
    fun testAddRemoveExceptions() {

        // Navigate to the Exceptions List
        exceptionsList {
            navigateFromLaunchToExceptionsList()
            waitForLoading()
            waitForRecyclerView()
            checkExceptionsItemsDisplayed()
        }

        // Add a host exception
        exceptionsList {
            clickOnAddExceptionButton()
            enterExceptionUrl("example.com")
            waitForLoading()
            checkEntryIsOfType(0, BlockingException.Type.HOST)
        }

        // Add a page exception
        exceptionsList {
            clickOnAddExceptionButton()
            enterExceptionUrl("example.com/about-us")
            waitForLoading()
            checkEntryIsOfType(1, BlockingException.Type.PAGE)
        }

        // Remove the exceptions just added
        exceptionsList {
            swipeLeftToDelete(1)
            waitForLoading()
            swipeLeftToDelete(0)
            waitForLoading()
        }

        // Add some more exceptions and the remove all
        exceptionsList {
            clickOnAddExceptionButton()
            enterExceptionUrl("https://anonyome.com")
            clickOnAddExceptionButton()
            enterExceptionUrl("https://mysudo.com/support")
            clickOnRemoveAll()
            waitForLoading()
        }
    }
}
