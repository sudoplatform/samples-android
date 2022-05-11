/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.documents

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.passwordmanagerexample.BaseRobot
import com.sudoplatform.passwordmanagerexample.R

fun editDocument(func: EditDocumentRobot.() -> Unit) = EditDocumentRobot().apply { func() }

private const val CREATED_AT_LENGTH = 28
private const val UPDATED_AT_LENGTH = 28

/**
 * Testing robot that manages the edit bank account screen.
 *
 * @since 2021-01-21
 */
class EditDocumentRobot : BaseRobot() {

    private val favoriteSwitch = withId(R.id.add_as_favorite_switch)
    private val documentNameField = withId(R.id.editText_documentName)
    private val contentTypeField = withId(R.id.editText_contentType)
    private val dataField = withId(R.id.editText_data)
    private val notesField = withId(R.id.editText_notes)
    private val radioGray = withId(R.id.radio_gray)
    private val radioRed = withId(R.id.radio_red)
    private val loadingDialog = withId(R.id.progressBar)
    private val saveButton = withId(R.id.save)
    private val createdAtField = withId(R.id.label_createdAt)
    private val updatedAtField = withId(R.id.label_updatedAt)

    fun checkEditDocumentItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(documentNameField, timeout)
        waitForViewToDisplay(contentTypeField, timeout)
        waitForViewToDisplay(dataField, timeout)
        waitForViewToDisplay(notesField, timeout)
    }

    fun checkDatesAreDisplayed() {
        Espresso.onView(createdAtField)
            .check(ViewAssertions.matches(withTextLength(com.sudoplatform.passwordmanagerexample.documents.CREATED_AT_LENGTH)))
        Espresso.onView(updatedAtField)
            .check(ViewAssertions.matches(withTextLength(com.sudoplatform.passwordmanagerexample.documents.UPDATED_AT_LENGTH)))
    }

    fun clickOnSave() {
        Thread.sleep(1000)
        clickOnView(saveButton)
    }

    fun scrollToTop() {
        Espresso.onView(saveButton).perform(ViewActions.scrollTo())
    }

    fun toggleFavorite() {
        clickOnView(favoriteSwitch)
    }

    fun enterDocumentName(text: String) {
        replaceText(documentNameField, text)
    }

    fun enterContentType(text: String) {
        replaceText(contentTypeField, text)
    }

    fun enterData(text: String) {
        replaceText(dataField, text)
    }

    fun enterNotes(text: String) {
        replaceText(notesField, text)
    }

    fun selectRedColor() {
        clickOnView(radioRed)
        radioRed.matches(ViewMatchers.isChecked())
        radioGray.matches(ViewMatchers.isNotChecked())
    }

    fun selectYellowColor() {
        clickOnView(withId(R.id.radio_yellow))
    }

    fun waitForLoading() {
        waitForViewToDisplay(loadingDialog, 2_500L)
        waitForViewToNotDisplay(loadingDialog, 10_000L)
    }
}
