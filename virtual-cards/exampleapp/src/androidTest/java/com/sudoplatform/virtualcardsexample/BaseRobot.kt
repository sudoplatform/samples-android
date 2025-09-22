/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample

import android.view.View
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.NavigationViewActions.navigateTo
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.contrib.RecyclerViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import junit.framework.AssertionFailedError
import org.hamcrest.Matcher
import org.hamcrest.core.IsNot.not

/**
 * Base class of the visual testing robots.
 */
open class BaseRobot {
    // Assumes that the view is visible on screen.
    fun replaceText(
        matcher: Matcher<View>,
        text: String,
    ) {
        onView(matcher)
            .perform(click())
            .perform(replaceText(text))
            .perform(pressImeActionButton())
            .perform(closeSoftKeyboard())
    }

    // Assumes that the view is visible on screen.
    fun fillEditText(
        resourceId: Int,
        text: String,
    ) {
        onView(withId(resourceId))
            .perform(click())
            .perform(typeText(text))
            .perform(pressImeActionButton())
            .perform(closeSoftKeyboard())
    }

    // Assumes that the view is visible on screen.
    fun fillText(
        matcher: Matcher<View>,
        text: String,
    ) {
        onView(matcher)
            .perform(click())
            .perform(typeText(text))
            .perform(pressImeActionButton())
            .perform(closeSoftKeyboard())
    }

    fun clickOnView(matcher: Matcher<View>) {
        onView(matcher)
            .perform(click())
    }

    fun longClickOnView(matcher: Matcher<View>) {
        onView(matcher)
            .perform(longClick())
    }

    fun scrollToView(matcher: Matcher<View>) {
        onView(matcher)
            .perform(scrollTo())
    }

    // Use this when the method to open the Drawer does not matter.
    fun openNavigationDrawer(matcher: Matcher<View>) {
        onView(matcher)
            .perform(DrawerActions.open())
    }

    // Navigate via the drawer
    fun selectNavigationDrawerDestination(
        matcher: Matcher<View>,
        resourceId: Int,
    ) {
        onView(matcher)
            .perform(navigateTo(resourceId))
    }

    // Scroll to the View in the Recycler View
    fun scrollToViewInRecyclerView(
        recyclerViewId: Int,
        matcher: Matcher<View>,
    ) {
        onView(withId(recyclerViewId))
            .perform(scrollTo<ViewHolder>(matcher))
    }

    // Click on the View in the Recycler View
    fun clickViewInRecyclerView(
        recyclerViewId: Int,
        matcher: Matcher<View>,
    ) {
        onView(withId(recyclerViewId))
            .perform(actionOnItem<ViewHolder>(matcher, click()))
    }

    // Check view is displayed
    fun checkIsDisplayed(vararg matchers: Matcher<View>) {
        for (matcher in matchers) {
            onView(matcher)
                .check(matches(isDisplayed()))
        }
    }

    // Check view is NOT displayed
    fun checkIsNotDisplayed(vararg matchers: Matcher<View>) {
        for (matcher in matchers) {
            onView(matcher)
                .check(matches(not(isDisplayed())))
        }
    }

    // NoMatchingViewException thrown when onView(matcher) fails to find a matching view in the ViewHierarchy.
    // AssertionFailedErrors are generally thrown when views are present in the View Hierarchy but are not visible/blocked.
    fun waitForViewToDisplay(
        matcher: Matcher<View>,
        timeout: Long = 10_000L,
        mayMissDisplay: Boolean = false,
    ) {
        val retryInterval = 250L // 250 ms between retries
        val endTime = System.currentTimeMillis() + timeout

        while (System.currentTimeMillis() < endTime) {
            try {
                onView(matcher).check(matches(isDisplayed()))
                return // Success - view is displayed
            } catch (e: NoMatchingViewException) {
                // View not found yet, wait and retry
                try {
                    Thread.sleep(retryInterval)
                } catch (ie: InterruptedException) {
                    // Thread was interrupted, break the loop
                    break
                }
            } catch (e: AssertionFailedError) {
                // View found but not displayed yet, wait and retry
                try {
                    Thread.sleep(retryInterval)
                } catch (ie: InterruptedException) {
                    // Thread was interrupted, break the loop
                    break
                }
            } catch (e: Exception) {
                // Any other exception (including activity not resumed)
                if (!mayMissDisplay) {
                    // Only rethrow if view must be displayed
                    throw e
                }
                try {
                    Thread.sleep(retryInterval)
                } catch (ie: InterruptedException) {
                    break
                }
            }
        }

        // Timeout occurred, perform final check if view must be displayed
        if (!mayMissDisplay) {
            try {
                onView(matcher).check(matches(isDisplayed()))
            } catch (e: NoMatchingViewException) {
                throw AssertionError("View with matcher $matcher not found after waiting $timeout ms", e)
            }
        }
    }

    // NoMatchingViewException thrown when onView(matcher) fails to find a matching view in the ViewHierarchy.
    // AssertionFailedErrors are generally thrown when views are present in the View Hierarchy but are not visible/blocked.
    fun waitForViewToNotDisplay(
        matcher: Matcher<View>,
        timeout: Long = 10_000L,
    ) {
        val retryInterval = 250L // 250 ms between retries var attempts = 1
        for (x in 0..timeout step retryInterval) {
            try {
                onView(matcher)
                    .check(matches(not(isDisplayed())))
                return
            } catch (e: NoMatchingViewException) {
                return
            } catch (e: AssertionFailedError) {
                println("AssertionFailedError Exception")
                Thread.sleep(retryInterval)
            }
        }

        try {
            onView(matcher)
                .check(matches(not(isDisplayed())))
        } catch (_: NoMatchingViewException) {
        }
    }

    fun pressBackUntilViewIsDisplayed(
        matcher: Matcher<View>,
        timeout: Long = 5000,
    ) {
        val retryInterval = 250L // 250 ms between retries var attempts = 1
        Thread.sleep(1000)
        for (x in 0..timeout step retryInterval) {
            try {
                onView(matcher).check(matches(isDisplayed()))
                return
            } catch (e: NoMatchingViewException) {
                Thread.sleep(retryInterval)
                pressBack()
            }
        }
        onView(matcher).check(matches(isDisplayed()))
    }
}
