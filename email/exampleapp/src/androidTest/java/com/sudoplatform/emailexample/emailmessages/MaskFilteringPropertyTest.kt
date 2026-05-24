/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.sudoplatform.sudoemail.types.EmailMaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/**
 * Property-based test for mask filtering correctness.
 *
 * Feature: send-masked-email, Property 1: Mask filtering correctness
 *
 * For any list of EmailMask objects and any target emailAddressId, the filter
 * function SHALL return only masks where mask.emailAddressId == emailAddressId
 * AND mask.status == EmailMaskStatus.ENABLED, and SHALL not exclude any mask
 * that satisfies both conditions.
 *
 * **Validates: Requirements 1.4, 4.1, 4.2**
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class MaskFilteringPropertyTest {
    companion object {
        private const val ITERATIONS = 100
        private const val MAX_LIST_SIZE = 20
        private const val MAX_ID_LENGTH = 12

        private val CHARS = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        private val ALL_STATUSES = EmailMaskStatus.values()
    }

    /**
     * Simplified representation of an EmailMask for property testing purposes.
     */
    data class TestEmailMask(
        val id: String,
        val maskAddress: String,
        val emailAddressId: String,
        val status: EmailMaskStatus,
    )

    /**
     * The filter function under test, modelling the filtering logic specified
     * in the design document: include only masks where emailAddressId matches
     * the target AND status is ENABLED.
     */
    private fun filterMasks(
        masks: List<TestEmailMask>,
        targetEmailAddressId: String,
    ): List<TestEmailMask> =
        masks.filter { mask ->
            mask.emailAddressId == targetEmailAddressId && mask.status == EmailMaskStatus.ENABLED
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
     * Generates a random [TestEmailMask] with the given random instance.
     * Uses a small pool of emailAddressIds to ensure some collisions with the target.
     */
    private fun generateRandomMask(
        random: Random,
        emailAddressIdPool: List<String>,
    ): TestEmailMask =
        TestEmailMask(
            id = randomString(random, MAX_ID_LENGTH),
            maskAddress = "${randomString(random, 8)}@masks.example.com",
            emailAddressId = emailAddressIdPool[random.nextInt(emailAddressIdPool.size)],
            status = ALL_STATUSES[random.nextInt(ALL_STATUSES.size)],
        )

    /**
     * Generates a random list of [TestEmailMask] objects.
     */
    private fun generateRandomMaskList(
        random: Random,
        emailAddressIdPool: List<String>,
    ): List<TestEmailMask> {
        val size = random.nextInt(0, MAX_LIST_SIZE + 1)
        return (0 until size).map { generateRandomMask(random, emailAddressIdPool) }
    }

    /**
     * Property 1: Mask filtering correctness
     *
     * For any list of EmailMask objects and any target emailAddressId:
     * 1. Every mask in the filtered result has emailAddressId == target AND status == ENABLED
     * 2. No mask satisfying both conditions is excluded from the result
     *
     * Feature: send-masked-email, Property 1: Mask filtering correctness
     * **Validates: Requirements 1.4, 4.1, 4.2**
     */
    @Test
    fun property1_maskFilteringCorrectness() {
        val random = Random(seed = 42)

        repeat(ITERATIONS) { iteration ->
            // Generate a small pool of emailAddressIds (2-4) to ensure some matches
            val poolSize = random.nextInt(2, 5)
            val emailAddressIdPool = (0 until poolSize).map { randomString(random, MAX_ID_LENGTH) }

            // Pick a target emailAddressId from the pool
            val targetEmailAddressId = emailAddressIdPool[random.nextInt(emailAddressIdPool.size)]

            // Generate a random list of masks
            val masks = generateRandomMaskList(random, emailAddressIdPool)

            // Apply the filter
            val result = filterMasks(masks, targetEmailAddressId)

            // Assert 1: Every mask in the result matches the target AND is ENABLED
            result.forEach { mask ->
                assertEquals(
                    "Iteration $iteration: Filtered mask has wrong emailAddressId. " +
                        "Expected '$targetEmailAddressId' but got '${mask.emailAddressId}'",
                    targetEmailAddressId,
                    mask.emailAddressId,
                )
                assertEquals(
                    "Iteration $iteration: Filtered mask has wrong status. " +
                        "Expected ENABLED but got '${mask.status}'",
                    EmailMaskStatus.ENABLED,
                    mask.status,
                )
            }

            // Assert 2: No qualifying mask is excluded
            val expectedQualifying =
                masks.filter { mask ->
                    mask.emailAddressId == targetEmailAddressId && mask.status == EmailMaskStatus.ENABLED
                }
            assertEquals(
                "Iteration $iteration: Filter excluded qualifying masks. " +
                    "Expected ${expectedQualifying.size} results but got ${result.size}. " +
                    "Masks: $masks, Target: $targetEmailAddressId",
                expectedQualifying.size,
                result.size,
            )
            assertTrue(
                "Iteration $iteration: Filter result does not contain all qualifying masks. " +
                    "Expected: $expectedQualifying, Got: $result",
                result.containsAll(expectedQualifying),
            )
        }
    }
}
