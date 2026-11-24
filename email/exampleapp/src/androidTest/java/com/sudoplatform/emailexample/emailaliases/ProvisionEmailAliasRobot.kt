/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaliases

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.emailaddresses.emailAddresses
import com.sudoplatform.emailexample.ui.TestTags
import org.junit.Assume.assumeFalse
import java.util.UUID

fun provisionEmailAlias(
    composeRule: ComposeTestRule,
    func: ProvisionEmailAliasRobot.() -> Unit,
) = ProvisionEmailAliasRobot(composeRule).apply { func() }

/** The SDK's InsufficientEntitlementsException message. */
private const val INSUFFICIENT_ENTITLEMENTS_MESSAGE = "Entitlements have been exceeded"

/**
 * Testing robot that manages the provision email alias screen, which provisions internal aliases
 * and, opened for external aliases, external ones.
 */
class ProvisionEmailAliasRobot(
    composeRule: ComposeTestRule,
) : BaseRobot(composeRule) {
    fun checkProvisionEmailAliasItemsDisplayed() {
        waitForTag(TestTags.PROVISION_EMAIL_ALIAS_ACTION)
        waitForTag(TestTags.REAL_ADDRESS_FIELD)
        checkCommonItemsDisplayed()
    }

    fun checkProvisionExternalEmailAliasItemsDisplayed() {
        waitForTag(TestTags.PROVISION_EMAIL_ALIAS_ACTION)
        waitForTag(TestTags.EXTERNAL_ADDRESS_FIELD)
        checkCommonItemsDisplayed()
    }

    /** Wait for the availability check, which runs a moment after typing stops, to find the alias available. */
    fun checkAvailableAddressMsg() {
        checkAvailabilityText(string(R.string.email_address_available))
    }

    fun checkUnavailableAddressMsg() {
        checkAvailabilityText(string(R.string.email_address_unavailable))
    }

    /** Choose the real address at [position] among the Sudo's email addresses. */
    fun selectRealEmailAddress(position: Int) {
        selectDropdownOption(TestTags.REAL_ADDRESS_FIELD, position)
    }

    fun enterExternalEmail(email: String) {
        replaceText(TestTags.EXTERNAL_ADDRESS_FIELD, email)
    }

    /** Enter [localPart], or a random one when it is blank. */
    fun setLocalPart(localPart: String? = "") {
        var input = localPart
        if (input.isNullOrBlank()) {
            input = UUID.randomUUID().toString().substring(0, 8)
        }
        replaceText(TestTags.ALIAS_LOCAL_PART_FIELD, input)
    }

    /** Create the alias and wait for the email addresses, which the app returns to. */
    fun clickOnCreateButton() {
        clickOnCreateButtonExpectingAlert()
        skipIfAliasProvisioningNotEntitled()
        waitForTagToDisappear(TestTags.ALIAS_LOCAL_PART_FIELD)
        emailAddresses(composeRule) {
            checkEmailAddressesItemsDisplayed()
        }
    }

    /** Tap Create when the form is expected to be rejected with an alert. */
    fun clickOnCreateButtonExpectingAlert() {
        clickOnEnabledTag(TestTags.PROVISION_EMAIL_ALIAS_ACTION)
    }

    fun checkAlertDialogDisplayed() {
        waitForTag(TestTags.DIALOG_CONFIRM)
    }

    /** Check the alert says the external address isn't a valid email address. */
    fun checkInvalidEmailFormatAlertDisplayed() {
        checkAlertDialogDisplayed()
        waitForText(string(R.string.invalid_email_format))
    }

    fun clickOnPositiveAlertDialogButton() {
        clickOnTag(TestTags.DIALOG_CONFIRM)
        waitForTagToDisappear(TestTags.DIALOG_CONFIRM)
    }

    /**
     * Skips the test if the service refused the alias because the test user isn't entitled to
     * provision one, as on environments whose TEST entitlement set lacks the alias entitlements.
     * Any other failure still fails the test.
     */
    @OptIn(ExperimentalTestApi::class)
    private fun skipIfAliasProvisioningNotEntitled() {
        val failureTitle = hasText(string(R.string.provision_email_alias_failure))
        composeRule.waitUntil(DEFAULT_TIMEOUT_MS) {
            composeRule.onAllNodes(hasTestTag(TestTags.ALIAS_LOCAL_PART_FIELD)).fetchSemanticsNodes().isEmpty() ||
                composeRule.onAllNodes(failureTitle).fetchSemanticsNodes().isNotEmpty()
        }
        val refused =
            composeRule
                .onAllNodes(
                    hasText(INSUFFICIENT_ENTITLEMENTS_MESSAGE, substring = true),
                ).fetchSemanticsNodes()
                .isNotEmpty()
        assumeFalse("The test user isn't entitled to provision email aliases in this environment", refused)
    }

    private fun checkCommonItemsDisplayed() {
        waitForTag(TestTags.ALIAS_LOCAL_PART_FIELD)
        waitForTag(TestTags.ALIAS_DOMAIN_FIELD)
        waitForText(string(R.string.sudo))
        waitForTag(TestTags.SUDO_LABEL_TEXT)
    }

    private fun checkAvailabilityText(text: String) {
        waitForNode(hasTestTag(TestTags.AVAILABILITY_TEXT) and hasText(text))
    }
}
