/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample

import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.sudoplatform.emailexample.ui.TestTags

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

    /** A string from the app's resources, so robots match what the screens show. */
    fun string(
        @StringRes resId: Int,
        vararg formatArgs: Any,
    ): String =
        InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .getString(resId, *formatArgs)

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

    /**
     * Wait for a node matching [matcher].
     *
     * @param useUnmergedTree true to find a node inside a clickable row, which merges its
     *  children's semantics and drops their test tags.
     */
    fun waitForNode(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean = false,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    ) {
        composeRule.waitUntil(timeoutMs) {
            composeRule.onAllNodes(matcher, useUnmergedTree).fetchSemanticsNodes().isNotEmpty()
        }
    }

    fun waitForTagToDisappear(
        tag: String,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    ) {
        composeRule.waitUntilDoesNotExist(hasTestTag(tag), timeoutMs)
    }

    /** Wait for the control tagged [tag] to show and be enabled, such as an action waiting on a load. */
    fun waitForTagToBeEnabled(
        tag: String,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    ) {
        composeRule.waitUntilAtLeastOneExists(hasTestTag(tag) and isEnabled(), timeoutMs)
    }

    /** Wait for any loading dialog or in-place spinner to go away. */
    fun waitForLoading(timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
        waitForTagToDisappear(TestTags.LOADING, timeoutMs)
    }

    /** Wait for at least [count] nodes tagged [tag]. */
    fun waitForTagCount(
        tag: String,
        count: Int,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    ) {
        composeRule.waitUntil(timeoutMs) {
            composeRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().size >= count
        }
    }

    fun clickOnTag(tag: String) {
        waitForTag(tag)
        composeRule.onNodeWithTag(tag).performClick()
    }

    /** Click the control tagged [tag] once it is enabled. */
    fun clickOnEnabledTag(tag: String) {
        waitForTagToBeEnabled(tag)
        composeRule.onNodeWithTag(tag).performClick()
    }

    fun clickOnText(text: String) {
        waitForText(text)
        composeRule.onNodeWithText(text).performClick()
    }

    /** Click the node tagged [tag] at [position] among those currently shown, such as a list row. */
    fun clickOnTagAtPosition(
        tag: String,
        position: Int,
    ) {
        waitForTagCount(tag, position + 1)
        composeRule.onAllNodes(hasTestTag(tag))[position].performClick()
    }

    /** Swipe the node tagged [tag] at [position] to the left, as to delete a list row. */
    fun swipeLeftOnTagAtPosition(
        tag: String,
        position: Int,
    ) {
        waitForTagCount(tag, position + 1)
        composeRule.onAllNodes(hasTestTag(tag))[position].performTouchInput { swipeLeft() }
    }

    /** Replace the text in the field tagged [tag]. */
    fun replaceText(
        tag: String,
        text: String,
    ) {
        waitForTag(tag)
        composeRule.onNodeWithTag(tag).performTextClearance()
        if (text.isNotEmpty()) {
            composeRule.onNodeWithTag(tag).performTextInput(text)
        }
    }

    /** The text shown by the node tagged [tag], or the text in it if it is a field. */
    fun textOf(tag: String): String {
        waitForTag(tag)
        val config = composeRule.onNodeWithTag(tag).fetchSemanticsNode().config
        return config.getOrNull(SemanticsProperties.EditableText)?.text
            ?: config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }.orEmpty()
    }

    /** Open the dropdown field tagged [fieldTag] and choose the option showing [text]. */
    fun selectDropdownOption(
        fieldTag: String,
        text: String,
    ) {
        clickOnEnabledTag(fieldTag)
        val option = dropdownOption() and hasText(text)
        waitForNode(option)
        composeRule.onNode(option).performClick()
    }

    /** Open the dropdown field tagged [fieldTag] and choose the option at [position]. */
    fun selectDropdownOption(
        fieldTag: String,
        position: Int,
    ) {
        clickOnEnabledTag(fieldTag)
        composeRule.waitUntil(DEFAULT_TIMEOUT_MS) {
            composeRule.onAllNodes(dropdownOption()).fetchSemanticsNodes().size > position
        }
        composeRule.onAllNodes(dropdownOption())[position].performClick()
    }

    /**
     * Scroll the list tagged [listTag] until a node matching [matcher] shows. Rows further down
     * a lazy list aren't composed until they are scrolled to.
     */
    fun scrollListTo(
        listTag: String,
        matcher: SemanticsMatcher,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    ) {
        waitForNode(hasTestTag(listTag) and hasScrollToNodeAction(), timeoutMs = timeoutMs)
        composeRule.waitUntil(timeoutMs) {
            try {
                composeRule.onNodeWithTag(listTag).performScrollToNode(matcher)
                true
            } catch (e: AssertionError) {
                false
            }
        }
    }

    /** Go back with the back arrow in the screen's top bar. */
    fun navigateBack() {
        clickOnTag(TestTags.NAVIGATE_BACK)
    }

    /** Press the system back button, which also closes the keyboard or a dialog. */
    fun pressBack() {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
    }

    private fun dropdownOption() = hasAnyAncestor(isPopup()) and hasClickAction()
}
