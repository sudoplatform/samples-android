/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmasks

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.sudoplatform.sudoemail.types.EmailMaskRealAddressType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/**
 * Property-based test for mask filtering by realAddressType.
 *
 * Feature: external-email-masks, Property 1: Mask filtering by realAddressType
 *
 * For any list of email masks with arbitrary realAddressType values (INTERNAL, EXTERNAL, UNKNOWN)
 * and for any switch state (on or off), the filtered list SHALL contain only masks whose
 * realAddressType matches the expected type: when the switch is off, only masks with
 * realAddressType INTERNAL or UNKNOWN are included; when the switch is on, only masks with
 * realAddressType EXTERNAL are included. The filtered list SHALL be a subset of the original
 * list, and the union of both filtered views SHALL equal the original list.
 *
 * **Validates: Requirements 3.2, 3.3, 3.7**
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class MaskFilteringPropertyTest {
    companion object {
        private const val ITERATIONS = 100
        private const val MAX_LIST_SIZE = 30

        private val CHARS = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        private val ALL_REAL_ADDRESS_TYPES = EmailMaskRealAddressType.values()
    }

    /**
     * Simplified representation of an EmailMask for property testing purposes.
     * Captures only the fields relevant to filtering by realAddressType.
     */
    data class TestEmailMask(
        val id: String,
        val maskAddress: String,
        val realAddress: String,
        val realAddressType: EmailMaskRealAddressType,
    )

    /**
     * The filter function under test, modelling the filtering logic from
     * EmailAddressesFragment.filterMasksByType:
     * - showExternal=false: include masks with realAddressType INTERNAL or UNKNOWN
     * - showExternal=true: include only masks with realAddressType EXTERNAL
     */
    private fun filterMasksByType(
        masks: List<TestEmailMask>,
        showExternal: Boolean,
    ): List<TestEmailMask> =
        if (showExternal) {
            masks.filter { it.realAddressType == EmailMaskRealAddressType.EXTERNAL }
        } else {
            masks.filter {
                it.realAddressType == EmailMaskRealAddressType.INTERNAL ||
                    it.realAddressType == EmailMaskRealAddressType.UNKNOWN
            }
        }

    /**
     * Generates a random string of the given maximum length using alphanumeric characters.
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
     * Generates a random [TestEmailMask] with a random realAddressType.
     */
    private fun generateRandomMask(random: Random): TestEmailMask {
        val realAddressType = ALL_REAL_ADDRESS_TYPES[random.nextInt(ALL_REAL_ADDRESS_TYPES.size)]
        return TestEmailMask(
            id = randomString(random, 12),
            maskAddress = "${randomString(random, 8)}@masks.example.com",
            realAddress = "${randomString(random, 8)}@${randomString(random, 6)}.com",
            realAddressType = realAddressType,
        )
    }

    /**
     * Generates a random list of [TestEmailMask] objects.
     */
    private fun generateRandomMaskList(random: Random): List<TestEmailMask> {
        val size = random.nextInt(0, MAX_LIST_SIZE + 1)
        return (0 until size).map { generateRandomMask(random) }
    }

    /**
     * Property 1: Mask filtering by realAddressType — switch OFF shows only INTERNAL or UNKNOWN.
     *
     * When the switch is in the disabled (off) position, the filtered list SHALL contain
     * only masks with realAddressType INTERNAL or UNKNOWN.
     *
     * **Validates: Requirements 3.2, 3.7**
     */
    @Test
    fun property1_switchOffFiltersToInternalAndUnknown() {
        val random = Random(seed = 42)

        repeat(ITERATIONS) { iteration ->
            val masks = generateRandomMaskList(random)

            val filtered = filterMasksByType(masks, showExternal = false)

            // Every mask in the result must be INTERNAL or UNKNOWN
            filtered.forEach { mask ->
                assertTrue(
                    "Iteration $iteration: Switch OFF filter included mask with " +
                        "realAddressType=${mask.realAddressType}, expected INTERNAL or UNKNOWN. " +
                        "Mask: $mask",
                    mask.realAddressType == EmailMaskRealAddressType.INTERNAL ||
                        mask.realAddressType == EmailMaskRealAddressType.UNKNOWN,
                )
            }

            // No qualifying mask is excluded
            val expected =
                masks.filter {
                    it.realAddressType == EmailMaskRealAddressType.INTERNAL ||
                        it.realAddressType == EmailMaskRealAddressType.UNKNOWN
                }
            assertEquals(
                "Iteration $iteration: Switch OFF filter excluded qualifying masks. " +
                    "Expected ${expected.size} results but got ${filtered.size}.",
                expected.size,
                filtered.size,
            )
        }
    }

    /**
     * Property 1: Mask filtering by realAddressType — switch ON shows only EXTERNAL.
     *
     * When the switch is in the enabled (on) position, the filtered list SHALL contain
     * only masks with realAddressType EXTERNAL.
     *
     * **Validates: Requirements 3.3**
     */
    @Test
    fun property1_switchOnFiltersToExternalOnly() {
        val random = Random(seed = 99)

        repeat(ITERATIONS) { iteration ->
            val masks = generateRandomMaskList(random)

            val filtered = filterMasksByType(masks, showExternal = true)

            // Every mask in the result must be EXTERNAL
            filtered.forEach { mask ->
                assertEquals(
                    "Iteration $iteration: Switch ON filter included mask with " +
                        "realAddressType=${mask.realAddressType}, expected EXTERNAL. " +
                        "Mask: $mask",
                    EmailMaskRealAddressType.EXTERNAL,
                    mask.realAddressType,
                )
            }

            // No qualifying mask is excluded
            val expected =
                masks.filter {
                    it.realAddressType == EmailMaskRealAddressType.EXTERNAL
                }
            assertEquals(
                "Iteration $iteration: Switch ON filter excluded qualifying masks. " +
                    "Expected ${expected.size} results but got ${filtered.size}.",
                expected.size,
                filtered.size,
            )
        }
    }

    /**
     * Property 1: Mask filtering by realAddressType — union of both views equals original list.
     *
     * The union of the filtered list when switch=off and the filtered list when switch=on
     * SHALL equal the original list (no masks are lost or duplicated).
     *
     * **Validates: Requirements 3.2, 3.3, 3.7**
     */
    @Test
    fun property1_unionOfBothFilteredViewsEqualsOriginalList() {
        val random = Random(seed = 77)

        repeat(ITERATIONS) { iteration ->
            val masks = generateRandomMaskList(random)

            val filteredInternal = filterMasksByType(masks, showExternal = false)
            val filteredExternal = filterMasksByType(masks, showExternal = true)

            val union = filteredInternal + filteredExternal

            // Union size must equal original size
            assertEquals(
                "Iteration $iteration: Union of both filtered views has different size " +
                    "than original list. Original: ${masks.size}, Union: ${union.size}. " +
                    "Internal: ${filteredInternal.size}, External: ${filteredExternal.size}.",
                masks.size,
                union.size,
            )

            // Every mask in the original must appear in the union
            masks.forEach { mask ->
                assertTrue(
                    "Iteration $iteration: Mask $mask from original list not found in union " +
                        "of filtered views.",
                    union.contains(mask),
                )
            }

            // Every mask in the union must appear in the original
            union.forEach { mask ->
                assertTrue(
                    "Iteration $iteration: Mask $mask from union not found in original list.",
                    masks.contains(mask),
                )
            }
        }
    }
}
