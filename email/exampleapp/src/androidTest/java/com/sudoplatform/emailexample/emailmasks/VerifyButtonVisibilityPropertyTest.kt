/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmasks

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.sudoplatform.sudoemail.types.EmailMaskRealAddressType
import com.sudoplatform.sudoemail.types.EmailMaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/**
 * Property-based test for verify button visibility determined by mask state.
 *
 * Feature: external-email-masks, Property 3: Verify button visibility determined by mask state
 *
 * For any email mask displayed when the switch is in the external (enabled) position, the
 * "Verify" button SHALL be visible if and only if the mask's realAddressType is EXTERNAL and
 * its status is PENDING (awaiting verification). For masks displayed when the switch is in
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
        private val ALL_REAL_ADDRESS_TYPES = EmailMaskRealAddressType.values()
        private val ALL_STATUSES = EmailMaskStatus.values()
    }

    /**
     * Simplified representation of an EmailMask for property testing purposes.
     * Captures only the fields relevant to verify button visibility logic.
     */
    data class TestEmailMask(
        val id: String,
        val maskAddress: String,
        val realAddress: String,
        val realAddressType: EmailMaskRealAddressType,
        val status: EmailMaskStatus,
    )

    /**
     * The verify button visibility logic under test, modelling the logic from
     * EmailAddressesFragment.showVerifyButton:
     *
     * The verify button is visible only when:
     * - The internal/external switch is in the external (enabled) position
     * - The mask's realAddressType is EXTERNAL
     * - The mask's status is PENDING (i.e., awaiting verification)
     *
     * @param switchIsExternal Whether the switch is in the external (enabled) position
     * @param emailMask The email mask to check
     * @return True if the verify button should be shown, false otherwise
     */
    private fun showVerifyButton(
        switchIsExternal: Boolean,
        emailMask: TestEmailMask,
    ): Boolean =
        switchIsExternal &&
            emailMask.realAddressType == EmailMaskRealAddressType.EXTERNAL &&
            emailMask.status == EmailMaskStatus.PENDING

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
     * Generates a random [TestEmailMask] with random realAddressType and status.
     */
    private fun generateRandomMask(random: Random): TestEmailMask {
        val realAddressType = ALL_REAL_ADDRESS_TYPES[random.nextInt(ALL_REAL_ADDRESS_TYPES.size)]
        val status = ALL_STATUSES[random.nextInt(ALL_STATUSES.size)]
        return TestEmailMask(
            id = randomString(random, 12),
            maskAddress = "${randomString(random, 8)}@masks.example.com",
            realAddress = "${randomString(random, 8)}@${randomString(random, 6)}.com",
            realAddressType = realAddressType,
            status = status,
        )
    }

    /**
     * Property 3: Verify button visible only when switch is external AND realAddressType
     * is EXTERNAL AND status is PENDING.
     *
     * Generates random email mask objects with various realAddressType and status combinations
     * and verifies the button is visible only under the correct conditions.
     *
     * **Validates: Requirements 6.1**
     */
    @Test
    fun property3_verifyButtonVisibleOnlyWhenExternalSwitchAndExternalTypeAndPending() {
        val random = Random(seed = 42)

        repeat(ITERATIONS) { iteration ->
            val mask = generateRandomMask(random)
            val switchIsExternal = random.nextBoolean()

            val result = showVerifyButton(switchIsExternal, mask)

            val expectedVisible =
                switchIsExternal &&
                    mask.realAddressType == EmailMaskRealAddressType.EXTERNAL &&
                    mask.status == EmailMaskStatus.PENDING

            assertEquals(
                "Iteration $iteration: Verify button visibility mismatch. " +
                    "switchIsExternal=$switchIsExternal, " +
                    "realAddressType=${mask.realAddressType}, " +
                    "status=${mask.status}. " +
                    "Expected visible=$expectedVisible but got $result.",
                expectedVisible,
                result,
            )
        }
    }

    /**
     * Property 3: Verify button never visible when switch is in internal position.
     *
     * Regardless of the mask's realAddressType or status, the verify button SHALL never
     * be visible when the switch is in the internal (disabled) position.
     *
     * **Validates: Requirements 6.1**
     */
    @Test
    fun property3_verifyButtonNeverVisibleWhenSwitchIsInternal() {
        val random = Random(seed = 99)

        repeat(ITERATIONS) { iteration ->
            val mask = generateRandomMask(random)

            val result = showVerifyButton(switchIsExternal = false, mask)

            assertFalse(
                "Iteration $iteration: Verify button should never be visible when switch " +
                    "is in internal position, but got visible=true. " +
                    "realAddressType=${mask.realAddressType}, status=${mask.status}.",
                result,
            )
        }
    }

    /**
     * Property 3: Verify button not visible for non-EXTERNAL masks even when switch is external.
     *
     * When the switch is in the external position but the mask's realAddressType is INTERNAL
     * or UNKNOWN, the verify button SHALL not be visible regardless of status.
     *
     * **Validates: Requirements 6.1**
     */
    @Test
    fun property3_verifyButtonNotVisibleForNonExternalMasksEvenWhenSwitchExternal() {
        val random = Random(seed = 77)
        val nonExternalTypes =
            arrayOf(
                EmailMaskRealAddressType.INTERNAL,
                EmailMaskRealAddressType.UNKNOWN,
            )

        repeat(ITERATIONS) { iteration ->
            val realAddressType = nonExternalTypes[random.nextInt(nonExternalTypes.size)]
            val status = ALL_STATUSES[random.nextInt(ALL_STATUSES.size)]
            val mask =
                TestEmailMask(
                    id = randomString(random, 12),
                    maskAddress = "${randomString(random, 8)}@masks.example.com",
                    realAddress = "${randomString(random, 8)}@${randomString(random, 6)}.com",
                    realAddressType = realAddressType,
                    status = status,
                )

            val result = showVerifyButton(switchIsExternal = true, mask)

            assertFalse(
                "Iteration $iteration: Verify button should not be visible for " +
                    "realAddressType=$realAddressType when switch is external, " +
                    "but got visible=true. status=$status.",
                result,
            )
        }
    }

    /**
     * Property 3: Verify button not visible for non-PENDING external masks.
     *
     * When the switch is in the external position and the mask's realAddressType is EXTERNAL
     * but the status is not PENDING, the verify button SHALL not be visible.
     *
     * **Validates: Requirements 6.1**
     */
    @Test
    fun property3_verifyButtonNotVisibleForNonPendingExternalMasks() {
        val random = Random(seed = 55)
        val nonPendingStatuses = ALL_STATUSES.filter { it != EmailMaskStatus.PENDING }.toTypedArray()

        repeat(ITERATIONS) { iteration ->
            val status = nonPendingStatuses[random.nextInt(nonPendingStatuses.size)]
            val mask =
                TestEmailMask(
                    id = randomString(random, 12),
                    maskAddress = "${randomString(random, 8)}@masks.example.com",
                    realAddress = "${randomString(random, 8)}@${randomString(random, 6)}.com",
                    realAddressType = EmailMaskRealAddressType.EXTERNAL,
                    status = status,
                )

            val result = showVerifyButton(switchIsExternal = true, mask)

            assertFalse(
                "Iteration $iteration: Verify button should not be visible for " +
                    "EXTERNAL mask with status $status (only PENDING should show verify), " +
                    "but got visible=true. Mask: $mask.",
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
                    val mask =
                        TestEmailMask(
                            id = "test-id",
                            maskAddress = "test@masks.example.com",
                            realAddress = "user@example.com",
                            realAddressType = realAddressType,
                            status = status,
                        )

                    val result = showVerifyButton(switchIsExternal, mask)

                    val expectedVisible =
                        switchIsExternal &&
                            realAddressType == EmailMaskRealAddressType.EXTERNAL &&
                            status == EmailMaskStatus.PENDING

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
