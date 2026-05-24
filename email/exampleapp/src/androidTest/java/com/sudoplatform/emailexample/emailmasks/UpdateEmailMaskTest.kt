/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmasks

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
 * Test the update email mask flows.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class UpdateEmailMaskTest {
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
     * Test: Provision a mask, tap item, tap "Update", verify the update dialog appears.
     */
    @Test
    fun testUpdateDialogDisplay() {
        provisionEmailMaskFlow()
        emailAddresses {
            waitForRecyclerView()
            checkEmailMasksItemsDisplayed()
            clickOnMaskItem(0)
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
        provisionEmailMaskFlow()
        emailAddresses {
            waitForRecyclerView()
            checkEmailMasksItemsDisplayed()
            clickOnMaskItem(0)
            checkActionDialogDisplayed()
            clickOnUpdateButton()
            checkUpdateDialogDisplayed()
            clickOnCancelButton()
            // Verify list is still displayed and unchanged
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
     * Test: Enter valid JSON metadata, tap Save, verify loading shown and list refreshes
     * with metadata displayed.
     */
    @Test
    fun testSaveWithValidMetadata() {
        provisionEmailMaskFlow()
        emailAddresses {
            waitForRecyclerView()
            checkEmailMasksItemsDisplayed()
            clickOnMaskItem(0)
            checkActionDialogDisplayed()
            clickOnUpdateButton()
            checkUpdateDialogDisplayed()
            enterMetadataJson("{\"key1\":\"value1\"}")
            clickSaveButton()
            waitForLoading()
            waitForRecyclerView()
            checkEmailMasksItemsDisplayed()
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
        provisionEmailMaskFlow()
        emailAddresses {
            waitForRecyclerView()
            checkEmailMasksItemsDisplayed()
            clickOnMaskItem(0)
            checkActionDialogDisplayed()
            clickOnUpdateButton()
            checkUpdateDialogDisplayed()
            // Leave metadata empty (default state)
            clickSaveButton()
            waitForLoading()
            waitForRecyclerView()
            checkEmailMasksItemsDisplayed()
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
        provisionEmailMaskFlow()
        emailAddresses {
            waitForRecyclerView()
            checkEmailMasksItemsDisplayed()
            clickOnMaskItem(0)
            checkActionDialogDisplayed()
            clickOnUpdateButton()
            checkUpdateDialogDisplayed()
            clickPickDate()
            selectDate(2026, 6, 15)
            clickSaveButton()
            waitForLoading()
            waitForRecyclerView()
            checkEmailMasksItemsDisplayed()
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
        provisionEmailMaskFlow()
        emailAddresses {
            waitForRecyclerView()
            checkEmailMasksItemsDisplayed()
            // First set an expiration so we can clear it
            clickOnMaskItem(0)
            checkActionDialogDisplayed()
            clickOnUpdateButton()
            checkUpdateDialogDisplayed()
            clickPickDate()
            selectDate(2026, 6, 15)
            clickSaveButton()
            waitForLoading()
            waitForRecyclerView()
            checkEmailMasksItemsDisplayed()
            // Now clear the expiration
            clickOnMaskItem(0)
            checkActionDialogDisplayed()
            clickOnUpdateButton()
            checkUpdateDialogDisplayed()
            clickClearDate()
            clickSaveButton()
            waitForLoading()
            waitForRecyclerView()
            checkEmailMasksItemsDisplayed()
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
     * A freshly provisioned mask should display dashes for both fields.
     */
    @Test
    fun testDashDisplayForAbsentValues() {
        provisionEmailMaskFlow()
        emailAddresses {
            waitForRecyclerView()
            checkEmailMasksItemsDisplayed()
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
