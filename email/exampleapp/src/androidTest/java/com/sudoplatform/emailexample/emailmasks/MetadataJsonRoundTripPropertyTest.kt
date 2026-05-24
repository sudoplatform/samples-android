/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmasks

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/**
 * Property-based test for metadata JSON round-trip.
 *
 * Feature: update-email-mask, Property 2: Metadata JSON round-trip
 *
 * For any valid Map<String, String>, formatting the map to a JSON string using
 * JSONObject(map).toString() and then parsing it back with parseMetadataJson
 * should produce a map equal to the original.
 *
 * **Validates: Requirements 3.1, 6.1**
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class MetadataJsonRoundTripPropertyTest {
    companion object {
        private const val ITERATIONS = 100
        private const val MAX_MAP_SIZE = 10
        private const val MAX_KEY_LENGTH = 20
        private const val MAX_VALUE_LENGTH = 50

        /**
         * Characters used for generating random keys and values.
         * Includes alphanumeric, spaces, and common punctuation that are valid in JSON strings.
         */
        private val CHARS =
            ('a'..'z') + ('A'..'Z') + ('0'..'9') +
                listOf(
                    ' ',
                    '-',
                    '_',
                    '.',
                    '!',
                    '@',
                    '#',
                    '%',
                    '&',
                    '(',
                    ')',
                    '+',
                    '=',
                )
    }

    /**
     * Parses a JSON string into a Map<String, String>.
     * Returns null if parsing fails (invalid JSON or non-string values).
     *
     * This mirrors the implementation specified in the design document for
     * EmailAddressesFragment.parseMetadataJson.
     */
    private fun parseMetadataJson(jsonString: String): Map<String, String>? =
        try {
            val jsonObject = JSONObject(jsonString)
            val map = mutableMapOf<String, String>()
            jsonObject.keys().forEach { key ->
                map[key] = jsonObject.getString(key)
            }
            map
        } catch (e: Exception) {
            null
        }

    /**
     * Generates a random string of the given maximum length using the allowed character set.
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
     * Generates a random Map<String, String> with up to MAX_MAP_SIZE entries.
     */
    private fun generateRandomMap(random: Random): Map<String, String> {
        val size = random.nextInt(0, MAX_MAP_SIZE + 1)
        val map = mutableMapOf<String, String>()
        repeat(size) {
            val key = randomString(random, MAX_KEY_LENGTH)
            val value = randomString(random, MAX_VALUE_LENGTH)
            map[key] = value
        }
        return map
    }

    /**
     * Property 2: Metadata JSON round-trip
     *
     * For any valid Map<String, String>, formatting the map to a JSON string using
     * JSONObject(map).toString() and then parsing it back with parseMetadataJson
     * should produce a map equal to the original.
     */
    @Test
    fun testMetadataJsonRoundTrip() {
        val random = Random(seed = 42)

        repeat(ITERATIONS) { iteration ->
            val originalMap = generateRandomMap(random)

            // Format to JSON string
            val jsonString = JSONObject(originalMap as Map<*, *>).toString()

            // Parse back with parseMetadataJson
            val parsedMap = parseMetadataJson(jsonString)

            // Verify equality
            assertNotNull(
                "Iteration $iteration: parseMetadataJson returned null for valid JSON: $jsonString",
                parsedMap,
            )
            assertEquals(
                "Iteration $iteration: Round-trip failed. Original: $originalMap, JSON: $jsonString, Parsed: $parsedMap",
                originalMap,
                parsedMap,
            )
        }
    }

    /**
     * Additional edge case: empty map round-trips correctly.
     */
    @Test
    fun testEmptyMapRoundTrip() {
        val originalMap = emptyMap<String, String>()
        val jsonString = JSONObject(originalMap as Map<*, *>).toString()
        val parsedMap = parseMetadataJson(jsonString)

        assertNotNull("parseMetadataJson returned null for empty JSON object", parsedMap)
        assertEquals("Empty map round-trip failed", originalMap, parsedMap)
    }

    /**
     * Additional edge case: single entry map round-trips correctly.
     */
    @Test
    fun testSingleEntryMapRoundTrip() {
        val originalMap = mapOf("key" to "value")
        val jsonString = JSONObject(originalMap as Map<*, *>).toString()
        val parsedMap = parseMetadataJson(jsonString)

        assertNotNull("parseMetadataJson returned null for single-entry JSON", parsedMap)
        assertEquals("Single entry map round-trip failed", originalMap, parsedMap)
    }
}
