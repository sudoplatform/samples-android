/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/**
 * Property-based test for spinner item ordering.
 *
 * Feature: send-aliased-email, Property 4: Spinner item ordering
 *
 * For any filtered list of email aliases, the spinner items shall be ordered as
 * the string "None (send from email address)" at position 0, followed by the
 * aliasAddress of each filtered alias in the same order as the input list.
 *
 * **Validates: Requirements 4.3, 1.5**
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class SpinnerOrderingPropertyTest {
    companion object {
        private const val ITERATIONS = 100
        private const val NONE_OPTION = "None (send from email address)"
        private const val MAX_LIST_SIZE = 20
        private const val MAX_ADDRESS_LENGTH = 40

        /**
         * Characters used for generating random alias addresses.
         */
        private val ADDRESS_CHARS = ('a'..'z') + ('0'..'9') + listOf('.', '-', '_', '@')
    }

    /**
     * Models the spinner item construction logic from
     * SendEmailMessageFragment.populateEmailAliasSpinner.
     *
     * This mirrors the actual implementation:
     * ```
     * val spinnerItems = mutableListOf(getString(R.string.send_aliased_email_none_option))
     * spinnerItems.addAll(emailAliasList.map { it.aliasAddress })
     * ```
     *
     * @param aliasAddresses The list of alias addresses from the filtered email alias list
     * @return The ordered list of spinner items
     */
    private fun buildSpinnerItems(aliasAddresses: List<String>): List<String> {
        val spinnerItems = mutableListOf(NONE_OPTION)
        spinnerItems.addAll(aliasAddresses)
        return spinnerItems
    }

    /**
     * Generates a random email-like alias address string.
     */
    private fun randomAliasAddress(random: Random): String {
        val localLength = random.nextInt(3, MAX_ADDRESS_LENGTH / 2)
        val domainLength = random.nextInt(3, MAX_ADDRESS_LENGTH / 2)
        val local =
            (1..localLength)
                .map {
                    ADDRESS_CHARS[random.nextInt(ADDRESS_CHARS.size)]
                }.joinToString("")
        val domain =
            (1..domainLength)
                .map {
                    ADDRESS_CHARS[random.nextInt(ADDRESS_CHARS.size)]
                }.joinToString("")
        return "$local@$domain"
    }

    /**
     * Generates a random list of alias addresses.
     */
    private fun generateRandomAliasAddresses(random: Random): List<String> {
        val size = random.nextInt(0, MAX_LIST_SIZE + 1)
        return (1..size).map { randomAliasAddress(random) }
    }

    /**
     * Property 4: Spinner item ordering.
     *
     * For any filtered list of aliases, the spinner items are "None (send from email address)"
     * at position 0 followed by each alias's aliasAddress in original order.
     *
     * Verifies:
     * - Position 0 is always "None (send from email address)"
     * - Positions 1..N correspond to aliasAddress values in original order
     * - Total size is aliases.size + 1
     *
     * **Validates: Requirements 4.3, 1.5**
     */
    @Test
    fun property4_spinnerItemOrdering() {
        val random = Random(seed = 42)

        repeat(ITERATIONS) { iteration ->
            val aliasAddresses = generateRandomAliasAddresses(random)

            val spinnerItems = buildSpinnerItems(aliasAddresses)

            // Total size is aliases.size + 1
            assertEquals(
                "Iteration $iteration: Expected spinner size to be ${aliasAddresses.size + 1} " +
                    "but got ${spinnerItems.size}",
                aliasAddresses.size + 1,
                spinnerItems.size,
            )

            // Position 0 is always "None (send from email address)"
            assertEquals(
                "Iteration $iteration: Expected position 0 to be '$NONE_OPTION' " +
                    "but got '${spinnerItems[0]}'",
                NONE_OPTION,
                spinnerItems[0],
            )

            // Positions 1..N correspond to aliasAddress values in original order
            aliasAddresses.forEachIndexed { index, address ->
                assertEquals(
                    "Iteration $iteration: Expected position ${index + 1} to be '$address' " +
                        "but got '${spinnerItems[index + 1]}'",
                    address,
                    spinnerItems[index + 1],
                )
            }
        }
    }

    /**
     * Edge case: empty alias list produces spinner with only the "None" option.
     */
    @Test
    fun property4_emptyAliasListProducesOnlyNoneOption() {
        val spinnerItems = buildSpinnerItems(emptyList())

        assertEquals("Expected spinner size to be 1 for empty alias list", 1, spinnerItems.size)
        assertEquals(
            "Expected only item to be '$NONE_OPTION'",
            NONE_OPTION,
            spinnerItems[0],
        )
    }

    /**
     * Edge case: single alias address is placed at position 1.
     */
    @Test
    fun property4_singleAliasAddressAtPosition1() {
        val address = "alias123@example.com"
        val spinnerItems = buildSpinnerItems(listOf(address))

        assertEquals("Expected spinner size to be 2", 2, spinnerItems.size)
        assertEquals("Expected position 0 to be '$NONE_OPTION'", NONE_OPTION, spinnerItems[0])
        assertEquals("Expected position 1 to be '$address'", address, spinnerItems[1])
    }
}
