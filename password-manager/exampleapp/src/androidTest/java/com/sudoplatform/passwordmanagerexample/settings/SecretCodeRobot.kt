/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.settings

import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.sudoplatform.passwordmanagerexample.BaseRobot
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.sudos.sudos

fun secretCode(func: SecretCodeRobot.() -> Unit) = SecretCodeRobot().apply { func() }

/**
 * Testing robot that manages the secret code screen.
 *
 * @since 2020-08-03
 */
class SecretCodeRobot : BaseRobot() {

    private val secretCodeTextView = withId(R.id.textView_secretCode)
    private val copyButton = withId(R.id.button_copy)
    private val downloadButton = withId(R.id.button_download)

    fun checkSecretCodeItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(secretCodeTextView, timeout)
        waitForViewToDisplay(copyButton, timeout)
        waitForViewToDisplay(downloadButton, timeout)
    }

    fun clickOnCopy() {
        Thread.sleep(1000)
        clickOnView(copyButton)
    }

    fun navigateFromLaunchToSecretCode() {
        sudos {
            navigateFromLaunchToSudos()
            clickOnSettings()
        }
        settings {
            clickOnSecretCode()
        }
    }

    fun checkSecretCodeInClipboard() {
        onView(secretCodeTextView).check(matches(withText(getClipboardText())))
    }

    private fun getClipboardText(): String? {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clipboard: ClipboardManager = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        return clipboard.primaryClip?.getItemAt(0)?.text.toString()
    }
}
