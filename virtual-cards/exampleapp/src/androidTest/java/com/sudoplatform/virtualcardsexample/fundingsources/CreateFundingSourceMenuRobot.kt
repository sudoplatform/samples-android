/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.fundingsources

import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sudoplatform.virtualcardsexample.BaseRobot
import com.sudoplatform.virtualcardsexample.R

fun createFundingSourceMenu(func: CreateFundingSourceMenuRobot.() -> Unit) = CreateFundingSourceMenuRobot().apply { func() }

/**
 * Testing robot that manages the Create Funding Source Menu screen.
 */
class CreateFundingSourceMenuRobot : BaseRobot() {

    private val addStripeCreditCardButton = withId(R.id.createStripeCardButton)
    private val addCheckoutBankAccountButton = withId(R.id.createCheckoutBankAccountButton)
    private val learnMoreTextView = withId(R.id.learnMoreTextView)
    private val learnMoreButton = withId(R.id.learnMoreButton)

    fun checkMenuItemsDisplayed(timeout: Long = 1000L) {
        waitForViewToDisplay(addStripeCreditCardButton, timeout)
        waitForViewToDisplay(addCheckoutBankAccountButton, timeout)
        waitForViewToDisplay(learnMoreButton, timeout)
        waitForViewToDisplay(learnMoreTextView, timeout)
    }

    fun navigateToAddStripeCreditCardScreen() {
        clickOnView(addStripeCreditCardButton)
        createFundingSource {
            checkCreateCreditCardFundingSourceItemsDisplayed()
        }
    }

    fun navigateToAddCheckoutBankAccountScreen() {
        clickOnView(addCheckoutBankAccountButton)
        createFundingSource {
            checkCreateBankAccountFundingSourceItemsDisplayed()
        }
    }
}
