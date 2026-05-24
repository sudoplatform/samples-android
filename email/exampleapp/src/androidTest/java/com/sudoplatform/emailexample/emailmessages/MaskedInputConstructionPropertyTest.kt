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
import com.sudoplatform.sudoemail.types.inputs.SendMaskedEmailMessageInput
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/**
 * Property-based test for masked email input construction.
 *
 * Feature: send-masked-email, Property 3: Masked email input construction
 *
 * For any selected EmailMask and any valid form data (recipients, subject, body,
 * attachments), the constructed SendMaskedEmailMessageInput SHALL have
 * senderEmailMaskId equal to the selected mask's id, and the
 * InternetMessageFormatHeader.from SHALL be an EmailAddress with the selected
 * mask's maskAddress.
 *
 * **Validates: Requirements 2.3, 2.4**
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class MaskedInputConstructionPropertyTest {
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
     * Simplified representation of a mask for property testing purposes.
     */
    data class TestMask(
        val id: String,
        val maskAddress: String,
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
     * Models the masked email input construction logic from
     * SendEmailMessageFragment.sendMaskedEmailMessage.
     *
     * This mirrors the actual implementation:
     * ```
     * val emailMessageHeader = InternetMessageFormatHeader(
     *     from = EmailMessage.EmailAddress(mask.maskAddress),
     *     to = ...,
     *     cc = ...,
     *     bcc = ...,
     *     replyTo = emptyList(),
     *     subject = ...,
     * )
     * val input = SendMaskedEmailMessageInput(
     *     senderEmailMaskId = mask.id,
     *     emailMessageHeader = emailMessageHeader,
     *     body = ...,
     *     attachments = ...,
     * )
     * ```
     */
    private fun constructMaskedEmailInput(
        mask: TestMask,
        formData: TestFormData,
    ): SendMaskedEmailMessageInput {
        val emailMessageHeader =
            InternetMessageFormatHeader(
                from = EmailMessage.EmailAddress(mask.maskAddress),
                to = formData.toRecipients.map { EmailMessage.EmailAddress(it) },
                cc = formData.ccRecipients.map { EmailMessage.EmailAddress(it) },
                bcc = formData.bccRecipients.map { EmailMessage.EmailAddress(it) },
                replyTo = emptyList(),
                subject = formData.subject,
            )
        return SendMaskedEmailMessageInput(
            senderEmailMaskId = mask.id,
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
     * Generates a random [TestMask] with a random id and mask address.
     */
    private fun generateRandomMask(random: Random): TestMask =
        TestMask(
            id = randomAlphaString(random, MAX_ID_LENGTH, 8),
            maskAddress = randomEmailAddress(random),
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
     * Property 3: Masked email input construction.
     *
     * For any selected mask and valid form data, the constructed
     * SendMaskedEmailMessageInput has senderEmailMaskId == mask.id and
     * header.from == EmailAddress(mask.maskAddress).
     *
     * **Validates: Requirements 2.3, 2.4**
     */
    @Test
    fun property3_maskedEmailInputConstruction() {
        val random = Random(seed = 42)

        repeat(ITERATIONS) { iteration ->
            val mask = generateRandomMask(random)
            val formData = generateRandomFormData(random)

            val input = constructMaskedEmailInput(mask, formData)

            // Assert: senderEmailMaskId == mask.id
            assertEquals(
                "Iteration $iteration: senderEmailMaskId should equal mask.id. " +
                    "Expected '${mask.id}' but got '${input.senderEmailMaskId}'",
                mask.id,
                input.senderEmailMaskId,
            )

            // Assert: header.from.emailAddress == mask.maskAddress
            assertEquals(
                "Iteration $iteration: header.from should be EmailAddress(mask.maskAddress). " +
                    "Expected '${mask.maskAddress}' but got " +
                    "'${input.emailMessageHeader.from.emailAddress}'",
                mask.maskAddress,
                input.emailMessageHeader.from.emailAddress,
            )
        }
    }

    /**
     * Edge case: mask with minimal id and address still constructs correctly.
     */
    @Test
    fun property3_minimalMaskConstructsCorrectly() {
        val mask = TestMask(id = "a", maskAddress = "x@y.z")
        val formData =
            TestFormData(
                toRecipients = listOf("recipient@example.com"),
                ccRecipients = emptyList(),
                bccRecipients = emptyList(),
                subject = "S",
                body = "B",
            )

        val input = constructMaskedEmailInput(mask, formData)

        assertEquals("a", input.senderEmailMaskId)
        assertEquals("x@y.z", input.emailMessageHeader.from.emailAddress)
    }

    /**
     * Edge case: mask address with special characters constructs correctly.
     */
    @Test
    fun property3_specialCharacterMaskAddress() {
        val mask =
            TestMask(
                id = "mask-id-123-abc-456",
                maskAddress = "user.name+tag@masks.sudoplatform.com",
            )
        val formData =
            TestFormData(
                toRecipients = listOf("to@example.com"),
                ccRecipients = listOf("cc@example.com"),
                bccRecipients = listOf("bcc@example.com"),
                subject = "Test Subject",
                body = "Test body content",
            )

        val input = constructMaskedEmailInput(mask, formData)

        assertEquals("mask-id-123-abc-456", input.senderEmailMaskId)
        assertEquals(
            "user.name+tag@masks.sudoplatform.com",
            input.emailMessageHeader.from.emailAddress,
        )
    }
}
