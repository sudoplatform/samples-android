/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaddressblocklist

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.R
import org.hamcrest.core.AllOf.allOf

fun emailAddressBlocklist(func: EmailAddressBlocklistRobot.() -> Unit) = EmailAddressBlocklistRobot().apply { func() }

/**
 * Testing robot that manages the email address blocklist screen.
 */
class EmailAddressBlocklistRobot : BaseRobot() {
    private val toolbarDeleteButton = withId(R.id.delete)
    private val blockedAddressesRecyclerView = withId(R.id.blockedAddressesRecyclerView)
    private val loadingProgress = withId(R.id.progressBar)

    fun waitForLoading() {
        waitForViewToDisplay(loadingProgress, 5_000L)
        waitForViewToNotDisplay(loadingProgress, 60_000L)
    }

    fun checkEmailAddressBlocklistItemsDisplayed() {
        waitForViewToDisplay(toolbarDeleteButton, 5_000L)
        waitForViewToDisplay(blockedAddressesRecyclerView, 5_000L)
    }

    fun clickCheckboxOnRecyclerViewItem(position: Int) {
        checkRecyclerViewHasMinimumItemAmount(blockedAddressesRecyclerView, position + 1)
        onView(blockedAddressesRecyclerView).perform(scrollToPosition<RecyclerView.ViewHolder>(position))
        val checkBoxMatcher = allOf(withId(R.id.checkbox), isDescendantOfA(blockedAddressesRecyclerView))
        onView(checkBoxMatcher).perform(click())
    }

    fun clickOnDeleteButton() {
        clickOnView(toolbarDeleteButton)
    }
}
