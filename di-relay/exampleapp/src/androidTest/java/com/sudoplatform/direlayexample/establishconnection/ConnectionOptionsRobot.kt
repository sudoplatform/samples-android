/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.establishconnection

import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.direlayexample.BaseRobot
import com.sudoplatform.direlayexample.R
import com.sudoplatform.direlayexample.postboxes.postboxes

fun connectionOptions(func: ConnectionOptionsRobot.() -> Unit) = ConnectionOptionsRobot().apply { func() }

/**
 * Testing robot that manages the ConnectionOptions screen.
 *
 * @since 2021-06-29
 */
class ConnectionOptionsRobot : BaseRobot() {
    private val createInvitationButton = withId(R.id.inviteButton)
    private val scanInvitationButton = withId(R.id.scanInvitationButton)

    fun connectionOptionsFlow() {
        postboxes {
            createPostboxFlow()
            clickOnPostbox(0)
        }
        checkConnectionOptionsItemsDisplayed()
    }

    fun checkConnectionOptionsItemsDisplayed() {
        waitForViewToDisplay(createInvitationButton)
        waitForViewToDisplay(scanInvitationButton)
    }

    fun navigateToInviteScreen() {
        clickOnView(createInvitationButton)
        invite {
            checkInviteItemsDisplayed()
        }
    }

    fun navigateToScanInvitationScreen() {
        clickOnView(scanInvitationButton)
        scanInvitation {
            checkScanInvitationItemsDisplayed()
        }
    }
}
