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
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date
import kotlin.random.Random

/**
 * Property-based test for alias filtering correctness.
 *
 * Feature: send-aliased-email, Property 1: Alias filtering correctness
 *
 * For any list of EmailAlias objects and any target email address, the filter
 * function SHALL return only aliases where alias.realAddress == emailAddress
 * AND alias.status == EmailAliasStatus.ENABLED, and SHALL not exclude any alias
 * that satisfies both conditions.
 *
 * **Validates: Requirements 1.4, 4.1, 4.2**
 */
class AliasFilteringPropertyTest {
    companion object {
        private const val ITERATIONS = 100
        private const val MAX_LIST_SIZE = 20
        private const val MAX_ID_LENGTH = 12

        private val CHARS = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        private val ALL_STATUSES = EmailAliasStatus.values()
    }

    /**
     * Generates a random string of the given length using alphanumeric characters.
     */
    private fun randomString(
        random: Random,
        maxLength: Int,
        minLength: Int = 1,
    ): String {
        val length = random.nextInt(minLength, maxLength + 1)
        return (1..length).map { CHARS[random.nextInt(CHARS.size)] }.joinToString("")
    }

    /**
     * Builds an [EmailAlias] in which only the fields relevant to filtering vary.
     */
    private fun emailAlias(
        id: String,
        aliasAddress: String,
        realAddress: String,
        status: EmailAliasStatus,
    ) = EmailAlias(
        id = id,
        owner = "owner",
        owners = emptyList(),
        identityId = "identity",
        aliasAddress = aliasAddress,
        realAddress = realAddress,
        realAddressType = EmailAliasRealAddressType.INTERNAL,
        status = status,
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
     * Generates a random [EmailAlias] with the given random instance.
     * Uses a small pool of real addresses to ensure some collisions with the target.
     */
    private fun generateRandomAlias(
        random: Random,
        realAddressPool: List<String>,
    ): EmailAlias =
        emailAlias(
            id = randomString(random, MAX_ID_LENGTH),
            aliasAddress = "${randomString(random, 8)}@aliases.example.com",
            realAddress = realAddressPool[random.nextInt(realAddressPool.size)],
            status = ALL_STATUSES[random.nextInt(ALL_STATUSES.size)],
        )

    /**
     * Generates a random list of [EmailAlias] objects.
     */
    private fun generateRandomAliasList(
        random: Random,
        realAddressPool: List<String>,
    ): List<EmailAlias> {
        val size = random.nextInt(0, MAX_LIST_SIZE + 1)
        return (0 until size).map { generateRandomAlias(random, realAddressPool) }
    }

    /**
     * Property 1: Alias filtering correctness
     *
     * For any list of EmailAlias objects and any target email address:
     * 1. Every alias in the filtered result has realAddress == target AND status == ENABLED
     * 2. No alias satisfying both conditions is excluded from the result
     *
     * Feature: send-aliased-email, Property 1: Alias filtering correctness
     * **Validates: Requirements 1.4, 4.1, 4.2**
     */
    @Test
    fun property1_aliasFilteringCorrectness() {
        val random = Random(seed = 42)

        repeat(ITERATIONS) { iteration ->
            // Generate a small pool of real addresses (2-4) to ensure some matches
            val poolSize = random.nextInt(2, 5)
            val realAddressPool = (0 until poolSize).map { "${randomString(random, MAX_ID_LENGTH)}@example.com" }

            // Pick a target email address from the pool
            val targetEmailAddress = realAddressPool[random.nextInt(realAddressPool.size)]

            // Generate a random list of aliases
            val aliases = generateRandomAliasList(random, realAddressPool)

            // Apply the filter
            val result = sendFromAliases(aliases, targetEmailAddress)

            // Assert 1: Every alias in the result matches the target AND is ENABLED
            result.forEach { alias ->
                assertEquals(
                    "Iteration $iteration: Filtered alias has wrong realAddress. " +
                        "Expected '$targetEmailAddress' but got '${alias.realAddress}'",
                    targetEmailAddress,
                    alias.realAddress,
                )
                assertEquals(
                    "Iteration $iteration: Filtered alias has wrong status. " +
                        "Expected ENABLED but got '${alias.status}'",
                    EmailAliasStatus.ENABLED,
                    alias.status,
                )
            }

            // Assert 2: No qualifying alias is excluded
            val expectedQualifying =
                aliases.filter { alias ->
                    alias.realAddress == targetEmailAddress && alias.status == EmailAliasStatus.ENABLED
                }
            assertEquals(
                "Iteration $iteration: Filter excluded qualifying aliases. " +
                    "Expected ${expectedQualifying.size} results but got ${result.size}. " +
                    "Aliases: $aliases, Target: $targetEmailAddress",
                expectedQualifying.size,
                result.size,
            )
            assertTrue(
                "Iteration $iteration: Filter result does not contain all qualifying aliases. " +
                    "Expected: $expectedQualifying, Got: $result",
                result.containsAll(expectedQualifying),
            )
        }
    }
}
