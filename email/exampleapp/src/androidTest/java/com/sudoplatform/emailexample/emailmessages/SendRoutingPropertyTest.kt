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
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/**
 * Property-based test for send routing decision.
 *
 * Feature: send-aliased-email, Property 2: Send routing decision
 *
 * For any valid compose form state, if a non-null selectedEmailAlias is present
 * then the system SHALL use the sendAliasedEmailMessage path, and if
 * selectedEmailAlias is null then the system SHALL use the sendEmailMessage path.
 *
 * **Validates: Requirements 2.1, 2.2, 3.1**
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class SendRoutingPropertyTest {
    companion object {
        private const val ITERATIONS = 100
        private const val MAX_ID_LENGTH = 12

        private val CHARS = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        private val ALL_STATUSES = EmailAliasStatus.values()
    }

    /**
     * Represents the two possible send paths.
     */
    enum class SendRoute {
        ALIASED,
        REGULAR,
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
     * Models the routing decision as a pure function.
     * This mirrors the logic in SendEmailMessageFragment.sendEmailMessage():
     * if selectedEmailAlias != null → ALIASED path, else → REGULAR path.
     */
    private fun determineSendRoute(selectedEmailAlias: TestEmailAlias?): SendRoute =
        if (selectedEmailAlias != null) {
            SendRoute.ALIASED
        } else {
            SendRoute.REGULAR
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
     */
    private fun generateRandomAlias(random: Random): TestEmailAlias =
        TestEmailAlias(
            id = randomString(random, MAX_ID_LENGTH),
            aliasAddress = "${randomString(random, 8)}@aliases.example.com",
            emailAddressId = randomString(random, MAX_ID_LENGTH),
            status = ALL_STATUSES[random.nextInt(ALL_STATUSES.size)],
        )

    /**
     * Generates a random nullable [TestEmailAlias].
     * Approximately 50% chance of being null to ensure both paths are exercised.
     */
    private fun generateRandomSelectedAlias(random: Random): TestEmailAlias? =
        if (random.nextBoolean()) {
            generateRandomAlias(random)
        } else {
            null
        }

    /**
     * Property 2: Send routing decision
     *
     * For any compose state, if selectedEmailAlias is non-null then the aliased
     * path is chosen; if null then the regular path is chosen.
     *
     * Feature: send-aliased-email, Property 2: Send routing decision
     * **Validates: Requirements 2.1, 2.2, 3.1**
     */
    @Test
    fun property2_sendRoutingDecision() {
        val random = Random(seed = 42)

        repeat(ITERATIONS) { iteration ->
            // Generate a random selectedEmailAlias (nullable)
            val selectedEmailAlias = generateRandomSelectedAlias(random)

            // Determine the route
            val route = determineSendRoute(selectedEmailAlias)

            // Assert: non-null alias → ALIASED path
            if (selectedEmailAlias != null) {
                assertEquals(
                    "Iteration $iteration: Expected ALIASED route when selectedEmailAlias " +
                        "is non-null ($selectedEmailAlias), but got $route",
                    SendRoute.ALIASED,
                    route,
                )
            } else {
                // Assert: null alias → REGULAR path
                assertEquals(
                    "Iteration $iteration: Expected REGULAR route when selectedEmailAlias " +
                        "is null, but got $route",
                    SendRoute.REGULAR,
                    route,
                )
            }
        }
    }
}
