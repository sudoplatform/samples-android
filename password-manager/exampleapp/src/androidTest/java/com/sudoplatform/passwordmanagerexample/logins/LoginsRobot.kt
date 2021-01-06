/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.passwordmanagerexample.logins

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import com.sudoplatform.passwordmanagerexample.BaseRobot
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.vaults.VaultViewHolder
import com.sudoplatform.passwordmanagerexample.vaults.vaults

fun logins(func: LoginsRobot.() -> Unit) = LoginsRobot().apply { func() }

/**
 * Testing robot that manages the logins screen.
 *
 * @since 2020-11-06
 */
class LoginsRobot : BaseRobot() {

    private val toolbar = ViewMatchers.withId(R.id.toolbar)
    private val createLoginButton = ViewMatchers.withId(R.id.createLoginButton)
    private val loginRecyclerView = ViewMatchers.withId(R.id.loginRecyclerView)
    private val loadingDialog = ViewMatchers.withId(R.id.progressBar)
    private val positiveAlertButton = ViewMatchers.withId(android.R.id.button1)

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 2_500L)
        waitForViewToNotDisplay(loadingDialog, 2_500L)
    }

    fun waitForRecyclerView() {
        waitForViewToDisplay(loginRecyclerView, 5_000L)
    }

    fun checkLoginsItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(toolbar, timeout)
        waitForViewToDisplay(createLoginButton)
    }

    fun clickOnCreateLoginButton() {
        clickOnView(createLoginButton)
    }

    fun swipeLeftToDelete(position: Int) {
        Espresso.onView(loginRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<VaultViewHolder>(position,
                ViewActions.swipeLeft()
            )
        )
    }

    fun clickOn(position: Int) {
        Espresso.onView(loginRecyclerView).perform(
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

    fun navigateFromLaunchToLogins() {
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
