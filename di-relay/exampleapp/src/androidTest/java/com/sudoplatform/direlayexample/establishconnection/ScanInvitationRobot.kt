/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.establishconnection

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.direlayexample.BaseRobot
import com.sudoplatform.direlayexample.R

fun scanInvitation(func: ScanInvitationRobot.() -> Unit) = ScanInvitationRobot().apply { func() }

/**
 * Testing robot that manages the ScanInvitation screen.
 *
 * @since 2021-06-29
 */
class ScanInvitationRobot : BaseRobot() {
    private val scanner = withId(R.id.cameraContainer)
    private val connectButton = withId(R.id.connectButton)
    private val inputInvitation = withId(R.id.enterInvitationEditText)
    private val negativeAlertButton = withId(android.R.id.button2)

    fun checkScanInvitationItemsDisplayed() {
        waitForViewToDisplay(scanner)
        waitForViewToDisplay(connectButton)
        waitForViewToDisplay(inputInvitation)
    }

    fun setInputInvitationField(invitationString: String) {
        fillEditText(R.id.enterInvitationEditText, invitationString)
    }

    fun clickConnectButton() {
        clickOnView(connectButton)
    }

    fun clickOnNegativeErrorAlertDialogButton() {
        checkErrorAlertDialog()
        Thread.sleep(1_000L)
        clickOnView(negativeAlertButton)
    }

    private fun checkErrorAlertDialog() {
        waitForViewToDisplay(negativeAlertButton, 15_000L)
        onView(negativeAlertButton)
            .check(ViewAssertions.matches(ViewMatchers.withText(android.R.string.ok)))
    }
}
