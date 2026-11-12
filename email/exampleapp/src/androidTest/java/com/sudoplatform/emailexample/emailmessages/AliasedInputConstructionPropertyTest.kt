/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.sudoplatform.sudoemail.types.EmailMessage
import com.sudoplatform.sudoemail.types.InternetMessageFormatHeader
import com.sudoplatform.sudoemail.types.inputs.SendEmailMessageViaAliasInput
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/**
 * Property-based test for aliased email input construction.
 *
 * Feature: send-aliased-email, Property 3: Aliased email input construction
 *
 * For any selected EmailAlias and any valid form data (recipients, subject, body,
 * attachments), the constructed SendEmailMessageViaAliasInput SHALL have
 * senderEmailAliasId equal to the selected alias's id, and the
 * InternetMessageFormatHeader.from SHALL be an EmailAddress with the selected
 * alias's aliasAddress.
 *
 * **Validates: Requirements 2.3, 2.4**
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class AliasedInputConstructionPropertyTest {
    companion object {
        private const val ITERATIONS = 100
        private const val MAX_ID_LENGTH = 36
        private const val MAX_ADDRESS_LENGTH = 40
        private const val MAX_SUBJECT_LENGTH = 100
        private const val MAX_BODY_LENGTH = 200
        private const val MAX_RECIPIENTS = 5

        private val ALPHA_CHARS = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        private val TEXT_CHARS = ALPHA_CHARS + listOf(' ', '-', '_', '.', '!', '?', ',')
    }

    /**
     * Simplified representation of a alias for property testing purposes.
     */
    data class TestAlias(
        val id: String,
        val aliasAddress: String,
    )

    /**
     * Simplified representation of form data for property testing purposes.
     */
    data class TestFormData(
        val toRecipients: List<String>,
        val ccRecipients: List<String>,
        val bccRecipients: List<String>,
        val subject: String,
        val body: String,
    )

    /**
     * Models the aliased email input construction logic from
     * SendEmailMessageFragment.sendAliasedEmailMessage.
     *
     * This mirrors the actual implementation:
     * ```
     * val emailMessageHeader = InternetMessageFormatHeader(
     *     from = EmailMessage.EmailAddress(alias.aliasAddress),
     *     to = ...,
     *     cc = ...,
     *     bcc = ...,
     *     replyTo = emptyList(),
     *     subject = ...,
     * )
     * val input = SendEmailMessageViaAliasInput(
     *     senderEmailAliasId = alias.id,
     *     emailMessageHeader = emailMessageHeader,
     *     body = ...,
     *     attachments = ...,
     * )
     * ```
     */
    private fun constructAliasedEmailInput(
        alias: TestAlias,
        formData: TestFormData,
    ): SendEmailMessageViaAliasInput {
        val emailMessageHeader =
            InternetMessageFormatHeader(
                from = EmailMessage.EmailAddress(alias.aliasAddress),
                to = formData.toRecipients.map { EmailMessage.EmailAddress(it) },
                cc = formData.ccRecipients.map { EmailMessage.EmailAddress(it) },
                bcc = formData.bccRecipients.map { EmailMessage.EmailAddress(it) },
                replyTo = emptyList(),
                subject = formData.subject,
            )
        return SendEmailMessageViaAliasInput(
            senderEmailAliasId = alias.id,
            emailMessageHeader = emailMessageHeader,
            body = formData.body,
            attachments = emptyList(),
        )
    }

    /**
     * Generates a random alphanumeric string of the given maximum length.
     */
    private fun randomAlphaString(
        random: Random,
        maxLength: Int,
        minLength: Int = 1,
    ): String {
        val length = random.nextInt(minLength, maxLength + 1)
        return (1..length).map { ALPHA_CHARS[random.nextInt(ALPHA_CHARS.size)] }.joinToString("")
    }

    /**
     * Generates a random text string (with spaces and punctuation) of the given maximum length.
     */
    private fun randomTextString(
        random: Random,
        maxLength: Int,
        minLength: Int = 1,
    ): String {
        val length = random.nextInt(minLength, maxLength + 1)
        return (1..length).map { TEXT_CHARS[random.nextInt(TEXT_CHARS.size)] }.joinToString("")
    }

    /**
     * Generates a random email-like address string.
     */
    private fun randomEmailAddress(random: Random): String {
        val local = randomAlphaString(random, 12, 3)
        val domain = randomAlphaString(random, 10, 3)
        return "$local@$domain.com"
    }

    /**
     * Generates a random [TestAlias] with a random id and alias address.
     */
    private fun generateRandomAlias(random: Random): TestAlias =
        TestAlias(
            id = randomAlphaString(random, MAX_ID_LENGTH, 8),
            aliasAddress = randomEmailAddress(random),
        )

    /**
     * Generates a random list of email addresses for recipients.
     */
    private fun generateRandomRecipients(random: Random): List<String> {
        val size = random.nextInt(0, MAX_RECIPIENTS + 1)
        return (1..size).map { randomEmailAddress(random) }
    }

    /**
     * Generates random [TestFormData] with recipients, subject, and body.
     */
    private fun generateRandomFormData(random: Random): TestFormData =
        TestFormData(
            toRecipients =
                generateRandomRecipients(random).let {
                    // Ensure at least one "to" recipient for valid form data
                    if (it.isEmpty()) listOf(randomEmailAddress(random)) else it
                },
            ccRecipients = generateRandomRecipients(random),
            bccRecipients = generateRandomRecipients(random),
            subject = randomTextString(random, MAX_SUBJECT_LENGTH, 1),
            body = randomTextString(random, MAX_BODY_LENGTH, 1),
        )

    /**
     * Property 3: Aliased email input construction.
     *
     * For any selected alias and valid form data, the constructed
     * SendEmailMessageViaAliasInput has senderEmailAliasId == alias.id and
     * header.from == EmailAddress(alias.aliasAddress).
     *
     * **Validates: Requirements 2.3, 2.4**
     */
    @Test
    fun property3_aliasedEmailInputConstruction() {
        val random = Random(seed = 42)

        repeat(ITERATIONS) { iteration ->
            val alias = generateRandomAlias(random)
            val formData = generateRandomFormData(random)

            val input = constructAliasedEmailInput(alias, formData)

            // Assert: senderEmailAliasId == alias.id
            assertEquals(
                "Iteration $iteration: senderEmailAliasId should equal alias.id. " +
                    "Expected '${alias.id}' but got '${input.senderEmailAliasId}'",
                alias.id,
                input.senderEmailAliasId,
            )

            // Assert: header.from.emailAddress == alias.aliasAddress
            assertEquals(
                "Iteration $iteration: header.from should be EmailAddress(alias.aliasAddress). " +
                    "Expected '${alias.aliasAddress}' but got " +
                    "'${input.emailMessageHeader.from.emailAddress}'",
                alias.aliasAddress,
                input.emailMessageHeader.from.emailAddress,
            )
        }
    }

    /**
     * Edge case: alias with minimal id and address still constructs correctly.
     */
    @Test
    fun property3_minimalAliasConstructsCorrectly() {
        val alias = TestAlias(id = "a", aliasAddress = "x@y.z")
        val formData =
            TestFormData(
                toRecipients = listOf("recipient@example.com"),
                ccRecipients = emptyList(),
                bccRecipients = emptyList(),
                subject = "S",
                body = "B",
            )

        val input = constructAliasedEmailInput(alias, formData)

        assertEquals("a", input.senderEmailAliasId)
        assertEquals("x@y.z", input.emailMessageHeader.from.emailAddress)
    }

    /**
     * Edge case: alias address with special characters constructs correctly.
     */
    @Test
    fun property3_specialCharacterAliasAddress() {
        val alias =
            TestAlias(
                id = "alias-id-123-abc-456",
                aliasAddress = "user.name+tag@aliases.sudoplatform.com",
            )
        val formData =
            TestFormData(
                toRecipients = listOf("to@example.com"),
                ccRecipients = listOf("cc@example.com"),
                bccRecipients = listOf("bcc@example.com"),
                subject = "Test Subject",
                body = "Test body content",
            )

        val input = constructAliasedEmailInput(alias, formData)

        assertEquals("alias-id-123-abc-456", input.senderEmailAliasId)
        assertEquals(
            "user.name+tag@aliases.sudoplatform.com",
            input.emailMessageHeader.from.emailAddress,
        )
    }
}
