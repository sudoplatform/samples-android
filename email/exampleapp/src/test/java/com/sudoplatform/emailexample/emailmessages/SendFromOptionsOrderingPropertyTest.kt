/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import com.sudoplatform.sudoemail.types.EmailAlias
import com.sudoplatform.sudoemail.types.EmailAliasRealAddressType
import com.sudoplatform.sudoemail.types.EmailAliasStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date
import kotlin.random.Random

/**
 * Property-based test for "send from" option ordering.
 *
 * Feature: send-aliased-email, Property 4: Send-from option ordering
 *
 * For any filtered list of email aliases, the "send from" options shall be ordered as
 * the string "None (send from email address)" at position 0, followed by the
 * aliasAddress of each filtered alias in the same order as the input list.
 *
 * **Validates: Requirements 4.3, 1.5**
 */
class SendFromOptionsOrderingPropertyTest {
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
     * The labels of the "send from" options for aliases with the given addresses, as the compose
     * screen shows them.
     *
     * @param aliasAddresses The list of alias addresses from the filtered email alias list
     * @return The ordered list of option labels
     */
    private fun buildSendFromOptions(aliasAddresses: List<String>): List<String> =
        sendFromOptions(aliasAddresses.map(::emailAlias)).map { sendFromOptionLabel(it, NONE_OPTION) }

    /**
     * Builds an enabled [EmailAlias] with the given address.
     */
    private fun emailAlias(aliasAddress: String) =
        EmailAlias(
            id = aliasAddress,
            owner = "owner",
            owners = emptyList(),
            identityId = "identity",
            aliasAddress = aliasAddress,
            realAddress = "real@example.com",
            realAddressType = EmailAliasRealAddressType.INTERNAL,
            status = EmailAliasStatus.ENABLED,
            inboundReceived = 0,
            inboundDelivered = 0,
            outboundReceived = 0,
            outboundDelivered = 0,
            spamCount = 0,
            virusCount = 0,
            expiresAt = null,
            createdAt = Date(0),
            updatedAt = Date(0),
            version = 1,
            metadata = null,
        )

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
     * Property 4: Send-from option ordering.
     *
     * For any filtered list of aliases, the options are "None (send from email address)"
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
    fun property4_sendFromOptionOrdering() {
        val random = Random(seed = 42)

        repeat(ITERATIONS) { iteration ->
            val aliasAddresses = generateRandomAliasAddresses(random)

            val options = buildSendFromOptions(aliasAddresses)

            // Total size is aliases.size + 1
            assertEquals(
                "Iteration $iteration: Expected option count to be ${aliasAddresses.size + 1} " +
                    "but got ${options.size}",
                aliasAddresses.size + 1,
                options.size,
            )

            // Position 0 is always "None (send from email address)"
            assertEquals(
                "Iteration $iteration: Expected position 0 to be '$NONE_OPTION' " +
                    "but got '${options[0]}'",
                NONE_OPTION,
                options[0],
            )

            // Positions 1..N correspond to aliasAddress values in original order
            aliasAddresses.forEachIndexed { index, address ->
                assertEquals(
                    "Iteration $iteration: Expected position ${index + 1} to be '$address' " +
                        "but got '${options[index + 1]}'",
                    address,
                    options[index + 1],
                )
            }
        }
    }

    /**
     * Edge case: empty alias list produces only the "None" option.
     */
    @Test
    fun property4_emptyAliasListProducesOnlyNoneOption() {
        val options = buildSendFromOptions(emptyList())

        assertEquals("Expected option count to be 1 for empty alias list", 1, options.size)
        assertEquals(
            "Expected only item to be '$NONE_OPTION'",
            NONE_OPTION,
            options[0],
        )
    }

    /**
     * Edge case: single alias address is placed at position 1.
     */
    @Test
    fun property4_singleAliasAddressAtPosition1() {
        val address = "alias123@example.com"
        val options = buildSendFromOptions(listOf(address))

        assertEquals("Expected option count to be 2", 2, options.size)
        assertEquals("Expected position 0 to be '$NONE_OPTION'", NONE_OPTION, options[0])
        assertEquals("Expected position 1 to be '$address'", address, options[1])
    }
}
