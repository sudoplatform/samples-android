/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.transactions.transactiondetail

import android.content.Context
import com.sudoplatform.sudovirtualcards.types.DeclineReason
import com.sudoplatform.virtualcardsexample.R

/**
 * Extension mapping [DeclineReason] name to a description.
 */
fun DeclineReason.description(context: Context): String {
    return when (this) {
        DeclineReason.INSUFFICIENT_FUNDS -> context.getString(R.string.dr_insufficient_funds)
        DeclineReason.INSUFFICIENT_ENTITLEMENTS -> context.getString(
            R.string.dr_insufficient_entitlements,
        )
        DeclineReason.SUSPICIOUS -> context.getString(R.string.dr_suspcious)
        DeclineReason.CARD_STOPPED -> context.getString(R.string.dr_card_stopped)
        DeclineReason.CARD_EXPIRED -> context.getString(R.string.dr_card_expired)
        DeclineReason.MERCHANT_BLOCKED -> context.getString(R.string.dr_merchant_blocked)
        DeclineReason.MERCHANT_CODE_BLOCKED -> context.getString(R.string.dr_merchant_code_blocked)
        DeclineReason.MERCHANT_COUNTRY_BLOCKED -> context.getString(R.string.dr_merchant_country_blocked)
        DeclineReason.AVS_CHECK_FAILED -> context.getString(R.string.dr_avs_check_failed)
        DeclineReason.CSC_CHECK_FAILED -> context.getString(R.string.dr_csc_check_failed)
        DeclineReason.EXPIRY_CHECK_FAILED -> context.getString(R.string.dr_expiry_check_failed)
        DeclineReason.PROCESSING_ERROR -> context.getString(R.string.dr_processing_error)
        DeclineReason.DECLINED -> context.getString(R.string.dr_declined)
        DeclineReason.VELOCITY_EXCEEDED -> context.getString(R.string.dr_velocity_exceeded)
        DeclineReason.CURRENCY_BLOCKED -> context.getString(R.string.dr_currency_blocked)
        DeclineReason.FUNDING_ERROR -> context.getString(R.string.dr_funding_error)
        DeclineReason.SERVICE_UNAVAILABLE -> context.getString(R.string.dr_service_unavailable)
        DeclineReason.UNKNOWN -> this.name
    }
}
