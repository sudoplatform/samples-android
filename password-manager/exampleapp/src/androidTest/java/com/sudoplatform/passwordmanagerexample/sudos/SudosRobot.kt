/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.sudos

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.sudoplatform.passwordmanagerexample.AppHolder
import com.sudoplatform.passwordmanagerexample.BaseRobot
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.register.login
import com.sudoplatform.passwordmanagerexample.saveSecretCode
import com.sudoplatform.passwordmanagerexample.unlock.unlock
import com.sudoplatform.sudopasswordmanager.PasswordManagerRegistrationStatus

fun sudos(func: SudosRobot.() -> Unit) = SudosRobot().apply { func() }

/**
 * Testing robot that manages the Sudos screen.
 *
 * @since 2020-08-03
 */
class SudosRobot : BaseRobot() {

    private val toolbar = withId(R.id.toolbar)
    private val createSudoButton = withId(R.id.createSudoButton)
    private val sudoRecyclerView = withId(R.id.sudoRecyclerView)
    private val toolbarSettingsButton = withId(R.id.settings)
    private val loadingDialog = withId(R.id.progressBar)
    private val positiveAlertButton = withId(android.R.id.button1)

    fun waitForLoading(mayMissView: Boolean = false) {
        waitForViewToDisplay(loadingDialog, 2_500L, mayMissView)
        waitForViewToNotDisplay(loadingDialog, 10_000L)
    }

    fun waitForRecyclerView() {
        waitForViewToDisplay(sudoRecyclerView, 5_000L)
    }

    fun checkSudosItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(toolbar, timeout)
        waitForViewToDisplay(createSudoButton)
    }

    fun clickOnPositiveAlertDialogButton() {
        checkAlertDialog()
        Thread.sleep(1000)
        clickOnView(positiveAlertButton)
    }

    fun swipeLeftToDelete(position: Int) {
        onView(sudoRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<SudoViewHolder>(
                position,
                ViewActions.swipeLeft()
            )
        )
    }

    fun clickOn(position: Int) {
        onView(sudoRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<SudoViewHolder>(
                position,
                ViewActions.click()
            )
        )
    }

    fun clickCreateSudo() {
        clickOnView(createSudoButton)
    }

    fun navigateFromLaunchToSudos() {
        login {
            try {
                waitForRegisterButton()
                clickOnRegister()
            } catch (e: NoMatchingViewException) {
                // Login screen was skipped because already logged in
            }
        }
        unlock {
            try {
                when (AppHolder.getRegistrationStatus()) {
                    PasswordManagerRegistrationStatus.REGISTERED -> {
                        if (AppHolder.isLocked()) {
                            waitForPasswordView(30_000L)
                            checkEnterMasterPasswordItemsDisplayed(5_000L)
                            enterMasterPassword()
                        } else {
                            Thread.sleep(1000)
                        }
                    }
                    PasswordManagerRegistrationStatus.MISSING_SECRET_CODE -> {
                        waitForPasswordView(30_000L)
                        checkEnterMasterPasswordItemsDisplayed(5_000L)
                        enterMasterPasswordAndSecretCode()
                    }
                    else -> {
                        waitForPasswordView(30_000L)
                        checkCreateMasterPasswordItemsDisplayed(5_000L)
                        createMasterPassword()
                    }
                }
                saveSecretCode()
            } catch (e: NoMatchingViewException) {
                // Unlock screen was skipped because we have launched the app soon after
                // it was last unlocked and unlocking is not required.
            }
        }
        checkSudosItemsDisplayed(20_000L)
    }

    fun clickOnSettings() {
        clickOnView(toolbarSettingsButton)
    }

    private fun checkAlertDialog() {
        waitForViewToDisplay(positiveAlertButton, 5_000L)
        onView(positiveAlertButton).check(matches(withText(android.R.string.ok)))
    }
}
