/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaliases

import androidx.test.espresso.Espresso.pressBack
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
 * Test the update email alias flows.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class UpdateEmailAliasTest {
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
     * Test: Provision a alias, tap item, tap "Update", verify the update dialog appears.
     */
    @Test
    fun testUpdateDialogDisplay() {
        provisionEmailAliasFlow()
        emailAddresses {
            waitForRecyclerView()
            checkEmailAliasesItemsDisplayed()
            clickOnAliasItem(0)
            checkActionDialogDisplayed()
            clickOnUpdateButton()
            checkUpdateDialogDisplayed()
            clickOnCancelButton()
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
     * Test: Open update dialog, tap Cancel, verify list unchanged.
     */
    @Test
    fun testCancelDismissesWithoutAction() {
        provisionEmailAliasFlow()
        emailAddresses {
            waitForRecyclerView()
            checkEmailAliasesItemsDisplayed()
            clickOnAliasItem(0)
            checkActionDialogDisplayed()
            clickOnUpdateButton()
            checkUpdateDialogDisplayed()
            clickOnCancelButton()
            // Verify list is still displayed and unchanged
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
     * Test: Enter valid JSON metadata, tap Save, verify loading shown and list refreshes
     * with metadata displayed.
     */
    @Test
    fun testSaveWithValidMetadata() {
        provisionEmailAliasFlow()
        emailAddresses {
            waitForRecyclerView()
            checkEmailAliasesItemsDisplayed()
            clickOnAliasItem(0)
            checkActionDialogDisplayed()
            clickOnUpdateButton()
            checkUpdateDialogDisplayed()
            enterMetadataJson("{\"key1\":\"value1\"}")
            clickSaveButton()
            waitForLoading()
            waitForRecyclerView()
            checkEmailAliasesItemsDisplayed()
            checkMetadataDisplayed("key1")
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
     * Test: Leave metadata empty, tap Save, verify "-" shown for metadata.
     */
    @Test
    fun testSaveWithEmptyMetadata() {
        provisionEmailAliasFlow()
        emailAddresses {
            waitForRecyclerView()
            checkEmailAliasesItemsDisplayed()
            clickOnAliasItem(0)
            checkActionDialogDisplayed()
            clickOnUpdateButton()
            checkUpdateDialogDisplayed()
            // Leave metadata empty (default state)
            clickSaveButton()
            waitForLoading()
            waitForRecyclerView()
            checkEmailAliasesItemsDisplayed()
            checkDashDisplayedForMetadata()
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
     * Test: Pick a date, tap Save, verify formatted date in list.
     */
    @Test
    fun testSaveWithExpirationSet() {
        provisionEmailAliasFlow()
        emailAddresses {
            waitForRecyclerView()
            checkEmailAliasesItemsDisplayed()
            clickOnAliasItem(0)
            checkActionDialogDisplayed()
            clickOnUpdateButton()
            checkUpdateDialogDisplayed()
            clickPickDate()
            selectDate(2026, 6, 15)
            clickSaveButton()
            waitForLoading()
            waitForRecyclerView()
            checkEmailAliasesItemsDisplayed()
            checkExpiresAtDisplayed("Expires: 06/15/2026")
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
     * Test: Clear expiration date using the Clear button, tap Save, verify "-" shown for expiration.
     */
    @Test
    fun testSaveWithExpirationCleared() {
        provisionEmailAliasFlow()
        emailAddresses {
            waitForRecyclerView()
            checkEmailAliasesItemsDisplayed()
            // First set an expiration so we can clear it
            clickOnAliasItem(0)
            checkActionDialogDisplayed()
            clickOnUpdateButton()
            checkUpdateDialogDisplayed()
            clickPickDate()
            selectDate(2026, 6, 15)
            clickSaveButton()
            waitForLoading()
            waitForRecyclerView()
            checkEmailAliasesItemsDisplayed()
            // Now clear the expiration
            clickOnAliasItem(0)
            checkActionDialogDisplayed()
            clickOnUpdateButton()
            checkUpdateDialogDisplayed()
            clickClearDate()
            clickSaveButton()
            waitForLoading()
            waitForRecyclerView()
            checkEmailAliasesItemsDisplayed()
            checkDashDisplayedForExpiresAt()
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
     * Test: Verify "-" is shown when metadata is null/empty and expiresAt is null/epoch-zero.
     * A freshly provisioned alias should display dashes for both fields.
     */
    @Test
    fun testDashDisplayForAbsentValues() {
        provisionEmailAliasFlow()
        emailAddresses {
            waitForRecyclerView()
            checkEmailAliasesItemsDisplayed()
            checkDashDisplayedForMetadata()
            checkDashDisplayedForExpiresAt()
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
