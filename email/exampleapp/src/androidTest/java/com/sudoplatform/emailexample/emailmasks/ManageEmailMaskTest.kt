/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmasks

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.sudoplatform.emailexample.MainActivity
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
 * Test the email mask management flows (deprovision, enable, disable).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ManageEmailMaskTest {
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

    /**
     * Test: Provision an email mask, then swipe left to delete it.
     * Verifies the swipe-to-delete flow removes the item from the list.
     */
    @Test
    fun testSwipeToDeleteEmailMask() {
        provisionEmailMaskFlow()
        emailAddresses {
            waitForRecyclerView()
            checkEmailMasksItemsDisplayed()
            swipeLeftToDeleteMask(0)
            waitForActionToComplete()
            checkEmptyMasksStateDisplayed()
            pressBack()
        }
        sudos {
            waitForLoading()
            waitForRecyclerView()
            checkSudosItemsDisplayed()
            pressBack()
        }
    }

    /**
     * Test: Provision an email mask, tap it, verify action dialog shows,
     * then tap Disable to disable the mask.
     */
    @Test
    fun testTapItemAndDisableEmailMask() {
        provisionEmailMaskFlow()
        emailAddresses {
            waitForRecyclerView()
            checkEmailMasksItemsDisplayed()
            clickOnMaskItem(0)
            checkActionDialogDisplayed()
            clickOnDisableButton()
            waitForActionToComplete()
            waitForMasksRecyclerView()
            checkEmailMasksItemsDisplayed()
            pressBack()
        }
        sudos {
            waitForLoading()
            waitForRecyclerView()
            checkSudosItemsDisplayed()
            pressBack()
        }
    }

    /**
     * Test: Provision an email mask, disable it, then tap it and enable it.
     * Verifies the tap → action dialog → enable flow.
     */
    @Test
    fun testTapItemAndEnableEmailMask() {
        provisionEmailMaskFlow()
        emailAddresses {
            waitForRecyclerView()
            checkEmailMasksItemsDisplayed()
            // First disable the mask so we can test enabling it
            clickOnMaskItem(0)
            checkActionDialogDisplayed()
            clickOnDisableButton()
            waitForActionToComplete()
            // Wait for listEmailMasks() to refresh the list with updated status
            onView(isRoot()).perform(waitFor(3_000L))
            waitForMasksRecyclerView()
            checkEmailMasksItemsDisplayed()
            // Now enable the disabled mask
            clickOnMaskItem(0)
            checkActionDialogDisplayed()
            clickOnEnableButton()
            waitForActionToComplete()
            waitForMasksRecyclerView()
            checkEmailMasksItemsDisplayed()
            pressBack()
        }
        sudos {
            waitForLoading()
            waitForRecyclerView()
            checkSudosItemsDisplayed()
            pressBack()
        }
    }

    /**
     * Helper: Registers, creates a sudo, provisions an email address,
     * and provisions an email mask. Stays on the email addresses screen.
     */
    private fun provisionEmailMaskFlow() {
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
            navigateToCreateEmailMaskScreen()
        }
        provisionEmailMask {
            checkProvisionEmailMaskItemsDisplayed()
            selectRealEmailAddress(1)
            setLocalPart()
            waitForRootFocus()
            checkAvailableAddressMsg()
            clickOnCreateButton()
            waitForLoading()
        }
    }
}
