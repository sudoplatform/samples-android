/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaddresses

import android.text.format.DateFormat
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.emailaliases.formatExpiryDate
import com.sudoplatform.emailexample.emailaliases.provisionEmailAlias
import com.sudoplatform.emailexample.emailmessages.emailMessages
import com.sudoplatform.emailexample.ui.TestTags
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

fun emailAddresses(
    composeRule: ComposeTestRule,
    func: EmailAddressesRobot.() -> Unit,
) = EmailAddressesRobot(composeRule).apply { func() }

/**
 * Testing robot that manages the email addresses screen, which also lists the email aliases.
 */
class EmailAddressesRobot(
    composeRule: ComposeTestRule,
) : BaseRobot(composeRule) {
    private companion object {
        /** The skeleton Material's date picker formats each day's description with. */
        const val DAY_DESCRIPTION_SKELETON = "yMMMMEEEEd"

        /** The description of Material's date picker button that shows the next month. */
        const val NEXT_MONTH_DESCRIPTION = "Change to next month"
    }

    fun checkEmailAddressesItemsDisplayed() {
        waitForTag(TestTags.CREATE_EMAIL_ADDRESS_BUTTON)
    }

    /** Wait for the list to show at least [count] email addresses. */
    fun waitForEmailAddresses(count: Int = 1) {
        waitForTagCount(TestTags.EMAIL_ADDRESS_ROW, count)
    }

    fun navigateToProvisionEmailAddressScreen() {
        clickOnTag(TestTags.CREATE_EMAIL_ADDRESS_BUTTON)
        provisionEmailAddress(composeRule) {
            checkProvisionEmailAddressItemsDisplayed()
        }
    }

    fun navigateToEmailMessagesScreen(position: Int) {
        clickOnTagAtPosition(TestTags.EMAIL_ADDRESS_ROW, position)
        emailMessages(composeRule) {
            checkEmailMessagesItemsDisplayed()
        }
    }

    fun navigateToCreateEmailAliasScreen() {
        scrollToAliases(hasTestTag(TestTags.CREATE_EMAIL_ALIAS_BUTTON))
        clickOnTag(TestTags.CREATE_EMAIL_ALIAS_BUTTON)
        provisionEmailAlias(composeRule) {
            checkProvisionEmailAliasItemsDisplayed()
        }
    }

    /** Swipe the email address at [position] away, confirm deprovisioning it and wait for it to go. */
    fun swipeLeftToDelete(position: Int) {
        val count = rowCount(TestTags.EMAIL_ADDRESS_ROW, position)
        swipeLeftOnTagAtPosition(TestTags.EMAIL_ADDRESS_ROW, position)
        clickOnTag(TestTags.DIALOG_CONFIRM)
        waitForLoading()
        waitForRowCountBelow(TestTags.EMAIL_ADDRESS_ROW, count)
    }

    // --- Email Alias management methods ---

    /** Wait for the list to show at least one alias of the selected type. */
    fun checkEmailAliasesItemsDisplayed() {
        scrollToAliases(hasTestTag(TestTags.EMAIL_ALIAS_ROW))
    }

    fun checkEmptyAliasesStateDisplayed() {
        scrollToAliases(hasTestTag(TestTags.EMPTY_ALIASES_TEXT))
    }

    /** Swipe the alias at [position] away, confirm deprovisioning it and wait for it to go. */
    fun swipeLeftToDeleteAlias(position: Int) {
        val count = rowCount(TestTags.EMAIL_ALIAS_ROW, position)
        swipeLeftOnTagAtPosition(TestTags.EMAIL_ALIAS_ROW, position)
        clickOnTag(TestTags.DIALOG_CONFIRM)
        waitForLoading()
        waitForRowCountBelow(TestTags.EMAIL_ALIAS_ROW, count)
    }

    fun clickOnAliasItem(position: Int) {
        checkEmailAliasesItemsDisplayed()
        clickOnTagAtPosition(TestTags.EMAIL_ALIAS_ROW, position)
    }

    fun checkActionDialogDisplayed() {
        waitForTag(TestTags.UPDATE_ALIAS_ACTION)
        waitForTag(TestTags.DIALOG_DISMISS)
    }

    fun clickOnEnableButton() {
        clickOnTag(TestTags.ENABLE_ALIAS_ACTION)
    }

    fun clickOnDisableButton() {
        clickOnTag(TestTags.DISABLE_ALIAS_ACTION)
    }

    fun clickOnCancelButton() {
        clickOnTag(TestTags.DIALOG_DISMISS)
        waitForTagToDisappear(TestTags.DIALOG_DISMISS)
    }

    fun waitForActionToComplete() {
        waitForLoading()
    }

    /**
     * Wait for an alias to show [statusResId]. The list is reloaded after enabling or disabling,
     * and the actions offered when an alias is tapped follow its listed status.
     */
    fun waitForAliasStatus(statusResId: Int) {
        waitForNode(hasTestTag(TestTags.ALIAS_STATUS_TEXT) and hasText(string(statusResId)), useUnmergedTree = true)
    }

    // --- Update-related methods ---

    fun clickOnUpdateButton() {
        clickOnTag(TestTags.UPDATE_ALIAS_ACTION)
    }

    fun checkUpdateDialogDisplayed() {
        waitForTag(TestTags.ALIAS_METADATA_FIELD)
    }

    fun enterMetadataJson(json: String) {
        replaceText(TestTags.ALIAS_METADATA_FIELD, json)
    }

    /** Open the date picker for the alias's expiry. */
    fun clickPickDate() {
        clickOnTag(TestTags.ALIAS_EXPIRY_BUTTON)
    }

    fun clickClearDate() {
        clickOnTag(TestTags.CLEAR_ALIAS_EXPIRY_BUTTON)
    }

    /**
     * Pick [date], which must be in the month after the current one, in the open date picker and
     * confirm it. The picker opens on the current month.
     */
    fun selectDateInNextMonth(date: LocalDate) {
        val nextMonth = hasContentDescription(NEXT_MONTH_DESCRIPTION) and hasClickAction()
        waitForNode(nextMonth)
        composeRule.onNode(nextMonth).performClick()
        val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), DAY_DESCRIPTION_SKELETON)
        val description = date.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
        val day = hasText(description, substring = true) and hasClickAction()
        waitForNode(day)
        composeRule.onNode(day).performClick()
        clickOnTag(TestTags.DATE_PICKER_CONFIRM)
    }

    /** Save the update dialog and wait for the aliases to reload. */
    fun clickSaveButton() {
        clickOnTag(TestTags.DIALOG_CONFIRM)
        waitForTagToDisappear(TestTags.ALIAS_METADATA_FIELD)
        waitForLoading()
    }

    fun checkMetadataDisplayed(expectedJson: String) {
        waitForNode(
            hasTestTag(TestTags.ALIAS_METADATA_TEXT) and hasText(expectedJson, substring = true),
            useUnmergedTree = true,
        )
    }

    /** Check an alias shows that it expires on [date]. */
    fun checkExpiresAtDisplayed(date: LocalDate) {
        val expiry = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
        checkExpiresAtText("${string(R.string.set_expiration)} ${formatExpiryDate(expiry)}")
    }

    fun checkDashDisplayedForMetadata() {
        waitForNode(
            hasTestTag(TestTags.ALIAS_METADATA_TEXT) and hasText(string(R.string.alias_metadata_value, "-")),
            useUnmergedTree = true,
        )
    }

    fun checkDashDisplayedForExpiresAt() {
        checkExpiresAtText("${string(R.string.set_expiration)} -")
    }

    private fun checkExpiresAtText(text: String) {
        waitForNode(hasTestTag(TestTags.ALIAS_EXPIRES_TEXT) and hasText(text), useUnmergedTree = true)
    }

    private fun scrollToAliases(matcher: SemanticsMatcher) {
        scrollListTo(TestTags.EMAIL_ADDRESSES_LIST, matcher)
    }

    /** The number of rows tagged [tag], once there is one at [position]. */
    private fun rowCount(
        tag: String,
        position: Int,
    ): Int {
        waitForTagCount(tag, position + 1)
        return composeRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().size
    }

    private fun waitForRowCountBelow(
        tag: String,
        count: Int,
    ) {
        composeRule.waitUntil(DEFAULT_TIMEOUT_MS) {
            composeRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().size < count
        }
    }
}
