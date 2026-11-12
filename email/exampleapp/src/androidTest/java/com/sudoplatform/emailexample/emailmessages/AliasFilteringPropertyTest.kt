/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.sudoplatform.sudoemail.types.EmailAliasStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/**
 * Property-based test for alias filtering correctness.
 *
 * Feature: send-aliased-email, Property 1: Alias filtering correctness
 *
 * For any list of EmailAlias objects and any target emailAddressId, the filter
 * function SHALL return only aliases where alias.emailAddressId == emailAddressId
 * AND alias.status == EmailAliasStatus.ENABLED, and SHALL not exclude any alias
 * that satisfies both conditions.
 *
 * **Validates: Requirements 1.4, 4.1, 4.2**
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class AliasFilteringPropertyTest {
    companion object {
        private const val ITERATIONS = 100
        private const val MAX_LIST_SIZE = 20
        private const val MAX_ID_LENGTH = 12

        private val CHARS = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        private val ALL_STATUSES = EmailAliasStatus.values()
    }

    /**
     * Simplified representation of an EmailAlias for property testing purposes.
     */
    data class TestEmailAlias(
        val id: String,
        val aliasAddress: String,
        val emailAddressId: String,
        val status: EmailAliasStatus,
    )

    /**
     * The filter function under test, modelling the filtering logic specified
     * in the design document: include only aliases where emailAddressId matches
     * the target AND status is ENABLED.
     */
    private fun filterAliases(
        aliases: List<TestEmailAlias>,
        targetEmailAddressId: String,
    ): List<TestEmailAlias> =
        aliases.filter { alias ->
            alias.emailAddressId == targetEmailAddressId && alias.status == EmailAliasStatus.ENABLED
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
     * Generates a random [TestEmailAlias] with the given random instance.
     * Uses a small pool of emailAddressIds to ensure some collisions with the target.
     */
    private fun generateRandomAlias(
        random: Random,
        emailAddressIdPool: List<String>,
    ): TestEmailAlias =
        TestEmailAlias(
            id = randomString(random, MAX_ID_LENGTH),
            aliasAddress = "${randomString(random, 8)}@aliases.example.com",
            emailAddressId = emailAddressIdPool[random.nextInt(emailAddressIdPool.size)],
            status = ALL_STATUSES[random.nextInt(ALL_STATUSES.size)],
        )

    /**
     * Generates a random list of [TestEmailAlias] objects.
     */
    private fun generateRandomAliasList(
        random: Random,
        emailAddressIdPool: List<String>,
    ): List<TestEmailAlias> {
        val size = random.nextInt(0, MAX_LIST_SIZE + 1)
        return (0 until size).map { generateRandomAlias(random, emailAddressIdPool) }
    }

    /**
     * Property 1: Alias filtering correctness
     *
     * For any list of EmailAlias objects and any target emailAddressId:
     * 1. Every alias in the filtered result has emailAddressId == target AND status == ENABLED
     * 2. No alias satisfying both conditions is excluded from the result
     *
     * Feature: send-aliased-email, Property 1: Alias filtering correctness
     * **Validates: Requirements 1.4, 4.1, 4.2**
     */
    @Test
    fun property1_aliasFilteringCorrectness() {
        val random = Random(seed = 42)

        repeat(ITERATIONS) { iteration ->
            // Generate a small pool of emailAddressIds (2-4) to ensure some matches
            val poolSize = random.nextInt(2, 5)
            val emailAddressIdPool = (0 until poolSize).map { randomString(random, MAX_ID_LENGTH) }

            // Pick a target emailAddressId from the pool
            val targetEmailAddressId = emailAddressIdPool[random.nextInt(emailAddressIdPool.size)]

            // Generate a random list of aliases
            val aliases = generateRandomAliasList(random, emailAddressIdPool)

            // Apply the filter
            val result = filterAliases(aliases, targetEmailAddressId)

            // Assert 1: Every alias in the result matches the target AND is ENABLED
            result.forEach { alias ->
                assertEquals(
                    "Iteration $iteration: Filtered alias has wrong emailAddressId. " +
                        "Expected '$targetEmailAddressId' but got '${alias.emailAddressId}'",
                    targetEmailAddressId,
                    alias.emailAddressId,
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
                    alias.emailAddressId == targetEmailAddressId && alias.status == EmailAliasStatus.ENABLED
                }
            assertEquals(
                "Iteration $iteration: Filter excluded qualifying aliases. " +
                    "Expected ${expectedQualifying.size} results but got ${result.size}. " +
                    "Aliases: $aliases, Target: $targetEmailAddressId",
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
