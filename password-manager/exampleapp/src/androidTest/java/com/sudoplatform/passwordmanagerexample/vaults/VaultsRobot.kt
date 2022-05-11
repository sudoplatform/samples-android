/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.vaults

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.sudoplatform.passwordmanagerexample.BaseRobot
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.sudos.sudos
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.startsWith
import java.text.SimpleDateFormat
import java.util.Date

fun vaults(func: VaultsRobot.() -> Unit) = VaultsRobot().apply { func() }

/**
 * Testing robot that manages the Vaults screen.
 *
 * @since 2020-09-24
 */
class VaultsRobot : BaseRobot() {

    private val toolbar = withId(R.id.toolbar)
    private val createVaultButton = withId(R.id.createVaultButton)
    private val vaultRecyclerView = withId(R.id.vaultRecyclerView)
    private val loadingDialog = withId(R.id.progressBar)
    private val positiveAlertButton = withId(android.R.id.button1)
    private val nameText = withId(R.id.name)

    fun waitForLoading(mayMissView: Boolean = true) {
        waitForViewToDisplay(loadingDialog, 2_500L, mayMissView)
        waitForViewToNotDisplay(loadingDialog, 10_000L)
    }

    fun waitForRecyclerView() {
        waitForViewToDisplay(vaultRecyclerView, 10_000L)
    }

    fun checkVaultsItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(toolbar, timeout)
        waitForViewToDisplay(createVaultButton)
    }

    fun clickOnCreateVaultButton() {
        Thread.sleep(1000)
        clickOnView(createVaultButton)
    }

    fun clickOnPositiveAlertDialogButton() {
        checkAlertDialog()
        Thread.sleep(1000)
        clickOnView(positiveAlertButton)
    }

    fun checkNewVaultIsDisplayed() {
        val now = Date()
        val expected = SimpleDateFormat("yyyy-MM").format(now)
        scrollToViewInRecyclerView(
            R.id.vaultRecyclerView,
            hasDescendant(allOf(nameText, withText(startsWith(expected))))
        )
    }

    fun clickOnVault(position: Int) {
        onView(vaultRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<VaultViewHolder>(
                position,
                ViewActions.click()
            )
        )
    }

    fun swipeLeftToDelete(position: Int) {
        onView(vaultRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<VaultViewHolder>(
                position,
                ViewActions.swipeLeft()
            )
        )
    }

    private fun checkAlertDialog() {
        waitForViewToDisplay(positiveAlertButton)
        onView(positiveAlertButton).check(matches(withText(android.R.string.ok)))
    }

    fun navigateFromLaunchToVaults() {
        sudos {
            navigateFromLaunchToSudos()
            waitForLoading(true)
            checkSudosItemsDisplayed()
            clickCreateSudo()
            waitForLoading()
        }
        sudos {
            waitForRecyclerView()
            checkSudosItemsDisplayed()
            clickOn(0)
        }
    }
}
