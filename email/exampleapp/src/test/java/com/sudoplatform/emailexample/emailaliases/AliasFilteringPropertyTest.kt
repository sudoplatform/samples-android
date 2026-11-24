/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaliases

import com.sudoplatform.sudoemail.types.EmailAlias
import com.sudoplatform.sudoemail.types.EmailAliasRealAddressType
import com.sudoplatform.sudoemail.types.EmailAliasStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date
import kotlin.random.Random

/**
 * Property-based test for alias filtering by realAddressType.
 *
 * Feature: external-email-aliases, Property 1: Alias filtering by realAddressType
 *
 * For any list of email aliases with arbitrary realAddressType values (INTERNAL, EXTERNAL, UNKNOWN)
 * and for any switch state (on or off), the filtered list SHALL contain only aliases whose
 * realAddressType matches the expected type: when the switch is off, only aliases with
 * realAddressType INTERNAL or UNKNOWN are included; when the switch is on, only aliases with
 * realAddressType EXTERNAL are included. The filtered list SHALL be a subset of the original
 * list, and the union of both filtered views SHALL equal the original list.
 *
 * **Validates: Requirements 3.2, 3.3, 3.7**
 */
class AliasFilteringPropertyTest {
    companion object {
        private const val ITERATIONS = 100
        private const val MAX_LIST_SIZE = 30

        private val CHARS = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        private val ALL_REAL_ADDRESS_TYPES = EmailAliasRealAddressType.values()
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
     * Generates a random [EmailAlias] with a random realAddressType.
     */
    private fun generateRandomAlias(random: Random): EmailAlias {
        val realAddressType = ALL_REAL_ADDRESS_TYPES[random.nextInt(ALL_REAL_ADDRESS_TYPES.size)]
        return emailAlias(
            id = randomString(random, 12),
            aliasAddress = "${randomString(random, 8)}@aliases.example.com",
            realAddress = "${randomString(random, 8)}@${randomString(random, 6)}.com",
            realAddressType = realAddressType,
        )
    }

    /**
     * Builds an [EmailAlias] in which only the fields relevant to filtering vary.
     */
    private fun emailAlias(
        id: String,
        aliasAddress: String,
        realAddress: String,
        realAddressType: EmailAliasRealAddressType,
    ) = EmailAlias(
        id = id,
        owner = "owner",
        owners = emptyList(),
        identityId = "identity",
        aliasAddress = aliasAddress,
        realAddress = realAddress,
        realAddressType = realAddressType,
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
     * Generates a random list of [EmailAlias] objects.
     */
    private fun generateRandomAliasList(random: Random): List<EmailAlias> {
        val size = random.nextInt(0, MAX_LIST_SIZE + 1)
        return (0 until size).map { generateRandomAlias(random) }
    }

    /**
     * Property 1: Alias filtering by realAddressType — switch OFF shows only INTERNAL or UNKNOWN.
     *
     * When the switch is in the disabled (off) position, the filtered list SHALL contain
     * only aliases with realAddressType INTERNAL or UNKNOWN.
     *
     * **Validates: Requirements 3.2, 3.7**
     */
    @Test
    fun property1_switchOffFiltersToInternalAndUnknown() {
        val random = Random(seed = 42)

        repeat(ITERATIONS) { iteration ->
            val aliases = generateRandomAliasList(random)

            val filtered = filterAliasesByType(aliases, showExternal = false)

            // Every alias in the result must be INTERNAL or UNKNOWN
            filtered.forEach { alias ->
                assertTrue(
                    "Iteration $iteration: Switch OFF filter included alias with " +
                        "realAddressType=${alias.realAddressType}, expected INTERNAL or UNKNOWN. " +
                        "Alias: $alias",
                    alias.realAddressType == EmailAliasRealAddressType.INTERNAL ||
                        alias.realAddressType == EmailAliasRealAddressType.UNKNOWN,
                )
            }

            // No qualifying alias is excluded
            val expected =
                aliases.filter {
                    it.realAddressType == EmailAliasRealAddressType.INTERNAL ||
                        it.realAddressType == EmailAliasRealAddressType.UNKNOWN
                }
            assertEquals(
                "Iteration $iteration: Switch OFF filter excluded qualifying aliases. " +
                    "Expected ${expected.size} results but got ${filtered.size}.",
                expected.size,
                filtered.size,
            )
        }
    }

    /**
     * Property 1: Alias filtering by realAddressType — switch ON shows only EXTERNAL.
     *
     * When the switch is in the enabled (on) position, the filtered list SHALL contain
     * only aliases with realAddressType EXTERNAL.
     *
     * **Validates: Requirements 3.3**
     */
    @Test
    fun property1_switchOnFiltersToExternalOnly() {
        val random = Random(seed = 99)

        repeat(ITERATIONS) { iteration ->
            val aliases = generateRandomAliasList(random)

            val filtered = filterAliasesByType(aliases, showExternal = true)

            // Every alias in the result must be EXTERNAL
            filtered.forEach { alias ->
                assertEquals(
                    "Iteration $iteration: Switch ON filter included alias with " +
                        "realAddressType=${alias.realAddressType}, expected EXTERNAL. " +
                        "Alias: $alias",
                    EmailAliasRealAddressType.EXTERNAL,
                    alias.realAddressType,
                )
            }

            // No qualifying alias is excluded
            val expected =
                aliases.filter {
                    it.realAddressType == EmailAliasRealAddressType.EXTERNAL
                }
            assertEquals(
                "Iteration $iteration: Switch ON filter excluded qualifying aliases. " +
                    "Expected ${expected.size} results but got ${filtered.size}.",
                expected.size,
                filtered.size,
            )
        }
    }

    /**
     * Property 1: Alias filtering by realAddressType — union of both views equals original list.
     *
     * The union of the filtered list when switch=off and the filtered list when switch=on
     * SHALL equal the original list (no aliases are lost or duplicated).
     *
     * **Validates: Requirements 3.2, 3.3, 3.7**
     */
    @Test
    fun property1_unionOfBothFilteredViewsEqualsOriginalList() {
        val random = Random(seed = 77)

        repeat(ITERATIONS) { iteration ->
            val aliases = generateRandomAliasList(random)

            val filteredInternal = filterAliasesByType(aliases, showExternal = false)
            val filteredExternal = filterAliasesByType(aliases, showExternal = true)

            val union = filteredInternal + filteredExternal

            // Union size must equal original size
            assertEquals(
                "Iteration $iteration: Union of both filtered views has different size " +
                    "than original list. Original: ${aliases.size}, Union: ${union.size}. " +
                    "Internal: ${filteredInternal.size}, External: ${filteredExternal.size}.",
                aliases.size,
                union.size,
            )

            // Every alias in the original must appear in the union
            aliases.forEach { alias ->
                assertTrue(
                    "Iteration $iteration: Alias $alias from original list not found in union " +
                        "of filtered views.",
                    union.contains(alias),
                )
            }

            // Every alias in the union must appear in the original
            union.forEach { alias ->
                assertTrue(
                    "Iteration $iteration: Alias $alias from union not found in original list.",
                    aliases.contains(alias),
                )
            }
        }
    }
}
