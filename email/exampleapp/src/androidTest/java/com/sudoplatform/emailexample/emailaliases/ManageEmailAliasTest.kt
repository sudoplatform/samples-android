/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaliases

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
 * Test the email alias management flows (deprovision, enable, disable).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ManageEmailAliasTest {
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
     * Test: Provision an email alias, then swipe left to delete it.
     * Verifies the swipe-to-delete flow removes the item from the list.
     */
    @Test
    fun testSwipeToDeleteEmailAlias() {
        provisionEmailAliasFlow()
        emailAddresses {
            waitForRecyclerView()
            checkEmailAliasesItemsDisplayed()
            swipeLeftToDeleteAlias(0)
            waitForActionToComplete()
            checkEmptyAliasesStateDisplayed()
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
     * Test: Provision an email alias, tap it, verify action dialog shows,
     * then tap Disable to disable the alias.
     */
    @Test
    fun testTapItemAndDisableEmailAlias() {
        provisionEmailAliasFlow()
        emailAddresses {
            waitForRecyclerView()
            checkEmailAliasesItemsDisplayed()
            clickOnAliasItem(0)
            checkActionDialogDisplayed()
            clickOnDisableButton()
            waitForActionToComplete()
            waitForAliasesRecyclerView()
            checkEmailAliasesItemsDisplayed()
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
     * Test: Provision an email alias, disable it, then tap it and enable it.
     * Verifies the tap → action dialog → enable flow.
     */
    @Test
    fun testTapItemAndEnableEmailAlias() {
        provisionEmailAliasFlow()
        emailAddresses {
            waitForRecyclerView()
            checkEmailAliasesItemsDisplayed()
            // First disable the alias so we can test enabling it
            clickOnAliasItem(0)
            checkActionDialogDisplayed()
            clickOnDisableButton()
            waitForActionToComplete()
            // Wait for listEmailAliases() to refresh the list with updated status
            onView(isRoot()).perform(waitFor(3_000L))
            waitForAliasesRecyclerView()
            checkEmailAliasesItemsDisplayed()
            // Now enable the disabled alias
            clickOnAliasItem(0)
            checkActionDialogDisplayed()
            clickOnEnableButton()
            waitForActionToComplete()
            waitForAliasesRecyclerView()
            checkEmailAliasesItemsDisplayed()
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
     * and provisions an email alias. Stays on the email addresses screen.
     */
    private fun provisionEmailAliasFlow() {
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
            navigateToCreateEmailAliasScreen()
        }
        provisionEmailAlias {
            checkProvisionEmailAliasItemsDisplayed()
            selectRealEmailAddress(1)
            setLocalPart()
            waitForRootFocus()
            checkAvailableAddressMsg()
            clickOnCreateButton()
            waitForLoading()
        }
    }
}
