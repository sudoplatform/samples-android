/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.passwordmanagerexample.vaultItems

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.sudoplatform.passwordmanagerexample.BaseRobot
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.vaults.VaultViewHolder
import com.sudoplatform.passwordmanagerexample.vaults.vaults

fun vaultItems(func: VaultItemsRobot.() -> Unit) = VaultItemsRobot().apply { func() }

const val LOGIN_BUTTON = "Login"
const val CREDIT_CARD_BUTTON = "Credit Card"
const val BANK_ACCOUNT_BUTTON = "Bank Account"
const val CONTACT_BUTTON = "Contact"
const val DRIVERS_LICENSE_BUTTON = "Driver's License"
const val DOCUMENT_BUTTON = "Document"
const val MEMBERSHIP_BUTTON = "Membership"
const val PASSPORT_BUTTON = "Passport"
const val SSN_BUTTON = "Social Security Number"

/**
 * Testing robot that manages the Vault Items screen.
 *
 * @since 2020-11-06
 */
class VaultItemsRobot : BaseRobot() {

    private val toolbar = ViewMatchers.withId(R.id.toolbar)
    private val createItemButton = ViewMatchers.withId(R.id.createItemButton)
    private val itemRecyclerView = ViewMatchers.withId(R.id.itemRecyclerView)
    private val loadingDialog = ViewMatchers.withId(R.id.progressBar)
    private val positiveAlertButton = ViewMatchers.withId(android.R.id.button1)

    fun waitForLoading(mayMissView: Boolean = true) {
        waitForViewToDisplay(loadingDialog, 2_500L, mayMissView)
        waitForViewToNotDisplay(loadingDialog, 10_000L)
    }

    fun waitForRecyclerView() {
        waitForViewToDisplay(itemRecyclerView, 10_000L)
    }

    fun checkVaultItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(toolbar, timeout)
        waitForViewToDisplay(createItemButton)
    }

    fun clickOnCreateLoginButton() {
        clickOnView(createItemButton)
        onView(withText(LOGIN_BUTTON)).perform(click())
    }

    fun clickOnCreateCreditCardButton() {
        clickOnView(createItemButton)
        onView(withText(CREDIT_CARD_BUTTON)).perform(click())
    }

    fun clickOnCreateBankAccountButton() {
        waitForViewToDisplay(createItemButton)
        clickOnView(createItemButton)
        onView(withText(BANK_ACCOUNT_BUTTON)).perform(click())
    }

    fun clickOnCreateContactButton() {
        waitForViewToDisplay(createItemButton)
        clickOnView(createItemButton)
        onView(withText(CONTACT_BUTTON)).perform(click())
    }

    fun clickOnCreateDriversLicenseButton() {
        waitForViewToDisplay(createItemButton)
        clickOnView(createItemButton)
        onView(withText(DRIVERS_LICENSE_BUTTON)).perform(click())
    }

    fun clickOnCreateDocumentButton() {
        waitForViewToDisplay(createItemButton)
        clickOnView(createItemButton)
        onView(withText(DOCUMENT_BUTTON)).perform(click())
    }

    fun clickOnCreateMembershipButton() {
        waitForViewToDisplay(createItemButton)
        clickOnView(createItemButton)
        onView(withText(MEMBERSHIP_BUTTON)).perform(click())
    }

    fun clickOnCreatePassportButton() {
        waitForViewToDisplay(createItemButton)
        clickOnView(createItemButton)
        onView(withText(PASSPORT_BUTTON)).perform(click())
    }

    fun clickOnCreateSSNButton() {
        waitForViewToDisplay(createItemButton)
        clickOnView(createItemButton)
        onView(withText(SSN_BUTTON)).perform(click())
    }

    fun swipeLeftToDelete(position: Int) {
        Espresso.onView(itemRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<VaultViewHolder>(
                position,
                ViewActions.swipeLeft()
            )
        )
    }

    fun clickOn(position: Int) {
        Espresso.onView(itemRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<VaultViewHolder>(
                position,
                ViewActions.click()
            )
        )
    }

    fun clickOnPositiveAlertDialogButton() {
        checkAlertDialog()
        clickOnView(positiveAlertButton)
    }

    private fun checkAlertDialog() {
        waitForViewToDisplay(positiveAlertButton, 5_000L)
        Espresso.onView(positiveAlertButton)
            .check(ViewAssertions.matches(ViewMatchers.withText(android.R.string.ok)))
    }

    fun navigateFromLaunchToVaultItems() {
        vaults {
            navigateFromLaunchToVaults()

            waitForLoading(true)
            checkVaultsItemsDisplayed()

            clickOnCreateVaultButton()
            waitForLoading()
            waitForRecyclerView()

            checkNewVaultIsDisplayed()
            clickOnVault(0)
        }
    }
}
