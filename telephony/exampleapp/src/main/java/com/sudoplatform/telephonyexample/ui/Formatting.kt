/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.ui

import android.telephony.PhoneNumberUtils
import java.util.IllformedLocaleException
import java.util.Locale

/**
 * Formats a number for display. The telephony service uses E.164 (`+14155550123`), which shows
 * in national format (`(415) 555-0123`) when it belongs to [countryCode] and in international
 * format otherwise. Short codes and other numbers that can't be formatted are shown unchanged.
 *
 * @param countryCode ISO 3166-1 alpha-2 code of the country the number is shown in. Defaults to
 *  the device's country.
 */
fun formatPhoneNumber(
    number: String,
    countryCode: String = Locale.getDefault().country,
): String = PhoneNumberUtils.formatNumber(number, countryCode) ?: number

/**
 * The display name of an ISO 3166-1 alpha-2 country code, such as "United States" for `US`, or
 * the code itself if it isn't recognised.
 */
fun countryName(countryCode: String): String =
    try {
        Locale
            .Builder()
            .setRegion(countryCode)
            .build()
            .displayCountry
            .ifEmpty { countryCode }
    } catch (e: IllformedLocaleException) {
        countryCode
    }
