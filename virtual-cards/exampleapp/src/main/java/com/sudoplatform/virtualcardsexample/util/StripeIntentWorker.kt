/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.util

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.stripe.android.ApiResultCallback
import com.stripe.android.SetupIntentResult
import com.stripe.android.Stripe
import com.stripe.android.confirmSetupIntent
import com.stripe.android.core.exception.StripeException
import com.stripe.android.model.Address
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.sudoplatform.sudovirtualcards.SudoVirtualCardsClient
import com.sudoplatform.sudovirtualcards.types.ProviderCompletionData
import com.sudoplatform.sudovirtualcards.types.StripeCardProviderCompletionData
import com.sudoplatform.sudovirtualcards.types.inputs.CreditCardFundingSourceInput
import com.sudoplatform.sudovirtualcards.util.LocaleUtil
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Utility worker encapsulating the functionality to perform processing of payment setup
 * confirmation.
 */
internal class StripeIntentWorker(
    private val context: Context,
    private val stripeClient: Stripe,
    private val activityResultHandler: ActivityResultHandler,
) {
    /**
     * Processes the payment setup confirmation to return the data needed to complete
     * the funding source creation process.
     *
     * @param input [CreditCardFundingSourceInput] The credit card input required to build the card
     *  and billing details.
     * @param clientSecret [String] The client secret from the provisional funding source
     *  provisioning data.
     */
    suspend fun confirmSetupIntent(
        input: CreditCardFundingSourceInput,
        clientSecret: String,
        callingFragment: Fragment,
    ): ProviderCompletionData {
        // Build card details
        val cardDetails = PaymentMethodCreateParams.Card.Builder()
            .setNumber(input.cardNumber)
            .setExpiryMonth(input.expirationMonth)
            .setExpiryYear(input.expirationYear)
            .setCvc(input.securityCode)
            .build()
        // Build billing details
        val billingDetails = PaymentMethod.BillingDetails.Builder()
            .setAddress(
                Address.Builder()
                    .setLine1(input.addressLine1)
                    .setLine2(input.addressLine2)
                    .setCity(input.city)
                    .setState(input.state)
                    .setPostalCode(input.postalCode)
                    .setCountry(ensureAlpha2CountryCode(context, input.country))
                    .build(),
            )
            .build()
        // Confirm setup
        val cardParams = PaymentMethodCreateParams.create(cardDetails, billingDetails)
        val confirmParams = ConfirmSetupIntentParams.create(cardParams, clientSecret)
        var setupIntent =
            try {
                stripeClient.confirmSetupIntent(confirmParams)
            } catch (e: StripeException) {
                throw SudoVirtualCardsClient.FundingSourceException.FailedException(e.message)
            }
        // Check the status of the setup intent. If more work is required, do it
        if (setupIntent.requiresAction()) {
            setupIntent = handleSetupIntentNextAction(stripeClient, clientSecret, callingFragment)
        }
        // Return completion data
        setupIntent.paymentMethodId?.let {
            return StripeCardProviderCompletionData(paymentMethod = it)
        }
        throw SudoVirtualCardsClient.FundingSourceException.FailedException()
    }

    /**
     * Parses the [countryCode] and ensures that it is of a ISO-3166 Alpha-2 format.
     *
     * @param countryCode [String] The country code to parse.
     */
    private fun ensureAlpha2CountryCode(context: Context, countryCode: String): String {
        if (countryCode.trim().length != 3) {
            return countryCode.trim()
        }
        return LocaleUtil.toCountryCodeAlpha2(context, countryCode)
            ?: countryCode
    }

    /**
     * This will be called if there is a next action that should be performed by the stripe SDK. This is for the 3D Secure Flow that
     * maybe be invoked by the SDK itself. Unfortunately as none of this is exposed we must leak activity handling.
     *
     * https://docs.stripe.com/payments/3d-secure/authentication-flow
     */
    private suspend fun handleSetupIntentNextAction(
        stripeClient: Stripe,
        clientSecret: String,
        callingFragment: Fragment,
    ): SetupIntent {
        val intentResultKey = UUID.randomUUID().toString()
        try {
            // Call the stripe client to handle the next action. This will kick off the next action (3DS flow).
            stripeClient.handleNextActionForSetupIntent(callingFragment, clientSecret)

            // Wrap our activity result handle in a suspending coroutine and wait for a result.
            // Note: The calling fragment that kicked off this core layer work should override onActivityResult and notify
            // the handler of any received results.
            val setupIntentResult: SetupIntentResult = suspendCoroutine {
                activityResultHandler.addListener(
                    intentResultKey,
                    object : ActivityResultHandler.ActivityResultListener {
                        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
                            val isSetupResult = stripeClient.isSetupResult(requestCode, data)
                            if (isSetupResult) {
                                stripeClient.onSetupResult(
                                    requestCode,
                                    data,
                                    object : ApiResultCallback<SetupIntentResult> {
                                        override fun onError(e: Exception) {
                                            it.resumeWithException(e)
                                        }

                                        override fun onSuccess(result: SetupIntentResult) {
                                            // Close, Success and Failed will all result in this being called.
                                            when (result.intent.status) {
                                                StripeIntent.Status.Succeeded -> {
                                                    it.resume(result)
                                                }
                                                StripeIntent.Status.RequiresAction -> {
                                                    it.resumeWithException(SudoVirtualCardsClient.FundingSourceException.FailedException("User cancelled 3DS flow"))
                                                }
                                                else -> {
                                                    it.resumeWithException(SudoVirtualCardsClient.FundingSourceException.FailedException("3DS flow was unsuccessful"))
                                                }
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    },
                )
            }
            return setupIntentResult.intent
        } finally {
            // Clean up the activity result handler by removing the listener
            activityResultHandler.removeListener(intentResultKey)
        }
    }
}
