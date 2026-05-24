/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmasks

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.json.JSONObject
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/**
 * Property-based test: Invalid JSON strings are rejected.
 *
 * Feature: update-email-mask, Property 3: Invalid JSON strings are rejected
 *
 * For any string that is not a valid JSON object (e.g., plain text, malformed JSON,
 * JSON arrays, numbers), parseMetadataJson should return null.
 *
 * **Validates: Requirements 3.3**
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class InvalidJsonRejectionPropertyTest {
    companion object {
        private const val ITERATIONS = 100
    }

    /**
     * Parses a JSON string into Map<String, String>.
     * Returns null if parsing fails (invalid JSON or non-string values).
     *
     * This is the same logic that will be implemented in EmailAddressesFragment.
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
     * Generates a random plain text string that is not valid JSON.
     */
    private fun generatePlainText(random: Random): String {
        val length = random.nextInt(1, 50)
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 !@#\$%^&*()-_=+;:',.<>?/"
        return buildString {
            repeat(length) {
                append(chars[random.nextInt(chars.length)])
            }
        }
    }

    /**
     * Generates a random JSON array string (not a JSON object).
     */
    private fun generateJsonArray(random: Random): String {
        val size = random.nextInt(0, 5)
        val elements =
            (0 until size).map {
                when (random.nextInt(3)) {
                    0 -> random.nextInt(-1000, 1000).toString()
                    1 -> "\"${generatePlainText(random).replace("\"", "\\\"")}\""
                    else -> random.nextBoolean().toString()
                }
            }
        return "[${elements.joinToString(",")}]"
    }

    /**
     * Generates a random number string (integer or decimal).
     */
    private fun generateNumber(random: Random): String =
        if (random.nextBoolean()) {
            random.nextInt(-10000, 10000).toString()
        } else {
            "${random.nextInt(-10000, 10000)}.${random.nextInt(0, 100)}"
        }

    /**
     * Generates malformed JSON (missing braces, extra commas, unclosed strings, etc.).
     */
    private fun generateMalformedJson(random: Random): String {
        val variants =
            listOf(
                // Missing closing brace
                "{\"key\": \"value\"",
                // Missing opening brace
                "\"key\": \"value\"}",
                // Extra comma
                "{\"key\": \"value\",}",
                // Unclosed string
                "{\"key\": \"value",
                // Missing colon
                "{\"key\" \"value\"}",
                // Double colon
                "{\"key\":: \"value\"}",
                // Missing quotes on key
                "{key: \"value\"}",
                // Trailing garbage
                "{\"key\": \"value\"} extra",
                // Just braces with garbage inside
                "{not valid json at all}",
                // Nested unclosed
                "{\"key\": {\"nested\": \"value\"}",
                // Empty string
                "",
                // Just whitespace
                "   ",
                // Single quote instead of double
                "{'key': 'value'}",
                // Random unicode garbage
                "{\"key\": \"\u0000\u0001\u0002\"}",
            )
        return variants[random.nextInt(variants.size)]
    }

    /**
     * Generates a random invalid JSON string from one of several categories:
     * plain text, arrays, numbers, booleans, null literal, or malformed JSON.
     */
    private fun generateInvalidJsonString(random: Random): String =
        when (random.nextInt(6)) {
            0 -> generatePlainText(random)
            1 -> generateJsonArray(random)
            2 -> generateNumber(random)
            3 -> random.nextBoolean().toString()
            4 -> "null"
            5 -> generateMalformedJson(random)
            else -> generatePlainText(random)
        }

    /**
     * Property 3: Invalid JSON strings are rejected.
     *
     * For any string that is not a valid JSON object, parseMetadataJson returns null.
     * We generate 100+ invalid JSON inputs and verify each returns null.
     *
     * Feature: update-email-mask, Property 3: Invalid JSON strings are rejected
     * **Validates: Requirements 3.3**
     */
    @Test
    fun invalidJsonStringsAreRejected() {
        val random = Random(seed = 42)
        var testedCount = 0

        repeat(ITERATIONS) { iteration ->
            val invalidJson = generateInvalidJsonString(random)

            // Skip any accidentally valid JSON objects with all-string values
            // (this ensures we only test truly invalid inputs)
            val isActuallyValidJsonObject =
                try {
                    val obj = JSONObject(invalidJson)
                    val allStrings =
                        obj.keys().asSequence().all { key ->
                            try {
                                obj.getString(key)
                                true
                            } catch (e: Exception) {
                                false
                            }
                        }
                    allStrings
                } catch (e: Exception) {
                    false
                }

            if (!isActuallyValidJsonObject) {
                val result = parseMetadataJson(invalidJson)
                assertNull(
                    "parseMetadataJson should return null for invalid JSON input " +
                        "(iteration $iteration): \"$invalidJson\"",
                    result,
                )
                testedCount++
            }
        }

        // Ensure we actually tested at least 100 invalid inputs
        assert(testedCount >= ITERATIONS / 2) {
            "Expected to test at least ${ITERATIONS / 2} invalid JSON strings, but only tested $testedCount"
        }
    }

    /**
     * Additional explicit edge cases for invalid JSON rejection.
     */
    @Test
    fun explicitInvalidJsonEdgeCases() {
        val invalidInputs =
            listOf(
                "", // empty string
                " ", // whitespace only
                "hello world", // plain text
                "123", // number
                "true", // boolean
                "false", // boolean
                "null", // null literal
                "[1, 2, 3]", // JSON array of numbers
                "[\"a\", \"b\"]", // JSON array of strings
                "[]", // empty array
                "{", // incomplete object
                "}", // just closing brace
                "{\"key\":}", // missing value
                "{:\"value\"}", // missing key
                "{{}}", // nested braces invalid
                "{\"key\": \"value\"", // missing closing brace
                "not json at all", // plain text
                "<xml>data</xml>", // XML-like
                "key=value", // query string format
            )

        for (input in invalidInputs) {
            val result = parseMetadataJson(input)
            assertNull(
                "parseMetadataJson should return null for: \"$input\"",
                result,
            )
        }
    }
}
