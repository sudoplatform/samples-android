/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaddresses

import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.emailexample.MainActivity
import com.sudoplatform.emailexample.mainmenu.mainMenu
import com.sudoplatform.emailexample.sudos.createSudo
import com.sudoplatform.emailexample.sudos.sudos
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import java.util.UUID

/**
 * Test the provision email address flow.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ProvisionEmailAddressTest {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setup() {
        Timber.plant(Timber.DebugTree())
    }

    @After
    fun fini() {
        mainMenu {
            pressBackUntilDeregisterToolbarButtonIsDisplayed()
            clickOnDeregister()
            clickOnPositiveDeregisterAlertDialogButton()
            waitForLoading()
        }
        Timber.uprootAll()
    }

    @Test
    fun testProvisionAndDeprovisionEmailAddress() {
        provisionEmailAddress {
            provisionEmailAddressFlow()
        }
        emailAddresses {
            waitForRecyclerView()
            checkEmailAddressesItemsDisplayed()
            swipeLeftToDelete(0)
            waitForLoading()
            pressBack()
        }
        sudos {
            waitForLoading()
            waitForRecyclerView()
            checkSudosItemsDisplayed()
            pressBack()
        }
    }

    @Test
    fun testAttemptToProvisionUnavailableEmailAddress() {
        val localPart = UUID.randomUUID().toString().substring(0, 8)
        createSudo {
            registerAndCreateSudoFlow()
        }
        emailAddresses {
            waitForLoading()
            checkEmailAddressesItemsDisplayed()
            navigateToProvisionEmailAddressScreen()
        }
        provisionEmailAddress {
            setLocalPart(localPart)
            waitForRootFocus()
            checkAvailableAddressMsg()
            clickOnCreateButton()
            waitForLoading()
        }
        emailAddresses {
            waitForRecyclerView()
            checkEmailAddressesItemsDisplayed()
            swipeLeftToDelete(0)
            waitForLoading()
            navigateToProvisionEmailAddressScreen()
        }
        provisionEmailAddress {
            setLocalPart(localPart)
            waitForRootFocus()
            checkUnavailableAddressMsg()
            pressBack()
        }
        emailAddresses {
            pressBack()
        }
        sudos {
            pressBack()
        }
    }

    @Test
    fun testProvisionEmailAddressAndDeleteSudo() {
        provisionEmailAddress {
            provisionEmailAddressFlow()
        }
        emailAddresses {
            waitForRecyclerView()
            checkEmailAddressesItemsDisplayed()
            pressBack()
        }
        sudos {
            waitForLoading()
            waitForRecyclerView()
            checkSudosItemsDisplayed()
            swipeLeftToDelete(0)
            waitForLoading()
            pressBack()
        }
    }

    @Test
    fun testProvisionMultipleEmailAddresses() {
        provisionEmailAddress {
            provisionEmailAddressFlow()
        }
        emailAddresses {
            waitForLoading()
            checkEmailAddressesItemsDisplayed()
            navigateToProvisionEmailAddressScreen()
        }
        provisionEmailAddress {
            setLocalPart()
            waitForRootFocus()
            checkAvailableAddressMsg()
            clickOnCreateButton()
            waitForLoading()
        }
        emailAddresses {
            pressBack()
        }
        sudos {
            pressBack()
        }
    }

    @Test
    fun testAttemptToProvisionInvalidEmailAddress() {
        createSudo {
            registerAndCreateSudoFlow()
        }
        emailAddresses {
            waitForLoading()
            checkEmailAddressesItemsDisplayed()
            navigateToProvisionEmailAddressScreen()
        }
        provisionEmailAddress {
            setLocalPart("_)(**")
            waitForRootFocus()
            checkInvalidAddressMsg()
            pressBack()
        }
        emailAddresses {
            pressBack()
        }
        sudos {
            pressBack()
        }
    }
}
