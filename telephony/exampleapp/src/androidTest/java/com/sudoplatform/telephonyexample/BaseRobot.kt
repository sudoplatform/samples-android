/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice

/**
 * Base class of the visual testing robots.
 *
 * Operations against the platform are slow, so waits default to a generous timeout.
 */
@OptIn(ExperimentalTestApi::class)
open class BaseRobot(
    protected val composeRule: ComposeTestRule,
) {
    companion object {
        const val DEFAULT_TIMEOUT_MS = 60_000L
    }

    fun waitForTag(
        tag: String,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    ) {
        composeRule.waitUntilAtLeastOneExists(hasTestTag(tag), timeoutMs)
    }

    fun waitForText(
        text: String,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    ) {
        composeRule.waitUntilAtLeastOneExists(hasText(text), timeoutMs)
    }

    fun waitForTagToDisappear(
        tag: String,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    ) {
        composeRule.waitUntilDoesNotExist(hasTestTag(tag), timeoutMs)
    }

    fun clickOnTag(tag: String) {
        waitForTag(tag)
        composeRule.onNodeWithTag(tag).performClick()
    }

    fun clickOnText(text: String) {
        waitForText(text)
        composeRule.onNodeWithText(text).performClick()
    }

    fun pressBack() {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
    }
}
