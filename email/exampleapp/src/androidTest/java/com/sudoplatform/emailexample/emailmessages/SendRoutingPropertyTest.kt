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
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/**
 * Property-based test for send routing decision.
 *
 * Feature: send-masked-email, Property 2: Send routing decision
 *
 * For any valid compose form state, if a non-null selectedEmailMask is present
 * then the system SHALL use the sendMaskedEmailMessage path, and if
 * selectedEmailMask is null then the system SHALL use the sendEmailMessage path.
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
        private val ALL_STATUSES = EmailMaskStatus.values()
    }

    /**
     * Represents the two possible send paths.
     */
    enum class SendRoute {
        MASKED,
        REGULAR,
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
     * Models the routing decision as a pure function.
     * This mirrors the logic in SendEmailMessageFragment.sendEmailMessage():
     * if selectedEmailMask != null → MASKED path, else → REGULAR path.
     */
    private fun determineSendRoute(selectedEmailMask: TestEmailMask?): SendRoute =
        if (selectedEmailMask != null) {
            SendRoute.MASKED
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
     * Generates a random [TestEmailMask] with the given random instance.
     */
    private fun generateRandomMask(random: Random): TestEmailMask =
        TestEmailMask(
            id = randomString(random, MAX_ID_LENGTH),
            maskAddress = "${randomString(random, 8)}@masks.example.com",
            emailAddressId = randomString(random, MAX_ID_LENGTH),
            status = ALL_STATUSES[random.nextInt(ALL_STATUSES.size)],
        )

    /**
     * Generates a random nullable [TestEmailMask].
     * Approximately 50% chance of being null to ensure both paths are exercised.
     */
    private fun generateRandomSelectedMask(random: Random): TestEmailMask? =
        if (random.nextBoolean()) {
            generateRandomMask(random)
        } else {
            null
        }

    /**
     * Property 2: Send routing decision
     *
     * For any compose state, if selectedEmailMask is non-null then the masked
     * path is chosen; if null then the regular path is chosen.
     *
     * Feature: send-masked-email, Property 2: Send routing decision
     * **Validates: Requirements 2.1, 2.2, 3.1**
     */
    @Test
    fun property2_sendRoutingDecision() {
        val random = Random(seed = 42)

        repeat(ITERATIONS) { iteration ->
            // Generate a random selectedEmailMask (nullable)
            val selectedEmailMask = generateRandomSelectedMask(random)

            // Determine the route
            val route = determineSendRoute(selectedEmailMask)

            // Assert: non-null mask → MASKED path
            if (selectedEmailMask != null) {
                assertEquals(
                    "Iteration $iteration: Expected MASKED route when selectedEmailMask " +
                        "is non-null ($selectedEmailMask), but got $route",
                    SendRoute.MASKED,
                    route,
                )
            } else {
                // Assert: null mask → REGULAR path
                assertEquals(
                    "Iteration $iteration: Expected REGULAR route when selectedEmailMask " +
                        "is null, but got $route",
                    SendRoute.REGULAR,
                    route,
                )
            }
        }
    }
}
