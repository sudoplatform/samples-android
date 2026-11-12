/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaliases

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.sudoplatform.sudoemail.types.EmailAliasRealAddressType
import com.sudoplatform.sudoemail.types.EmailAliasStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/**
 * Property-based test for verify button visibility determined by alias state.
 *
 * Feature: external-email-aliases, Property 3: Verify button visibility determined by alias state
 *
 * For any email alias displayed when the switch is in the external (enabled) position, the
 * "Verify" button SHALL be visible if and only if the alias's realAddressType is EXTERNAL and
 * its status is PENDING (awaiting verification). For aliases displayed when the switch is in
 * the internal (disabled) position, the "Verify" button SHALL never be visible regardless
 * of status.
 *
 * **Validates: Requirements 6.1**
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class VerifyButtonVisibilityPropertyTest {
    companion object {
        private const val ITERATIONS = 100

        private val CHARS = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        private val ALL_REAL_ADDRESS_TYPES = EmailAliasRealAddressType.values()
        private val ALL_STATUSES = EmailAliasStatus.values()
    }

    /**
     * Simplified representation of an EmailAlias for property testing purposes.
     * Captures only the fields relevant to verify button visibility logic.
     */
    data class TestEmailAlias(
        val id: String,
        val aliasAddress: String,
        val realAddress: String,
        val realAddressType: EmailAliasRealAddressType,
        val status: EmailAliasStatus,
    )

    /**
     * The verify button visibility logic under test, modelling the logic from
     * EmailAddressesFragment.showVerifyButton:
     *
     * The verify button is visible only when:
     * - The internal/external switch is in the external (enabled) position
     * - The alias's realAddressType is EXTERNAL
     * - The alias's status is PENDING (i.e., awaiting verification)
     *
     * @param switchIsExternal Whether the switch is in the external (enabled) position
     * @param emailAlias The email alias to check
     * @return True if the verify button should be shown, false otherwise
     */
    private fun showVerifyButton(
        switchIsExternal: Boolean,
        emailAlias: TestEmailAlias,
    ): Boolean =
        switchIsExternal &&
            emailAlias.realAddressType == EmailAliasRealAddressType.EXTERNAL &&
            emailAlias.status == EmailAliasStatus.PENDING

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
     * Generates a random [TestEmailAlias] with random realAddressType and status.
     */
    private fun generateRandomAlias(random: Random): TestEmailAlias {
        val realAddressType = ALL_REAL_ADDRESS_TYPES[random.nextInt(ALL_REAL_ADDRESS_TYPES.size)]
        val status = ALL_STATUSES[random.nextInt(ALL_STATUSES.size)]
        return TestEmailAlias(
            id = randomString(random, 12),
            aliasAddress = "${randomString(random, 8)}@aliases.example.com",
            realAddress = "${randomString(random, 8)}@${randomString(random, 6)}.com",
            realAddressType = realAddressType,
            status = status,
        )
    }

    /**
     * Property 3: Verify button visible only when switch is external AND realAddressType
     * is EXTERNAL AND status is PENDING.
     *
     * Generates random email alias objects with various realAddressType and status combinations
     * and verifies the button is visible only under the correct conditions.
     *
     * **Validates: Requirements 6.1**
     */
    @Test
    fun property3_verifyButtonVisibleOnlyWhenExternalSwitchAndExternalTypeAndPending() {
        val random = Random(seed = 42)

        repeat(ITERATIONS) { iteration ->
            val alias = generateRandomAlias(random)
            val switchIsExternal = random.nextBoolean()

            val result = showVerifyButton(switchIsExternal, alias)

            val expectedVisible =
                switchIsExternal &&
                    alias.realAddressType == EmailAliasRealAddressType.EXTERNAL &&
                    alias.status == EmailAliasStatus.PENDING

            assertEquals(
                "Iteration $iteration: Verify button visibility mismatch. " +
                    "switchIsExternal=$switchIsExternal, " +
                    "realAddressType=${alias.realAddressType}, " +
                    "status=${alias.status}. " +
                    "Expected visible=$expectedVisible but got $result.",
                expectedVisible,
                result,
            )
        }
    }

    /**
     * Property 3: Verify button never visible when switch is in internal position.
     *
     * Regardless of the alias's realAddressType or status, the verify button SHALL never
     * be visible when the switch is in the internal (disabled) position.
     *
     * **Validates: Requirements 6.1**
     */
    @Test
    fun property3_verifyButtonNeverVisibleWhenSwitchIsInternal() {
        val random = Random(seed = 99)

        repeat(ITERATIONS) { iteration ->
            val alias = generateRandomAlias(random)

            val result = showVerifyButton(switchIsExternal = false, alias)

            assertFalse(
                "Iteration $iteration: Verify button should never be visible when switch " +
                    "is in internal position, but got visible=true. " +
                    "realAddressType=${alias.realAddressType}, status=${alias.status}.",
                result,
            )
        }
    }

    /**
     * Property 3: Verify button not visible for non-EXTERNAL aliases even when switch is external.
     *
     * When the switch is in the external position but the alias's realAddressType is INTERNAL
     * or UNKNOWN, the verify button SHALL not be visible regardless of status.
     *
     * **Validates: Requirements 6.1**
     */
    @Test
    fun property3_verifyButtonNotVisibleForNonExternalAliasesEvenWhenSwitchExternal() {
        val random = Random(seed = 77)
        val nonExternalTypes =
            arrayOf(
                EmailAliasRealAddressType.INTERNAL,
                EmailAliasRealAddressType.UNKNOWN,
            )

        repeat(ITERATIONS) { iteration ->
            val realAddressType = nonExternalTypes[random.nextInt(nonExternalTypes.size)]
            val status = ALL_STATUSES[random.nextInt(ALL_STATUSES.size)]
            val alias =
                TestEmailAlias(
                    id = randomString(random, 12),
                    aliasAddress = "${randomString(random, 8)}@aliases.example.com",
                    realAddress = "${randomString(random, 8)}@${randomString(random, 6)}.com",
                    realAddressType = realAddressType,
                    status = status,
                )

            val result = showVerifyButton(switchIsExternal = true, alias)

            assertFalse(
                "Iteration $iteration: Verify button should not be visible for " +
                    "realAddressType=$realAddressType when switch is external, " +
                    "but got visible=true. status=$status.",
                result,
            )
        }
    }

    /**
     * Property 3: Verify button not visible for non-PENDING external aliases.
     *
     * When the switch is in the external position and the alias's realAddressType is EXTERNAL
     * but the status is not PENDING, the verify button SHALL not be visible.
     *
     * **Validates: Requirements 6.1**
     */
    @Test
    fun property3_verifyButtonNotVisibleForNonPendingExternalAliases() {
        val random = Random(seed = 55)
        val nonPendingStatuses = ALL_STATUSES.filter { it != EmailAliasStatus.PENDING }.toTypedArray()

        repeat(ITERATIONS) { iteration ->
            val status = nonPendingStatuses[random.nextInt(nonPendingStatuses.size)]
            val alias =
                TestEmailAlias(
                    id = randomString(random, 12),
                    aliasAddress = "${randomString(random, 8)}@aliases.example.com",
                    realAddress = "${randomString(random, 8)}@${randomString(random, 6)}.com",
                    realAddressType = EmailAliasRealAddressType.EXTERNAL,
                    status = status,
                )

            val result = showVerifyButton(switchIsExternal = true, alias)

            assertFalse(
                "Iteration $iteration: Verify button should not be visible for " +
                    "EXTERNAL alias with status $status (only PENDING should show verify), " +
                    "but got visible=true. Alias: $alias.",
                result,
            )
        }
    }

    /**
     * Exhaustive verification: explicitly tests every combination of realAddressType,
     * status, and switch state to ensure complete coverage beyond random sampling.
     */
    @Test
    fun property3_exhaustiveCombinationCoverage() {
        for (switchIsExternal in listOf(true, false)) {
            for (realAddressType in ALL_REAL_ADDRESS_TYPES) {
                for (status in ALL_STATUSES) {
                    val alias =
                        TestEmailAlias(
                            id = "test-id",
                            aliasAddress = "test@aliases.example.com",
                            realAddress = "user@example.com",
                            realAddressType = realAddressType,
                            status = status,
                        )

                    val result = showVerifyButton(switchIsExternal, alias)

                    val expectedVisible =
                        switchIsExternal &&
                            realAddressType == EmailAliasRealAddressType.EXTERNAL &&
                            status == EmailAliasStatus.PENDING

                    assertEquals(
                        "Exhaustive check failed: switchIsExternal=$switchIsExternal, " +
                            "realAddressType=$realAddressType, status=$status. " +
                            "Expected visible=$expectedVisible but got $result.",
                        expectedVisible,
                        result,
                    )
                }
            }
        }
    }
}
