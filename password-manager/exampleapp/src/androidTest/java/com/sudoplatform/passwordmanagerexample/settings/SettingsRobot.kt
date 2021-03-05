/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.settings

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.passwordmanagerexample.BaseRobot
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.sudos.sudos

fun settings(func: SettingsRobot.() -> Unit) = SettingsRobot().apply { func() }

/**
 * Testing robot that manages the settings screen.
 *
 * @since 2020-10-20
 */
class SettingsRobot : BaseRobot() {

    private val changeMasterPasswordButton = withId(R.id.changeMasterPasswordButton)
    private val secretCodeButton = withId(R.id.secretCodeButton)
    private val passwordGeneratorButton = withId(R.id.passwordGeneratorButton)
    private val lockVaultsButton = withId(R.id.lockVaultsButton)
    private val deregisterButton = withId(R.id.deregisterButton)
    private val viewEntitlementsButton = withId(R.id.viewEntitlementsButton)
    private val positiveAlertButton = withId(android.R.id.button1)
    private val negativeAlertButton = withId(android.R.id.button2)
    private val loadingDialog = withId(R.id.progressBar)

    fun clickOnChangeMasterPassword() {
        Thread.sleep(1000)
        clickOnView(changeMasterPasswordButton)
    }

    fun clickOnSecretCode() {
        Thread.sleep(1000)
        clickOnView(secretCodeButton)
    }

    fun clickOnPasswordGenerator() {
        Thread.sleep(1000)
        clickOnView(passwordGeneratorButton)
    }

    fun clickOnLockVaults() {
        Thread.sleep(1000)
        clickOnView(lockVaultsButton)
    }

    fun clickOnDeregister() {
        Thread.sleep(1000)
        clickOnView(deregisterButton)
    }

    fun clickOnViewEntitlements() {
        Thread.sleep(1000)
        clickOnView(viewEntitlementsButton)
    }

    fun navigateFromLaunchToSettings() {
        sudos {
            navigateFromLaunchToSudos()
            clickOnSettings()
        }
    }

    fun clickOnPositiveDeregisterAlertDialogButton() {
        checkDeregisterAlertDialog()
        Thread.sleep(1000)
        clickOnView(positiveAlertButton)
    }

    fun clickOnNegativeDeregisterAlertDialogButton() {
        checkDeregisterAlertDialog()
        Thread.sleep(1000)
        clickOnView(negativeAlertButton)
    }

    private fun checkDeregisterAlertDialog() {
        waitForViewToDisplay(positiveAlertButton, 5_000L)
        waitForViewToDisplay(negativeAlertButton, 5_000L)
        Espresso.onView(positiveAlertButton)
            .check(ViewAssertions.matches(ViewMatchers.withText(R.string.deregister)))
        Espresso.onView(negativeAlertButton)
            .check(ViewAssertions.matches(ViewMatchers.withText(android.R.string.cancel)))
    }

    fun waitForChangeMasterPasswordButton() {
        waitForViewToDisplay(changeMasterPasswordButton, 2_500L)
    }

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 2_500L)
        waitForViewToNotDisplay(loadingDialog, 10_000L)
    }
}
