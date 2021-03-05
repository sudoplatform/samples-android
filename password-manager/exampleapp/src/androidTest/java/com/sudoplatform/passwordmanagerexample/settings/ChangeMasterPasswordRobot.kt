/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.settings

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.sudoplatform.passwordmanagerexample.BaseRobot
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.unlock.MASTER_PASSWORD

fun changeMasterPassword(func: ChangeMasterPasswordRobot.() -> Unit) = ChangeMasterPasswordRobot().apply { func() }

private const val NEW_MASTER_PASSWORD = "${MASTER_PASSWORD}_new"

/**
 * Testing robot that manages the change master password screen.
 *
 * @since 2020-10-28
 */
class ChangeMasterPasswordRobot : BaseRobot() {

    private val currentPasswordTextView = withId(R.id.currentPasswordText)
    private val newPasswordTextView = withId(R.id.newPasswordText)
    private val saveButton = withId(R.id.save)
    private val positiveAlertButton = withId(android.R.id.button1)
    private val loadingDialog = withId(R.id.progressBar)
    private val alertMessage = withId(android.R.id.message)

    fun checkChangeMasterPasswordItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(currentPasswordTextView, timeout)
        waitForViewToDisplay(newPasswordTextView, timeout)
        waitForViewToDisplay(saveButton, timeout)
    }

    fun clickOnSave() {
        Thread.sleep(1000)
        clickOnView(saveButton)
    }

    fun enterCurrentPasswordAndEmptyNewPassword() {
        replaceText(currentPasswordTextView, MASTER_PASSWORD)
        onView(newPasswordTextView)
            .perform(ViewActions.click())
            .perform(ViewActions.clearText())
    }

    fun enterEmptyCurrentPasswordAndNewPassword() {
        replaceText(newPasswordTextView, MASTER_PASSWORD)
        onView(currentPasswordTextView)
            .perform(ViewActions.click())
            .perform(ViewActions.clearText())
    }

    fun enterSameCurrentAndNewPasswords() {
        replaceText(currentPasswordTextView, MASTER_PASSWORD)
        replaceText(newPasswordTextView, MASTER_PASSWORD)
    }

    fun enterCurrentPasswordAndNewPassword() {
        replaceText(currentPasswordTextView, MASTER_PASSWORD)
        replaceText(newPasswordTextView, NEW_MASTER_PASSWORD)
    }

    fun enterNewPasswordAndOldPassword() {
        replaceText(currentPasswordTextView, NEW_MASTER_PASSWORD)
        replaceText(newPasswordTextView, MASTER_PASSWORD)
    }

    fun checkMissingCurrentPasswordAlertDialog() {
        checkAlertDialog()
        onView(alertMessage)
            .check(matches(withText(R.string.enter_current_password_error)))
    }

    fun checkMissingNewPasswordAlertDialog() {
        checkAlertDialog()
        onView(alertMessage)
            .check(matches(withText(R.string.enter_new_password_error)))
    }

    fun checkSamePasswordsAlertDialog() {
        checkAlertDialog()
        onView(alertMessage)
            .check(matches(withText(R.string.master_password_same_error)))
    }

    fun clickOnPositiveAlertDialogButton() {
        checkAlertDialog()
        Thread.sleep(1000)
        clickOnView(positiveAlertButton)
    }

    fun checkAlertDialog() {
        waitForViewToDisplay(positiveAlertButton, 5_000L)
        onView(positiveAlertButton)
            .check(matches(withText(android.R.string.ok)))
    }

    fun waitForLoading(mayMissView: Boolean = false) {
        waitForViewToDisplay(loadingDialog, 2_500L, mayMissView)
        waitForViewToNotDisplay(loadingDialog, 10_000L)
    }
}
