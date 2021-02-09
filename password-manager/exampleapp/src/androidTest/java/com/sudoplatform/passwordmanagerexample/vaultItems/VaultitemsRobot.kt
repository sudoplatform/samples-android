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

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 2_500L)
        waitForViewToNotDisplay(loadingDialog, 2_500L)
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
        clickOnView(createItemButton)
        onView(withText(BANK_ACCOUNT_BUTTON)).perform(click())
    }

    fun swipeLeftToDelete(position: Int) {
        Espresso.onView(itemRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<VaultViewHolder>(position,
                ViewActions.swipeLeft()
            )
        )
    }

    fun clickOn(position: Int) {
        Espresso.onView(itemRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<VaultViewHolder>(position,
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

            waitForLoading()
            waitForRecyclerView()
            checkVaultsItemsDisplayed()

            clickOnCreateVaultButton()
            waitForLoading()
            waitForRecyclerView()

            checkNewVaultIsDisplayed()
            clickOnVault(0)
        }
    }
}
