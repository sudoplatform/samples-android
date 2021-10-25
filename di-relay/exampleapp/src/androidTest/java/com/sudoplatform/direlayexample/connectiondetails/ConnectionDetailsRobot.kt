/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.connectiondetails

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers
import com.sudoplatform.direlayexample.BaseRobot
import com.sudoplatform.direlayexample.R

fun connectionDetails(func: ConnectionDetailsRobot.() -> Unit) = ConnectionDetailsRobot().apply { func() }

/**
 * Testing robot that manages the ConnectionDetails screen.
 *
 * @since 2021-06-29
 */
class ConnectionDetailsRobot : BaseRobot() {
    private val myConnectionIdTitle = ViewMatchers.withId(R.id.connectionTitle)
    private val myConnectionId = ViewMatchers.withId(R.id.connectionId)
    private val peerConnectionIdTitle = ViewMatchers.withId(R.id.peerConnectionTitle)

    fun checkConnectionDetailsItemsDisplayed() {
        waitForViewToDisplay(myConnectionIdTitle, 5_000L)
        waitForViewToDisplay(peerConnectionIdTitle, 5_000L)
    }

    fun getMyConnectionIdField(): String {
        return getTextFromTextView(onView(myConnectionId))
    }
}
