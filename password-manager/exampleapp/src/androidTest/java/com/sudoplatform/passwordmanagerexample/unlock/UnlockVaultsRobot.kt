/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.unlock

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.sudoplatform.passwordmanagerexample.BaseRobot
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.loadSecretCode
import java.lang.AssertionError
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun unlock(func: UnlockVaultsRobot.() -> Unit) = UnlockVaultsRobot().apply { func() }

const val MASTER_PASSWORD = "Slartibartfast was here!"

/**
 * Testing robot that manages the unlock vaults screen.
 *
 * @since 2020-10-13
 */
class UnlockVaultsRobot : BaseRobot() {

    private val topText = withId(R.id.topText)
    private val bottomText = withId(R.id.bottomText)
    private val positiveAlertButton = withId(android.R.id.button1)
    private val neutralAlertButton = withId(android.R.id.button3)
    private val alertMessage = withId(android.R.id.message)
    private val loadingDialog = withId(R.id.progressBar)

    fun checkCreateMasterPasswordItemsDisplayed(timeout: Long) {
        waitForViewToDisplay(topText, timeout)
        waitForViewToDisplay(bottomText, timeout)
    }

    fun checkEnterMasterPasswordItemsDisplayed(timeout: Long) {
        waitForViewToDisplay(topText, timeout)
    }

    fun clickPositiveAlertDialogButton() {
        checkAlertDialog()
        clickOnView(positiveAlertButton)
    }

    fun clickOnNotNow() {
        waitForViewToDisplay(neutralAlertButton, 5_000L)
        waitForViewToDisplay(alertMessage, 5_000L)
        onView(neutralAlertButton)
            .check(matches(withText(R.string.not_now)))
        clickOnView(neutralAlertButton)
    }

    fun createMasterPassword() {
        replaceText(topText, MASTER_PASSWORD)
        replaceText(bottomText, MASTER_PASSWORD)
        clickOnNotNow()
        waitForLoading()
    }

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 2_500L)
        waitForViewToNotDisplay(loadingDialog, 2_500L)
    }

    fun waitForPasswordView() {
        waitForViewToDisplay(topText, 5_000L)
    }

    fun enterMasterPassword() {
        fillText(topText, MASTER_PASSWORD)
        waitForLoading()
    }

    fun enterMasterPasswordAndEmptyConfirmation() = runBlocking {
        replaceText(topText, MASTER_PASSWORD)
        onView(bottomText)
            .perform(ViewActions.click())
            .perform(ViewActions.clearText())
            .perform(ViewActions.pressImeActionButton())
        delay(500L)
    }

    fun checkMissingConfirmationAlertDialog() {
        checkAlertDialog()
        onView(alertMessage)
            .check(matches(withText(R.string.enter_master_password_error)))
    }

    fun enterEmptyMasterPasswordAndConfirmation() = runBlocking {
        onView(topText)
            .perform(ViewActions.click())
            .perform(ViewActions.clearText())
        fillText(bottomText, MASTER_PASSWORD)
        delay(500L)
    }

    fun checkMissingPasswordAlertDialog() {
        checkAlertDialog()
        onView(alertMessage)
            .check(matches(withText(R.string.enter_master_password_error)))
    }

    fun enterMismatchingMasterPasswordAndConfirmation() = runBlocking {
        replaceText(topText, MASTER_PASSWORD)
        replaceText(bottomText, UUID.randomUUID().toString())
        delay(500L)
    }

    fun checkMismatchingConfirmationAlertDialog() {
        checkAlertDialog()
        onView(alertMessage)
            .check(matches(withText(R.string.master_password_mismatch_error)))
    }

    fun enterMasterPasswordAndSecretCode() {
        replaceText(bottomText, MASTER_PASSWORD)
        val secretCode = loadSecretCode()
            ?: throw AssertionError("Secret code not saved to disc")
        replaceText(topText, secretCode)
        waitForLoading()
    }

    fun checkAlertDialog() {
        waitForViewToDisplay(positiveAlertButton, 5_000L)
        waitForViewToDisplay(alertMessage, 5_000L)
        onView(positiveAlertButton)
            .check(matches(withText(android.R.string.ok)))
    }
}
