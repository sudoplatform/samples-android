/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaliases

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import kotlin.random.Random

/**
 * Property-based test for expiration date formatting.
 *
 * Feature: update-email-alias, Property 4: Expiration date formatting produces consistent readable output
 *
 * **Validates: Requirements 6.3, 6.4**
 */
@RunWith(AndroidJUnit4::class)
class ExpirationDateFormattingPropertyTest {
    /**
     * Property 4: Expiration date formatting produces consistent readable output
     *
     * For any Date whose time value is greater than zero, formatExpiresAtForDisplay
     * should produce a string matching the pattern "Expires: MM/dd/yyyy" where
     * MM is 01-12, dd is 01-31, and yyyy is a 4-digit year.
     *
     * **Validates: Requirements 6.3, 6.4**
     */
    @Test
    fun property4_validDatesProduceConsistentFormattedOutput() {
        val datePattern = Regex("""^Expires: (0[1-9]|1[0-2])/(0[1-9]|[12]\d|3[01])/\d{4}$""")

        repeat(100) { iteration ->
            // Generate a random Date with time > 0
            // Use a range from 1ms to a reasonable future date (year ~2100)
            val randomTimeMs = Random.nextLong(1L, 4_102_444_800_000L) // 1ms to Jan 1, 2100
            val randomDate = Date(randomTimeMs)

            val result = EmailAliasViewHolder.formatExpiresAtForDisplay(randomDate)

            assertTrue(
                "Iteration $iteration: Expected result to match 'Expires: MM/dd/yyyy' pattern " +
                    "but got '$result' for Date(${randomDate.time})",
                datePattern.matches(result),
            )
        }
    }

    /**
     * Property 4 (edge case): Date(0) produces "Expires: -"
     *
     * **Validates: Requirements 6.4**
     */
    @Test
    fun property4_epochZeroDateProducesDash() {
        val result = EmailAliasViewHolder.formatExpiresAtForDisplay(Date(0))
        assertEquals(
            "Date(0) should produce 'Expires: -'",
            "Expires: -",
            result,
        )
    }

    /**
     * Property 4 (edge case): null produces "Expires: -"
     *
     * **Validates: Requirements 6.4**
     */
    @Test
    fun property4_nullDateProducesDash() {
        val result = EmailAliasViewHolder.formatExpiresAtForDisplay(null)
        assertEquals(
            "null should produce 'Expires: -'",
            "Expires: -",
            result,
        )
    }
}
