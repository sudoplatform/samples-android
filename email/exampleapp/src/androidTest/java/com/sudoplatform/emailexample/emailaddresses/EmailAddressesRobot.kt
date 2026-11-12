/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaddresses

import android.widget.DatePicker
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.PickerActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.sudoplatform.emailexample.BaseRobot
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.emailaliases.EmailAliasViewHolder
import com.sudoplatform.emailexample.emailaliases.provisionEmailAlias
import com.sudoplatform.emailexample.emailmessages.emailMessages
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Matchers.allOf

fun emailAddresses(func: EmailAddressesRobot.() -> Unit) = EmailAddressesRobot().apply { func() }

/**
 * Testing robot that manages the email addresses screen.
 */
class EmailAddressesRobot : BaseRobot() {
    private val createEmailAddressButton = withId(R.id.createEmailAddressButton)
    private val createEmailAliasButton = withId(R.id.createEmailAliasButton)
    private val emailAddressRecyclerView = withId(R.id.emailAddressRecyclerView)
    private val aliasesRecyclerView = withId(R.id.aliasesRecyclerView)
    private val loadingProgress = withId(R.id.progressBar)
    private val emptyAliasesStateText = withId(R.id.emptyAliasesStateText)
    private val positiveAlertButton = withId(android.R.id.button1)
    private val negativeAlertButton = withId(android.R.id.button2)
    private val neutralAlertButton = withId(android.R.id.button3)
    private val metadataText = withId(R.id.metadataText)
    private val expiresAtText = withId(R.id.expiresAtText)

    fun waitForLoading() {
        waitForViewToDisplay(loadingProgress, 5_000L)
        waitForViewToNotDisplay(loadingProgress, 60_000L)
    }

    fun checkEmailAddressesItemsDisplayed() {
        waitForViewToDisplay(createEmailAddressButton)
    }

    fun waitForRecyclerView() {
        waitForViewToDisplay(emailAddressRecyclerView, 5_000L)
    }

    fun navigateToProvisionEmailAddressScreen() {
        clickOnView(createEmailAddressButton)
        provisionEmailAddress {
            waitForLoading()
            checkProvisionEmailAddressItemsDisplayed()
        }
    }

    fun navigateToEmailMessagesScreen(position: Int) {
        clickRecyclerViewItem(position)
        emailMessages {
            waitForLoading()
            checkEmailMessagesItemsDisplayed()
        }
    }

    fun navigateToCreateEmailAliasScreen() {
        waitForViewToDisplay(createEmailAliasButton, 10_000L)
        clickOnView(createEmailAliasButton)
        provisionEmailAlias {
            checkProvisionEmailAliasItemsDisplayed()
        }
    }

    fun swipeLeftToDelete(position: Int) {
        checkRecyclerViewHasMinimumItemAmount(emailAddressRecyclerView, position + 1)
        onView(emailAddressRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<EmailAddressViewHolder>(
                position,
                ViewActions.swipeLeft(),
            ),
        )
    }

    private fun clickRecyclerViewItem(position: Int) {
        checkRecyclerViewHasMinimumItemAmount(emailAddressRecyclerView, position + 1)
        onView(emailAddressRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<EmailAddressViewHolder>(
                position,
                ViewActions.click(),
            ),
        )
    }

    // --- Email Alias management methods (integrated from EmailAliasesRobot) ---

    fun waitForAliasesRecyclerView() {
        waitForViewToDisplay(aliasesRecyclerView, 5_000L)
    }

    fun checkEmailAliasesItemsDisplayed() {
        waitForViewToDisplay(aliasesRecyclerView, 5_000L)
    }

    fun checkEmptyAliasesStateDisplayed() {
        waitForViewToDisplay(emptyAliasesStateText, 5_000L)
    }

    fun swipeLeftToDeleteAlias(position: Int) {
        checkRecyclerViewHasMinimumItemAmount(aliasesRecyclerView, position + 1)
        onView(aliasesRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<EmailAliasViewHolder>(
                position,
                ViewActions.swipeLeft(),
            ),
        )
    }

