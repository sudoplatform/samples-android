/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.establishconnection

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.direlayexample.BaseRobot
import com.sudoplatform.direlayexample.R

fun invite(func: InviteRobot.() -> Unit) = InviteRobot().apply { func() }

/**
 * Testing robot that manages the Invite screen.
 *
 * @since 2021-06-29
 */
class InviteRobot : BaseRobot() {

    private val qrCode = withId(R.id.qrCode)
    private val invitationText = withId(R.id.copyInvitationEditText)

    fun checkInviteItemsDisplayed() {
        waitForViewToDisplay(qrCode)
        waitForViewToDisplay(invitationText)
    }

    fun postboxInviteFlow() {
        connectionOptions {
            connectionOptionsFlow()
            navigateToInviteScreen()
        }
        checkInviteItemsDisplayed()
    }

    fun getInvitationText(): String {
        return getTextFromEditTextView(onView(invitationText))
    }
}
