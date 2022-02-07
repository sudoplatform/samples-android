/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.connection

import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.direlayexample.BaseRobot
import com.sudoplatform.direlayexample.R
import com.sudoplatform.direlayexample.connectiondetails.connectionDetails
import com.sudoplatform.direlayexample.establishconnection.connectionOptions
import com.sudoplatform.direlayexample.establishconnection.invite
import com.sudoplatform.direlayexample.establishconnection.scanInvitation
import com.sudoplatform.direlayexample.postboxes.postboxes

fun connection(func: ConnectionRobot.() -> Unit) = ConnectionRobot().apply { func() }

/**
 * Testing robot that manages the Connection screen.
 *
 * @since 2021-06-29
 */
class ConnectionRobot : BaseRobot() {

    private val sendMessageButton = withId(R.id.sendButton)
    private val loadingDialog = withId(R.id.progressBar3)
    private val messageItem = withId(R.id.messageBody)
    private val detailsButton = withId(R.id.details)
    private val messageInput = withId(R.id.composeMessageEditText)

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 5_000L)
        waitForViewToNotDisplay(loadingDialog, 60_000L)
    }

    fun checkConnectionItemsDisplayed() {
        waitForViewToDisplay(sendMessageButton, 20_000L)
        waitForViewToDisplay(detailsButton, 20_000L)
        waitForViewToDisplay(messageInput)
    }

    fun checkMessageItemDisplayed() {
        waitForViewToDisplay(messageItem, 10_000L)
        println("displayed message!")
    }

    fun createPostboxAndConnectFlow() {
        var invitation = ""
        invite {
            postboxInviteFlow()
            invitation = getInvitationText()
        }

        postboxes {
            pressBackUntilPostboxesDisplayed()
            createPostbox()
            clickOnPostbox(1)
        }

        connectionOptions {
            checkConnectionOptionsItemsDisplayed()
            navigateToScanInvitationScreen()
        }

        scanInvitation {
            setInputInvitationField(invitation)
            clickConnectButton()
        }

        connection {
            checkConnectionItemsDisplayed()
            waitForLoading()
        }
    }

    fun navigateToConnectionDetails() {
        clickOnView(detailsButton)
        connectionDetails {
            checkConnectionDetailsItemsDisplayed()
        }
    }

    fun sendMessage(message: String) {
        fillText(messageInput, message)
        clickOnView(sendMessageButton)
    }
}