    fun clickOnAliasItem(position: Int) {
        checkRecyclerViewHasMinimumItemAmount(aliasesRecyclerView, position + 1)
        onView(aliasesRecyclerView).perform(
            RecyclerViewActions.actionOnItemAtPosition<EmailAliasViewHolder>(
                position,
                ViewActions.click(),
            ),
        )
    }

    fun checkActionDialogDisplayed() {
        waitForViewToDisplay(negativeAlertButton, 5_000L)
    }

    fun clickOnEnableButton() {
        waitForViewToDisplay(positiveAlertButton, 5_000L)
        onView(positiveAlertButton).check(matches(withText(R.string.enable)))
        clickOnView(positiveAlertButton)
    }

    fun clickOnDisableButton() {
        waitForViewToDisplay(positiveAlertButton, 5_000L)
        onView(positiveAlertButton).check(matches(withText(R.string.disable)))
        clickOnView(positiveAlertButton)
    }

    fun clickOnCancelButton() {
        waitForViewToDisplay(negativeAlertButton, 5_000L)
        clickOnView(negativeAlertButton)
    }

    fun waitForActionToComplete() {
        waitForViewToNotDisplay(loadingProgress, 60_000L)
    }

    // --- Update-related methods ---

    /** Clicks the neutral "Update" button in the action dialog. */
    fun clickOnUpdateButton() {
        waitForViewToDisplay(neutralAlertButton, 5_000L)
        onView(neutralAlertButton).check(matches(withText(R.string.update)))
        clickOnView(neutralAlertButton)
    }

    /** Verifies the update dialog is visible by checking for the metadata hint EditText. */
    fun checkUpdateDialogDisplayed() {
        waitForViewToDisplay(withHint(R.string.metadata_json_hint), 5_000L)
    }

    /** Types into the metadata EditText (identified by its hint text). */
    fun enterMetadataJson(json: String) {
        onView(withHint(R.string.metadata_json_hint))
            .perform(click())
            .perform(replaceText(json))
            .perform(closeSoftKeyboard())
    }

    /** Clicks the "Pick Date" button/label to open the DatePickerDialog. */
    fun clickPickDate() {
        onView(withText(R.string.pick_date)).perform(click())
    }

    /** Clicks the "Clear" button to reset the expiration date. */
    fun clickClearDate() {
        onView(withText(R.string.clear_date)).perform(click())
    }

    /** Selects a date in the DatePickerDialog and confirms with OK. */
    fun selectDate(
        year: Int,
        month: Int,
        day: Int,
    ) {
        onView(isAssignableFrom(DatePicker::class.java))
            .perform(PickerActions.setDate(year, month, day))
        // Click OK on the DatePickerDialog
        onView(withId(android.R.id.button1)).perform(click())
    }

    /** Clicks the "Save" positive button in the update dialog. */
    fun clickSaveButton() {
        waitForViewToDisplay(positiveAlertButton, 5_000L)
        onView(positiveAlertButton).check(matches(withText(R.string.save)))
        clickOnView(positiveAlertButton)
    }

    /** Verifies metadata text is displayed in the list item. */
    fun checkMetadataDisplayed(expectedJson: String) {
        val matcher = allOf(withId(R.id.metadataText), withText(containsString(expectedJson)))
        waitForViewToDisplay(matcher, 10_000L)
        onView(matcher).check(matches(isDisplayed()))
    }

    /** Verifies expiration text is displayed in the list item. */
    fun checkExpiresAtDisplayed(expectedText: String) {
        val matcher = allOf(withId(R.id.expiresAtText), withText(expectedText))
        waitForViewToDisplay(matcher, 10_000L)
        onView(matcher).check(matches(isDisplayed()))
    }

    /** Verifies "-" is shown for metadata in the list item. */
    fun checkDashDisplayedForMetadata() {
        val matcher = allOf(withId(R.id.metadataText), withText("Metadata (JSON): -"))
        waitForViewToDisplay(matcher, 10_000L)
        onView(matcher).check(matches(isDisplayed()))
    }

    /** Verifies "-" is shown for expiration in the list item. */
    fun checkDashDisplayedForExpiresAt() {
        val matcher = allOf(withId(R.id.expiresAtText), withText("Expires: -"))
        waitForViewToDisplay(matcher, 10_000L)
        onView(matcher).check(matches(isDisplayed()))
    }
}
