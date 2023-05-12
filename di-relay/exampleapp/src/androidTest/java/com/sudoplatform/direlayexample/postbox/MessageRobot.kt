/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.postbox

import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.direlayexample.BaseRobot
import com.sudoplatform.direlayexample.R

fun message(func: MessageRobot.() -> Unit) = MessageRobot().apply { func() }

/**
 * Testing robot that manages the Message screen.
 *
 * @since 2021-06-29
 */
class MessageRobot : BaseRobot() {

    private val toolbar = withId(R.id.toolbar)

    private val messageId = withId(R.id.messageId)
    private val postboxId = withId(R.id.postbox_id)
    private val messageCreatedAt = withId(R.id.messageCreatedAt)
    private val messageOwner = withId(R.id.messageOwner)
    private val sudoOwner = withId(R.id.sudoOwner)
    private val messageContents = withId(R.id.messageContents)

    fun checkMessageItemsDisplayed() {
        waitForViewToDisplay(messageId)
        waitForViewToDisplay(postboxId)
        waitForViewToDisplay(messageCreatedAt)
        waitForViewToDisplay(messageOwner)
        waitForViewToDisplay(sudoOwner)
        waitForViewToDisplay(messageContents)
        waitForViewToDisplay(toolbar)
    }
}
