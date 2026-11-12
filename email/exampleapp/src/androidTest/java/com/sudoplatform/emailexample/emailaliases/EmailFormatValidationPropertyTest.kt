/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaliases

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/**
 * Property-based test for email format validation.
 *
 * Feature: external-email-aliases, Property 2: Email format validation
 *
 * For any string input, the email validation function SHALL accept the input if and only if
 * it matches the pattern `local-part@domain` (contains exactly one `@` with non-empty local
 * part and non-empty domain containing at least one `.`), and SHALL reject all other strings.
 * Additionally, for any valid email string, its length SHALL not exceed 254 characters.
 *
 * **Validates: Requirements 5.9**
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class EmailFormatValidationPropertyTest {
    companion object {
        private const val ITERATIONS = 100
        private const val MAX_EMAIL_LENGTH = 254

        private val ALPHA_CHARS = ('a'..'z') + ('A'..'Z')
        private val ALPHANUMERIC_CHARS = ALPHA_CHARS + ('0'..'9')
        private val LOCAL_PART_CHARS = ALPHANUMERIC_CHARS + listOf('.', '_', '-', '+')
        private val DOMAIN_LABEL_CHARS = ('a'..'z') + ('0'..'9') + listOf('-')
    }

    /**
     * Instance of the fragment used to access the isValidEmail function under test.
     */
    private val fragment = ProvisionExternalEmailAliasFragment()

    /**
     * Reference implementation of email validation that mirrors the expected behavior:
     * - Contains exactly one '@'
     * - Non-empty local part (before '@')
     * - Non-empty domain (after '@') containing at least one '.'
     * - Total length <= 254 characters
     */
    private fun referenceIsValidEmail(email: String): Boolean {
        if (email.length > MAX_EMAIL_LENGTH) return false
        val atIndex = email.indexOf('@')
        if (atIndex < 0) return false
        if (email.indexOf('@', atIndex + 1) >= 0) return false
        val localPart = email.substring(0, atIndex)
        val domain = email.substring(atIndex + 1)
        if (localPart.isEmpty()) return false
        if (domain.isEmpty()) return false
        if (!domain.contains('.')) return false
        return true
    }

    /**
     * Generates a random string of the given length range using the provided character set.
     */
    private fun randomString(
        random: Random,
        chars: List<Char>,
        minLength: Int = 1,
        maxLength: Int = 10,
    ): String {
        val length = random.nextInt(minLength, maxLength + 1)
        return (1..length).map { chars[random.nextInt(chars.size)] }.joinToString("")
    }

    /**
     * Generates a valid email address that should pass validation.
     */
    private fun generateValidEmail(random: Random): String {
        val localPart = randomString(random, LOCAL_PART_CHARS, minLength = 1, maxLength = 20)
        val domainLabel = randomString(random, DOMAIN_LABEL_CHARS, minLength = 1, maxLength = 10)
        val tld = randomString(random, ALPHA_CHARS, minLength = 2, maxLength = 5)
        return "$localPart@$domainLabel.$tld"
    }

    /**
     * Generates a random string that is intentionally invalid as an email.
     * Strategies include: no '@', multiple '@', empty local part, empty domain, no dot in domain.
     */
    private fun generateInvalidEmail(random: Random): String =
        when (random.nextInt(7)) {
            0 -> "" // empty string
            1 -> "   " // whitespace only
            2 -> randomString(random, ALPHANUMERIC_CHARS, minLength = 3, maxLength = 20) // no '@'
            3 -> { // multiple '@'
                val local = randomString(random, ALPHANUMERIC_CHARS, minLength = 1, maxLength = 5)
                val mid = randomString(random, ALPHANUMERIC_CHARS, minLength = 1, maxLength = 5)
                val domain = randomString(random, ALPHANUMERIC_CHARS, minLength = 1, maxLength = 5)
                "$local@$mid@$domain.com"
            }
            4 -> { // empty local part (starts with '@')
                val domain = randomString(random, DOMAIN_LABEL_CHARS, minLength = 1, maxLength = 5)
                val tld = randomString(random, ALPHA_CHARS, minLength = 2, maxLength = 4)
                "@$domain.$tld"
            }
            5 -> { // no dot in domain
                val local = randomString(random, LOCAL_PART_CHARS, minLength = 1, maxLength = 10)
                val domain = randomString(random, ALPHA_CHARS, minLength = 3, maxLength = 10)
                "$local@$domain"
            }
            6 -> { // empty domain (ends with '@')
                val local = randomString(random, LOCAL_PART_CHARS, minLength = 1, maxLength = 10)
                "$local@"
            }
            else -> ""
        }

    /**
     * Property 2: Valid emails are accepted.
     *
     * For any randomly generated valid email (one '@', non-empty local part, non-empty domain
     * with at least one '.', length <= 254), the validation function SHALL return true.
     *
     * **Validates: Requirements 5.9**
     */
    @Test
    fun property2_validEmailsAreAccepted() {
        val random = Random(seed = 42)

        repeat(ITERATIONS) { iteration ->
            val email = generateValidEmail(random)

            assertTrue(
                "Iteration $iteration: Valid email '$email' was rejected by isValidEmail(). " +
                    "Length: ${email.length}",
                fragment.isValidEmail(email),
            )
        }
    }

    /**
     * Property 2: Invalid emails are rejected.
     *
     * For any randomly generated invalid email (missing '@', multiple '@', empty local part,
     * empty domain, no dot in domain, empty string, whitespace), the validation function
     * SHALL return false.
     *
     * **Validates: Requirements 5.9**
     */
    @Test
    fun property2_invalidEmailsAreRejected() {
        val random = Random(seed = 99)

        repeat(ITERATIONS) { iteration ->
            val email = generateInvalidEmail(random)

            assertFalse(
                "Iteration $iteration: Invalid email '$email' was accepted by isValidEmail(). " +
                    "Length: ${email.length}",
                fragment.isValidEmail(email),
            )
        }
    }

    /**
     * Property 2: Emails exceeding 254 characters are rejected.
     *
     * For any string exceeding 254 characters (even if it otherwise matches the valid email
     * pattern), the validation function SHALL return false.
     *
     * **Validates: Requirements 5.9**
     */
    @Test
    fun property2_emailsExceeding254CharsAreRejected() {
        val random = Random(seed = 77)

        repeat(ITERATIONS) { iteration ->
            // Generate a valid-looking email that exceeds 254 characters
            val targetLength = random.nextInt(255, 500)
            val domain = "example.com"
            val atAndDomain = "@$domain"
            val localPartLength = targetLength - atAndDomain.length
            val localPart =
                (1..localPartLength)
                    .map {
                        LOCAL_PART_CHARS[random.nextInt(LOCAL_PART_CHARS.size)]
                    }.joinToString("")
            val email = "$localPart$atAndDomain"

            assertTrue(
                "Iteration $iteration: Generated email should exceed 254 chars but was " +
                    "${email.length} chars",
                email.length > MAX_EMAIL_LENGTH,
            )

            assertFalse(
                "Iteration $iteration: Email exceeding 254 chars ('${email.take(30)}...', " +
                    "length=${email.length}) was accepted by isValidEmail()",
                fragment.isValidEmail(email),
            )
        }
    }

    /**
     * Property 2: Validation agrees with reference implementation for random strings.
     *
     * For any randomly generated string (including edge cases), the validation function
     * SHALL produce the same result as the reference implementation.
     *
     * **Validates: Requirements 5.9**
     */
    @Test
    fun property2_validationMatchesReferenceForRandomStrings() {
        val random = Random(seed = 55)
        val allChars = ALPHANUMERIC_CHARS + listOf('@', '.', ' ', '\t', '\n', '_', '-', '+', '!')

        repeat(ITERATIONS) { iteration ->
            // Generate a completely random string
            val length = random.nextInt(0, 300)
            val randomStr =
                (0 until length)
                    .map {
                        allChars[random.nextInt(allChars.size)]
                    }.joinToString("")

            val expected = referenceIsValidEmail(randomStr)
            val actual = fragment.isValidEmail(randomStr)

            assertTrue(
                "Iteration $iteration: isValidEmail('${randomStr.take(50)}...') returned " +
                    "$actual but reference returned $expected. Length: ${randomStr.length}",
                expected == actual,
            )
        }
    }
}
