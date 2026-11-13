/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.sudos

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.sudoplatform.telephonyexample.BaseRobot
import com.sudoplatform.telephonyexample.register.register
import com.sudoplatform.telephonyexample.ui.TestTags

fun sudos(
    composeRule: ComposeTestRule,
    func: SudosRobot.() -> Unit,
) = SudosRobot(composeRule).apply { func() }

/**
 * Testing robot that manages the Sudos list, which is the home screen, and the Create Sudo and
 * Edit Sudo screens.
 */
@OptIn(ExperimentalTestApi::class)
class SudosRobot(
    composeRule: ComposeTestRule,
) : BaseRobot(composeRule) {
    companion object {
        private const val MAX_BACK_PRESSES = 6
        private const val BACK_SETTLE_TIMEOUT_MS = 2_000L
    }

    fun waitForSudos() {
        waitForTag(TestTags.CREATE_SUDO_BUTTON)
    }

    /** Register (or sign in) from the register screen and wait for the Sudos. */
    fun navigateFromLaunchToSudos() {
        register(composeRule) {
            waitForRegisterScreen()
            clickOnRegister()
        }
        waitForSudos()
    }

    /**
     * Press back until the Sudos show, from whichever screen a test stopped on. Each press is
     * given time to settle so that the Sudos screen itself is never backed out of.
     */
    fun navigateBackToSudos() {
        repeat(MAX_BACK_PRESSES) {
            try {
                composeRule.waitUntilAtLeastOneExists(hasTestTag(TestTags.CREATE_SUDO_BUTTON), BACK_SETTLE_TIMEOUT_MS)
                return
            } catch (e: ComposeTimeoutException) {
                pressBack()
            }
        }
        waitForSudos()
    }

    fun deregister() {
        clickOnAccountRow(TestTags.DEREGISTER_ACTION)
        clickOnTag(TestTags.DIALOG_CONFIRM)
        register(composeRule) { waitForRegisterScreen() }
    }

    /** Create a Sudo. The app then shows its phone numbers, so go back to the list. */
    fun createSudo(label: String) {
        clickOnTag(TestTags.CREATE_SUDO_BUTTON)
        waitForTag(TestTags.SUDO_LABEL_FIELD)
        composeRule.onNodeWithTag(TestTags.SUDO_LABEL_FIELD).performTextInput(label)
        clickOnTag(TestTags.CREATE_SUDO_ACTION)
        waitForTag(TestTags.PROVISION_PHONE_NUMBER_BUTTON)
        pressBack()
        waitForSudos()
    }

    fun openSudo(label: String) {
        waitForSudo(label)
        composeRule.onNode(sudoRow(label)).performClick()
    }

    fun editSudo(
        label: String,
        newLabel: String,
    ) {
        composeRule.onNodeWithContentDescription("Edit $label").performClick()
        waitForText(label)
        composeRule.onNodeWithTag(TestTags.SUDO_LABEL_FIELD).performTextClearance()
        composeRule.onNodeWithTag(TestTags.SUDO_LABEL_FIELD).performTextInput(newLabel)
        clickOnTag(TestTags.SAVE_SUDO_ACTION)
        waitForSudos()
    }

    fun deleteSudo(label: String) {
        composeRule.onNode(sudoRow(label)).performTouchInput { swipeLeft() }
        clickOnTag(TestTags.DIALOG_CONFIRM)
        waitForSudoToDisappear(label)
        waitForTagToDisappear(TestTags.LOADING)
    }

    /** A TEST registration can't back up its keys, so the app explains why instead of opening Key Backup. */
    fun checkKeyBackupNeedsFederatedSignIn() {
        clickOnAccountRow(TestTags.KEY_BACKUP_ACTION)
        waitForText("Key Backup Needs Federated Sign In")
        clickOnTag(TestTags.DIALOG_CONFIRM)
        waitForSudos()
    }

    /** Open a Sudo and come back, as the Sudos are fetched from the service each time the list shows. */
    fun refreshByOpeningSudo(label: String) {
        openSudo(label)
        waitForTag(TestTags.PROVISION_PHONE_NUMBER_BUTTON)
        pressBack()
    }

    fun waitForKeyImportNotice() {
        waitForTag(TestTags.KEY_IMPORT_NOTICE)
    }

    fun waitForTelephonyKeyBackupNotice() {
        waitForTag(TestTags.TELEPHONY_KEY_BACKUP_NOTICE)
    }

    /** Open Entitlements and wait for the user's entitlements, which registration redeems. */
    fun checkEntitlements() {
        clickOnAccountRow(TestTags.ENTITLEMENTS_ACTION)
        waitForTag(TestTags.ENTITLEMENT_ROW)
        pressBack()
        waitForSudos()
    }

    /** Open User Details and check it shows the signed in user's ID. */
    fun checkUserDetails() {
        clickOnAccountRow(TestTags.USER_DETAILS_ACTION)
        waitForTag(TestTags.USER_ID_VALUE)
        composeRule.onNodeWithTag(TestTags.USER_ID_VALUE).assert(hasText("Not available").not())
        pressBack()
        waitForSudos()
    }

    fun waitForSudo(label: String) {
        composeRule.waitUntilAtLeastOneExists(sudoRow(label), DEFAULT_TIMEOUT_MS)
    }

    fun waitForSudoToDisappear(label: String) {
        composeRule.waitUntilDoesNotExist(sudoRow(label), DEFAULT_TIMEOUT_MS)
    }

    /** The account rows follow the Sudos, so scroll to them first. */
    private fun clickOnAccountRow(tag: String) {
        waitForTag(TestTags.SUDOS_LIST)
        composeRule.onNodeWithTag(TestTags.SUDOS_LIST).performScrollToNode(hasTestTag(tag))
        clickOnTag(tag)
    }

    private fun sudoRow(label: String) = hasTestTag(TestTags.SUDO_ROW) and hasAnyDescendant(hasText(label))
}
