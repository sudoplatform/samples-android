/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.adtrackerblockerexample.exceptions

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.sudoplatform.adtrackerblockerexample.BaseRobot
import com.sudoplatform.adtrackerblockerexample.R
import com.sudoplatform.adtrackerblockerexample.register.login
import com.sudoplatform.adtrackerblockerexample.rulesets.rulesets
import com.sudoplatform.sudoadtrackerblocker.types.BlockingException
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.core.AllOf.allOf

fun exceptionsList(func: ExceptionsListRobot.() -> Unit) = ExceptionsListRobot().apply { func() }

/**
 * Testing robot that manages the Exception List screen.
 *
 * @since 2020-12-10
 */
class ExceptionsListRobot : BaseRobot() {

    private val toolbar = withId(R.id.toolbar)
    private val createExceptionButton = withId(R.id.floatingActionButton)
    private val exceptionsRecyclerView = withId(R.id.exceptionsRecyclerView)
    private val exceptionTypeIcon = withId(R.id.imageView)
    private val exceptionUrlInput = withId(R.id.exceptionUrlInput)
    private val toolbarRemoveAllButton = withId(R.id.removeAllExceptions)
    private val loadingDialog = withId(R.id.progressBar)
    private val positiveAlertButton = withId(android.R.id.button1)

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 2_000L)
        waitForViewToNotDisplay(loadingDialog, 4_000L)
    }

    fun waitForRecyclerView(timeout: Long = 10_000L) {
        waitForViewToDisplay(exceptionsRecyclerView, timeout)
    }

    fun checkExceptionsItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(toolbar, timeout)
        waitForViewToDisplay(createExceptionButton)
    }

    fun clickOnAddExceptionButton() {
        clickOnView(createExceptionButton)
    }

    fun clickOnPositiveAlertDialogButton() {
        checkAlertDialog()
        clickOnView(positiveAlertButton)
    }

    fun swipeLeftToDelete(position: Int) {
        onView(exceptionsRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<ExceptionViewHolder>(
                position,
                ViewActions.swipeLeft()
            )
        )
    }

    fun clickOn(position: Int) {
        onView(exceptionsRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<ExceptionViewHolder>(
                position,
                ViewActions.click()
            )
        )
    }

    fun navigateFromLaunchToExceptionsList() {
        login {
            try {
                waitForRegisterButton()
                clickOnRegister()
            } catch (e: NoMatchingViewException) {
                // Login screen was skipped because already logged in
            }
        }
        rulesets {
            waitForLoading(10_000L)
            waitForRecyclerView()
            checkItemsDisplayed()
            clickOnKebabMenu()
            clickOnExceptionsList()
        }
        checkExceptionsItemsDisplayed(10_000L)
    }

    fun clickOnRemoveAll() {
        clickOnView(toolbarRemoveAllButton)
    }

    private fun checkAlertDialog() {
        waitForViewToDisplay(positiveAlertButton, 5_000L)
        onView(positiveAlertButton).check(matches(withText(android.R.string.ok)))
    }

    fun enterExceptionUrl(url: String) {
        waitForViewToDisplay(exceptionUrlInput, 5_000L)
        replaceText(exceptionUrlInput, url)
        checkAlertDialog()
        clickOnView(positiveAlertButton)
    }

    fun checkEntryIsOfType(position: Int, exceptionType: BlockingException.Type) {
        scrollToViewInRecyclerView(
            R.id.exceptionsRecyclerView,
            hasDescendant(
                allOf(exceptionTypeIcon, withContentDescription(containsString(exceptionType.name)))
            )
        )
    }
}
