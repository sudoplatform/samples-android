/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
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
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import junit.framework.AssertionFailedError
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matcher

/**
 * Base class of the visual testing robots.
 */
open class BaseRobot {

    // Assumes that the view is visible on screen.
    fun replaceText(matcher: Matcher<View>, text: String) {
        onView(matcher)
            .perform(click())
            .perform(replaceText(text))
            .perform(pressImeActionButton())
            .perform(closeSoftKeyboard())
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

    // Check that the recycler view has the minimum amount of items specified
    fun checkRecyclerViewHasMinimumItemAmount(matcher: Matcher<View>, minimumAmount: Int, timeout: Long = 10_000L) {
        val retryInterval = 100L // 100 ms between retries var attempts = 1
        for (x in 0..timeout step retryInterval) {
            try {
                onView(matcher).check(matches(hasMinimumChildCount(minimumAmount)))
                break
            } catch (e: NoMatchingViewException) {
                println("NoMatchingViewException Exception")
                Thread.sleep(retryInterval)
            } catch (e: AssertionFailedError) {
                println("AssertionFailedError Exception")
                Thread.sleep(retryInterval)
            }
        }
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
    fun waitForViewToDisplay(matcher: Matcher<View>, timeout: Long = 10_000L) {
        val retryInterval = 250L // 250 ms between retries var attempts = 1
        for (x in 0..timeout step retryInterval) {
            try {
                onView(matcher)
                    .check(matches(isDisplayed()))
                break
            } catch (e: NoMatchingViewException) {
                println("NoMatchingViewException Exception")
                Thread.sleep(retryInterval)
            } catch (e: AssertionFailedError) {
                println("AssertionFailedError Exception")
                Thread.sleep(retryInterval)
            }
        }
    }

    // NoMatchingViewException thrown when onView(matcher) fails to find a matching view in the ViewHierarchy.
    // AssertionFailedErrors are generally thrown when views are present in the View Hierarchy but are not visible/blocked.
    fun waitForViewToNotDisplay(matcher: Matcher<View>, timeout: Long = 10_000L) {
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
        } catch (e: NoMatchingViewException) {
        }
    }

    // Perform a delay when executing an action on a view.
    fun waitFor(delay: Long): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isRoot()
            }
            override fun getDescription(): String {
                return "wait for " + delay + "milliseconds"
            }
            override fun perform(uiController: UiController?, view: View?) {
                uiController?.loopMainThreadForAtLeast(delay)
            }
        }
    }

    fun pressBackUntilViewIsDisplayed(matcher: Matcher<View>, timeout: Long = 5_000L) {
        val retryInterval = 250L // 250 ms between retries var attempts = 1
        Thread.sleep(1_000L)
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

    // Matcher used to retrieve the text from a TextView.
    fun getTextFromTextView(matcher: ViewInteraction): String {
        var text = String()
        matcher.perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return ViewMatchers.isAssignableFrom(TextView::class.java)
            }
            override fun getDescription(): String {
                return "Get text from the view"
            }
            override fun perform(uiController: UiController, view: View) {
                val textView = view as TextView
                text = textView.text.toString()
            }
        })
        return text
    }
}
