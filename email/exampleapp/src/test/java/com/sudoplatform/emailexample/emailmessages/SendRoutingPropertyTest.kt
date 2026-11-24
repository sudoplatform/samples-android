/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import com.sudoplatform.sudoemail.types.EmailAlias
import com.sudoplatform.sudoemail.types.EmailAliasRealAddressType
import com.sudoplatform.sudoemail.types.EmailAliasStatus
import com.sudoplatform.sudoemail.types.EmailMessage
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date
import kotlin.random.Random

/**
 * Property-based test for send routing decision.
 *
 * Feature: send-aliased-email, Property 2: Send routing decision
 *
 * For any valid compose form state, if a non-null selected alias is present
 * then the system SHALL send via the alias, and if the selected alias is null
 * then the system SHALL send from the email address.
 *
 * **Validates: Requirements 2.1, 2.2, 3.1**
 */
class SendRoutingPropertyTest {
    companion object {
        private const val ITERATIONS = 100
        private const val MAX_ID_LENGTH = 12

        private val CHARS = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        private val ALL_STATUSES = EmailAliasStatus.values()
    }

    /**
     * The kind of request the compose screen sends for the given selection.
     */
    private fun determineSendRoute(selectedEmailAlias: EmailAlias?): Class<out SendRequest> =
        buildSendRequest(
            emailAddressId = "email-address-id",
            sender = EmailMessage.EmailAddress("sender@example.com"),
            selectedAlias = selectedEmailAlias,
            content = MessageContent(to = "to@example.com", subject = "Subject", body = "Body"),
            attachments = emptyList(),
        ).javaClass

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
     * Generates a random [EmailAlias] with the given random instance.
     */
    private fun generateRandomAlias(random: Random): EmailAlias =
        EmailAlias(
            id = randomString(random, MAX_ID_LENGTH),
            owner = "owner",
            owners = emptyList(),
            identityId = "identity",
            aliasAddress = "${randomString(random, 8)}@aliases.example.com",
            realAddress = "${randomString(random, MAX_ID_LENGTH)}@example.com",
            realAddressType = EmailAliasRealAddressType.INTERNAL,
            status = ALL_STATUSES[random.nextInt(ALL_STATUSES.size)],
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
     * Generates a random nullable [EmailAlias].
     * Approximately 50% chance of being null to ensure both paths are exercised.
     */
    private fun generateRandomSelectedAlias(random: Random): EmailAlias? =
        if (random.nextBoolean()) {
            generateRandomAlias(random)
        } else {
            null
        }

    /**
     * Property 2: Send routing decision
     *
     * For any compose state, if the selected alias is non-null then the message is
     * sent via the alias; if null then it is sent from the email address.
     *
     * Feature: send-aliased-email, Property 2: Send routing decision
     * **Validates: Requirements 2.1, 2.2, 3.1**
     */
    @Test
    fun property2_sendRoutingDecision() {
        val random = Random(seed = 42)

        repeat(ITERATIONS) { iteration ->
            // Generate a random selected alias (nullable)
            val selectedEmailAlias = generateRandomSelectedAlias(random)

            // Determine the route
            val route = determineSendRoute(selectedEmailAlias)

            // Assert: non-null alias → sent via the alias
            if (selectedEmailAlias != null) {
                assertEquals(
                    "Iteration $iteration: Expected ViaAlias route when the selected alias " +
                        "is non-null ($selectedEmailAlias), but got $route",
                    SendRequest.ViaAlias::class.java,
                    route,
                )
            } else {
                // Assert: null alias → sent from the email address
                assertEquals(
                    "Iteration $iteration: Expected FromEmailAddress route when the selected alias " +
                        "is null, but got $route",
                    SendRequest.FromEmailAddress::class.java,
                    route,
                )
            }
        }
    }
}
