/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sitereputationexample

import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
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
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import junit.framework.AssertionFailedError
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.not

/**
 * Base class of the visual testing robots.
 *
 * @since 2020-08-02
 */
open class BaseRobot {

    // Assumes that the view is visible on screen.
    fun replaceText(matcher: Matcher<View>, text: String) {
        onView(matcher)
            .perform(click())
            .perform(replaceText(text))
            .perform(pressImeActionButton())
    }

    // Assumes that the view is visible on screen.
    fun fillEditText(resourceId: Int, text: String) {
        onView(withId(resourceId))
            .perform(click())
            .perform(typeText(text))
            .perform(pressImeActionButton())
            .perform(closeSoftKeyboard())
    }

    // Assumes that the view is visible on screen.
    fun fillText(matcher: Matcher<View>, text: String) {
        onView(matcher)
            .perform(click())
            .perform(typeText(text))
            .perform(pressImeActionButton())
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
    fun selectNavigationDrawerDestination(matcher: Matcher<View>, resourceId: Int) {
        onView(matcher)
            .perform(navigateTo(resourceId))
    }

    // Scroll to the View in the Recycler View
    fun scrollToViewInRecyclerView(recyclerViewId: Int, matcher: Matcher<View>) {
        onView(withId(recyclerViewId))
            .perform(scrollTo<ViewHolder>(matcher))
    }

    // Click on the View in the Recycler View
    fun clickViewInRecyclerView(recyclerViewId: Int, matcher: Matcher<View>) {
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
    fun waitForViewToDisplay(matcher: Matcher<View>, timeout: Long = 1000) {
        val retryInterval = 100L // 100 ms between retries var attempts = 1
        for (x in 0..timeout step retryInterval) {
            try {
                onView(matcher)
                    .check(matches(isDisplayed()))
                break
            } catch (e: NoMatchingViewException) {
                Thread.sleep(retryInterval)
            } catch (e: AssertionFailedError) {
                Thread.sleep(retryInterval)
            }
        }
    }

    // NoMatchingViewException thrown when onView(matcher) fails to find a matching view in the ViewHierarchy.
    // AssertionFailedErrors are generally thrown when views are present in the View Hierarchy but are not visible/blocked.
    fun waitForViewToNotDisplay(matcher: Matcher<View>, timeout: Long = 1000) {
        val retryInterval = 100L // 100 ms between retries var attempts = 1
        for (x in 0..timeout step retryInterval) {
            try {
                onView(matcher)
                    .check(matches(not(isDisplayed())))
                break
            } catch (e: NoMatchingViewException) {
                break
            } catch (e: AssertionFailedError) {
                Thread.sleep(retryInterval)
            }
        }
    }

    fun withTextLength(expectedLength: Int): Matcher<View?>? {
        return object : BoundedMatcher<View?, View>(View::class.java) {
            override fun matchesSafely(target: View): Boolean {
                if (target is TextView) {
                    return target.text.length == expectedLength
                } else if (target is EditText) {
                    return target.text.length == expectedLength
                }
                return false
            }

            override fun describeTo(description: Description) {
                description.appendText("with text length: ")
                description.appendValue(expectedLength)
            }
        }
    }
}
