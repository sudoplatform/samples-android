/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaddresses

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.sudos.createSudo
import com.sudoplatform.emailexample.ui.TestTags
import java.util.UUID

fun provisionEmailAddress(
    composeRule: ComposeTestRule,
    func: ProvisionEmailAddressRobot.() -> Unit,
) = ProvisionEmailAddressRobot(composeRule).apply { func() }

/**
 * Testing robot that manages the provision email address screen.
 */
class ProvisionEmailAddressRobot(
    composeRule: ComposeTestRule,
) : BaseRobot(composeRule) {
    fun checkProvisionEmailAddressItemsDisplayed() {
        waitForTag(TestTags.PROVISION_EMAIL_ADDRESS_ACTION)
        waitForTag(TestTags.LOCAL_PART_FIELD)
        waitForTag(TestTags.SUDO_LABEL_TEXT)
        waitForText(string(R.string.provision_email_address_learn_more))
        waitForTag(TestTags.LEARN_MORE_BUTTON)
    }

    /** The address that will be provisioned, once the local part is found to be available. */
    fun getAddressFromTextView(): String = textOf(TestTags.AVAILABLE_ADDRESS_TEXT)

    /** Wait for the availability check, which runs a moment after typing stops, to find the address available. */
    fun checkAvailableAddressMsg() {
        checkAvailabilityText(string(R.string.email_address_available))
    }

    fun checkUnavailableAddressMsg() {
        checkAvailabilityText(string(R.string.email_address_unavailable))
    }

    /** The service rejects the local part, and the screen shows its reason. */
    fun checkInvalidAddressMsg() {
        checkAvailabilityText("Invalid email address")
    }

    /** Create the address and wait for the email addresses, which the app returns to. */
    fun clickOnCreateButton() {
        clickOnEnabledTag(TestTags.PROVISION_EMAIL_ADDRESS_ACTION)
        waitForTagToDisappear(TestTags.LOCAL_PART_FIELD)
        emailAddresses(composeRule) {
            checkEmailAddressesItemsDisplayed()
        }
    }

    /** Enter [localPart], or a random one when it is blank. */
    fun setLocalPart(localPart: String? = "") {
        var input = localPart
        if (input.isNullOrBlank()) {
            input = UUID.randomUUID().toString().substring(0, 8)
        }
        replaceText(TestTags.LOCAL_PART_FIELD, input)
    }

    /**
     * Register, create a Sudo and provision an address for it, ending on the email addresses.
     *
     * @return the provisioned address.
     */
    fun provisionEmailAddressFlow(): String {
        createSudo(composeRule) {
            registerAndCreateSudoFlow()
        }
        emailAddresses(composeRule) {
            navigateToProvisionEmailAddressScreen()
        }
        setLocalPart()
        checkAvailableAddressMsg()
        val address = getAddressFromTextView()
        clickOnCreateButton()
        emailAddresses(composeRule) {
            waitForEmailAddresses()
        }
        return address
    }

    private fun checkAvailabilityText(text: String) {
        waitForNode(hasTestTag(TestTags.AVAILABILITY_TEXT) and hasText(text))
    }
}
