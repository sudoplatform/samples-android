/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.sudos

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.register.login
import com.sudoplatform.emailexample.ui.TestTags

fun sudos(
    composeRule: ComposeTestRule,
    func: SudosRobot.() -> Unit,
) = SudosRobot(composeRule).apply { func() }

/**
 * Testing robot that manages the Sudos screen, the app's home screen, and its account section.
 */
@OptIn(ExperimentalTestApi::class)
class SudosRobot(
    composeRule: ComposeTestRule,
) : BaseRobot(composeRule) {
    companion object {
        private const val MAX_BACK_PRESSES = 6
        private const val BACK_SETTLE_TIMEOUT_MS = 2_000L
    }

    fun checkSudosItemsDisplayed() {
        waitForTag(TestTags.CREATE_SUDO_BUTTON)
    }

    /** Register (or sign in) from the register screen, unless already signed in, and wait for the Sudos. */
    fun navigateFromLaunchToSudos() {
        composeRule.waitUntil(DEFAULT_TIMEOUT_MS) {
            composeRule.onAllNodes(hasTestTag(TestTags.REGISTER_BUTTON)).fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasTestTag(TestTags.CREATE_SUDO_BUTTON)).fetchSemanticsNodes().isNotEmpty()
        }
        if (composeRule.onAllNodes(hasTestTag(TestTags.REGISTER_BUTTON)).fetchSemanticsNodes().isNotEmpty()) {
            login(composeRule) { clickOnRegister() }
        }
        checkSudosItemsDisplayed()
    }

    fun navigateToCreateSudoScreen() {
        clickOnTag(TestTags.CREATE_SUDO_BUTTON)
        createSudo(composeRule) {
            checkCreateSudoItemsDisplayed()
        }
    }

    /** Swipe the Sudo at [position] away and confirm deleting it. */
    fun swipeLeftToDelete(position: Int) {
        swipeLeftOnTagAtPosition(TestTags.SUDO_ROW, position)
        clickOnDialogButton(TestTags.DIALOG_CONFIRM, R.string.delete)
        waitForLoading()
    }

    /** Swipe the Sudo at [position] away, then cancel deleting it. */
    fun swipeLeftAndCancelDelete(position: Int) {
        swipeLeftOnTagAtPosition(TestTags.SUDO_ROW, position)
        clickOnDialogButton(TestTags.DIALOG_DISMISS, R.string.cancel)
        waitForTagToDisappear(TestTags.DIALOG_DISMISS)
    }

    fun checkSudoCount(count: Int) {
        checkSudosItemsDisplayed()
        composeRule.waitUntil(DEFAULT_TIMEOUT_MS) {
            composeRule.onAllNodes(hasTestTag(TestTags.SUDO_ROW)).fetchSemanticsNodes().size == count
        }
    }

    /** A TEST registration shows Entitlements, Key Backup and Deregister, but not Sign Out. */
    fun checkSignedInWithTestRegistration() {
        scrollToAccountRow(TestTags.USER_DETAILS_ACTION)
        composeRule
            .onNodeWithTag(TestTags.USER_DETAILS_ACTION)
            .assert(hasText(string(R.string.test_registration)))
        scrollToAccountRow(TestTags.ENTITLEMENTS_ACTION)
        scrollToAccountRow(TestTags.KEY_BACKUP_ACTION)
        scrollToAccountRow(TestTags.DEREGISTER_ACTION)
        composeRule.onNodeWithTag(TestTags.SIGN_OUT_ACTION).assertDoesNotExist()
    }

    /** Open User Details, check it shows the signed in user's ID, and come back. */
    fun checkUserDetails() {
        clickOnAccountRow(TestTags.USER_DETAILS_ACTION)
        waitForTag(TestTags.USER_ID_VALUE)
        composeRule.onNodeWithTag(TestTags.USER_ID_VALUE).assert(hasText(string(R.string.not_available)).not())
        navigateBack()
        checkSudosItemsDisplayed()
    }

    /** Open Entitlements, wait for the entitlements registration redeemed, and come back. */
    fun checkEntitlements() {
        clickOnAccountRow(TestTags.ENTITLEMENTS_ACTION)
        waitForTag(TestTags.ENTITLEMENT_ROW)
        navigateBack()
        checkSudosItemsDisplayed()
    }

    fun clickOnKeyBackup() {
        clickOnAccountRow(TestTags.KEY_BACKUP_ACTION)
    }

    /** Check that a TEST registration is told key backup needs Federated Sign In, then close the message. */
    fun checkKeyBackupNeedsFssoAndDismiss() {
        waitForText(string(R.string.key_backup_needs_fsso_title))
        clickOnDialogButton(TestTags.DIALOG_CONFIRM, R.string.ok)
        checkSudosItemsDisplayed()
    }

    fun clickOnDeregister() {
        clickOnAccountRow(TestTags.DEREGISTER_ACTION)
    }

    fun clickOnPositiveDeregisterAlertDialogButton() {
        clickOnDialogButton(TestTags.DIALOG_CONFIRM, R.string.deregister)
    }

    fun clickOnNegativeDeregisterAlertDialogButton() {
        clickOnDialogButton(TestTags.DIALOG_DISMISS, R.string.cancel)
        waitForTagToDisappear(TestTags.DIALOG_DISMISS)
    }

    /** Deregister and wait for the register screen. */
    fun waitForEmailKeyBackupNotice() {
        waitForTag(TestTags.EMAIL_KEY_BACKUP_NOTICE)
    }

    fun deregister() {
        clickOnDeregister()
        clickOnPositiveDeregisterAlertDialogButton()
        login(composeRule) { waitForRegisterScreen() }
    }

    /**
     * Press back until the Sudos show, from whichever screen a test stopped on. Each press is
     * given time to settle so that the Sudos screen itself is never backed out of.
     */
    fun pressBackUntilSudosAreDisplayed() {
        repeat(MAX_BACK_PRESSES) {
            try {
                composeRule.waitUntilAtLeastOneExists(hasTestTag(TestTags.CREATE_SUDO_BUTTON), BACK_SETTLE_TIMEOUT_MS)
                return
            } catch (e: ComposeTimeoutException) {
                pressBack()
            }
        }
        checkSudosItemsDisplayed()
    }

    /** Click a dialog button after checking it shows [textResId]. */
    private fun clickOnDialogButton(
        tag: String,
        textResId: Int,
    ) {
        waitForTag(tag)
        composeRule
            .onNodeWithTag(tag)
            .assert(hasText(string(textResId).uppercase()))
            .performClick()
    }

    /** The account rows follow the Sudos, so scroll to them first. */
    private fun scrollToAccountRow(tag: String) {
        scrollListTo(TestTags.SUDOS_LIST, hasTestTag(tag))
        waitForTag(tag)
    }

    private fun clickOnAccountRow(tag: String) {
        scrollToAccountRow(tag)
        clickOnTag(tag)
    }
}
