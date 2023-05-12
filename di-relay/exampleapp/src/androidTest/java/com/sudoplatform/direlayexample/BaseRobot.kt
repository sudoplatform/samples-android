/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample

import android.view.View
import android.widget.Checkable
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
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
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.contrib.RecyclerViewActions.scrollTo
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import junit.framework.AssertionFailedError
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.isA
import org.hamcrest.core.IsNot.not

/**
 * Base class of the visual testing robots.
 *
 * @since 2021-06-29
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
    fun fillText(matcher: Matcher<View>, text: String) {
        onView(matcher)
            .perform(click())
            .perform(typeText(text))
            .perform(pressImeActionButton())
            .perform(closeSoftKeyboard())
    }

    // Matcher used to retrieve the text from a EditText.
    fun getTextFromEditTextView(matcher: ViewInteraction): String {
        var text = String()
        matcher.perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return ViewMatchers.isAssignableFrom(EditText::class.java)
            }
            override fun getDescription(): String {
                return "Get text from the view"
            }
            override fun perform(uiController: UiController, view: View) {
                val editText = view as EditText
                text = editText.text.toString()
            }
        })
        return text
    }

    fun clickOnView(matcher: Matcher<View>) {
        onView(matcher)
            .perform(click())
    }

    fun setSwitch(matcher: Matcher<View>, checked: Boolean) {
        onView(matcher)
            .perform(setChecked(checked))
    }

    fun longClickOnView(matcher: Matcher<View>) {
        onView(matcher)
            .perform(longClick())
    }

    fun scrollToView(matcher: Matcher<View>) {
        onView(matcher)
            .perform(scrollTo())
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
    fun waitForViewToDisplay(matcher: Matcher<View>, timeout: Long = 10_000L, mayMissDisplay: Boolean = false) {
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

        if (!mayMissDisplay) {
            onView(matcher)
                .check(matches(isDisplayed()))
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

    fun waitForViewToHaveItems(matcher: Matcher<View>, itemCount: Int, timeout: Long = 10_000L) {
        val retryInterval = 250L // 250 ms between retries var attempts = 1
        for (x in 0..timeout step retryInterval) {
            try {
                recyclerViewSizeMatcher(itemCount).matches(matcher)
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

    // Custom matchers
    fun setChecked(checked: Boolean) = object : ViewAction {
        val checkableViewMatcher = object : BaseMatcher<View>() {
            override fun matches(item: Any?): Boolean = isA(Checkable::class.java).matches(item)
            override fun describeTo(description: Description?) {
                description?.appendText("is Checkable instance with correct value ")
            }
        }

        override fun getConstraints(): BaseMatcher<View> = checkableViewMatcher
        override fun getDescription(): String? = null
        override fun perform(uiController: UiController?, view: View) {
            val checkableView: Checkable = view as Checkable
            checkableView.isChecked = checked
        }
    }
    fun recyclerViewSizeMatcher(matcherSize: Int): Matcher<View?> {
        return object : BoundedMatcher<View?, RecyclerView>(RecyclerView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("with list size: $matcherSize")
            }

            override fun matchesSafely(recyclerView: RecyclerView): Boolean {
                return matcherSize == recyclerView.adapter!!.itemCount
            }
        }
    }
}
