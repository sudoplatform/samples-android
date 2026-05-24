/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmasks

import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.emailexample.MainActivity
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.emailaddresses.emailAddresses
import com.sudoplatform.emailexample.emailaddresses.provisionEmailAddress
import com.sudoplatform.emailexample.mainmenu.mainMenu
import com.sudoplatform.emailexample.sudos.createSudo
import com.sudoplatform.emailexample.sudos.sudos
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

/**
 * Test the provision external email mask flow.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ProvisionExternalEmailMaskTest {
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
    fun testNavigateToExternalMaskFormFromSwitch() {
        createSudo {
            registerAndCreateSudoFlow()
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
            waitForRecyclerView()
            checkEmailAddressesItemsDisplayed()
            // Toggle switch to external position
            waitForViewToDisplay(withId(R.id.internalExternalSwitch), 10_000L)
            clickOnView(withId(R.id.internalExternalSwitch))
            // Tap create button which should now navigate to external form
            clickOnView(withId(R.id.createEmailMaskButton))
        }
        provisionExternalEmailMask {
            checkProvisionExternalEmailMaskItemsDisplayed()
            pressBack()
        }
        emailAddresses {
            checkEmailAddressesItemsDisplayed()
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
    fun testExternalFormDisplaysCorrectFields() {
        createSudo {
            registerAndCreateSudoFlow()
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
            waitForRecyclerView()
            checkEmailAddressesItemsDisplayed()
            // Toggle switch to external position
            waitForViewToDisplay(withId(R.id.internalExternalSwitch), 10_000L)
            clickOnView(withId(R.id.internalExternalSwitch))
            // Navigate to external form
            clickOnView(withId(R.id.createEmailMaskButton))
        }
        provisionExternalEmailMask {
            // Verify all expected form fields are displayed
            checkProvisionExternalEmailMaskItemsDisplayed()
            pressBack()
        }
        emailAddresses {
            checkEmailAddressesItemsDisplayed()
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
    fun testInvalidEmailFormatShowsErrorDialog() {
        createSudo {
            registerAndCreateSudoFlow()
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
            waitForRecyclerView()
            checkEmailAddressesItemsDisplayed()
            // Toggle switch to external position
            waitForViewToDisplay(withId(R.id.internalExternalSwitch), 10_000L)
            clickOnView(withId(R.id.internalExternalSwitch))
            // Navigate to external form
            clickOnView(withId(R.id.createEmailMaskButton))
        }
        provisionExternalEmailMask {
            checkProvisionExternalEmailMaskItemsDisplayed()
            // Enter an invalid email format
            enterExternalEmail("not-a-valid-email")
            setLocalPart()
            waitForRootFocus()
            checkAvailableAddressMsg()
            // Attempt to create - should show error dialog for invalid email format
            clickOnCreateButton()
            checkAlertDialogDisplayed()
            clickOnPositiveAlertDialogButton()
            pressBack()
        }
        emailAddresses {
            checkEmailAddressesItemsDisplayed()
            pressBack()
        }
        sudos {
            waitForLoading()
            waitForRecyclerView()
            checkSudosItemsDisplayed()
            pressBack()
        }
    }
}
