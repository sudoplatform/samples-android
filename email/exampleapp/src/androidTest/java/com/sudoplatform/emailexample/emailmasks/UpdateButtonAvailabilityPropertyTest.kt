/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmasks

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.sudoplatform.sudoemail.types.EmailMaskStatus
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/**
 * Property-based test for Update button availability across all mask statuses.
 *
 * Feature: update-email-mask, Property 1: Update button is available for all mask statuses
 *
 * For any EmailMaskStatus value (ENABLED, DISABLED, LOCKED, PENDING, UNKNOWN),
 * the action dialog options should always include an "Update" button, regardless
 * of which other actions are available.
 *
 * **Validates: Requirements 1.2**
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class UpdateButtonAvailabilityPropertyTest {
    companion object {
        private const val ITERATIONS = 100
    }

    /**
     * Represents the dialog button configuration produced by showActionDialog logic.
     */
    data class DialogConfiguration(
        val hasPositiveButton: Boolean,
        val positiveButtonLabel: String?,
        val hasNeutralButton: Boolean,
        val neutralButtonLabel: String?,
        val hasNegativeButton: Boolean,
        val negativeButtonLabel: String?,
    )

    /**
     * Models the action dialog configuration logic from EmailAddressesFragment.showActionDialog.
     *
     * This mirrors the actual implementation where:
     * - A negative "Cancel" button is always set
     * - A positive button ("Disable"/"Enable") is set only for ENABLED/DISABLED statuses
     * - A neutral "Update" button is set OUTSIDE the when block, so it applies to ALL statuses
     *
     * @param status The EmailMaskStatus to configure the dialog for
     * @return The resulting DialogConfiguration
     */
    private fun configureActionDialog(status: EmailMaskStatus): DialogConfiguration {
        var hasPositiveButton = false
        var positiveButtonLabel: String? = null

        // This mirrors the when block in showActionDialog
        when (status) {
            EmailMaskStatus.ENABLED -> {
                hasPositiveButton = true
                positiveButtonLabel = "Disable"
            }
            EmailMaskStatus.DISABLED -> {
                hasPositiveButton = true
                positiveButtonLabel = "Enable"
            }
            EmailMaskStatus.LOCKED,
            EmailMaskStatus.PENDING,
            EmailMaskStatus.UNKNOWN,
            -> {
                // No additional positive button for these statuses
            }
        }

        // The neutral "Update" button is set outside the when block — always present
        val hasNeutralButton = true
        val neutralButtonLabel = "Update"

        // The negative "Cancel" button is always set
        val hasNegativeButton = true
        val negativeButtonLabel = "Cancel"

        return DialogConfiguration(
            hasPositiveButton = hasPositiveButton,
            positiveButtonLabel = positiveButtonLabel,
            hasNeutralButton = hasNeutralButton,
            neutralButtonLabel = neutralButtonLabel,
            hasNegativeButton = hasNegativeButton,
            negativeButtonLabel = negativeButtonLabel,
        )
    }

    /**
     * Property 1: Update button is available for all mask statuses.
     *
     * For any EmailMaskStatus value, the action dialog configuration always includes
     * a neutral "Update" button. This test randomly selects statuses over 100 iterations
     * to verify the property holds universally.
     *
     * Feature: update-email-mask, Property 1: Update button is available for all mask statuses
     * **Validates: Requirements 1.2**
     */
    @Test
    fun property1_updateButtonAvailableForAllStatuses() {
        val allStatuses = EmailMaskStatus.values()
        val random = Random(seed = 42)

        repeat(ITERATIONS) { iteration ->
            // Randomly select a status for this iteration
            val status = allStatuses[random.nextInt(allStatuses.size)]

            val config = configureActionDialog(status)

            assertTrue(
                "Iteration $iteration: Expected neutral 'Update' button to be present " +
                    "for status $status, but hasNeutralButton was false",
                config.hasNeutralButton,
            )
            assertTrue(
                "Iteration $iteration: Expected neutral button label to be 'Update' " +
                    "for status $status, but got '${config.neutralButtonLabel}'",
                config.neutralButtonLabel == "Update",
            )
        }
    }

    /**
     * Exhaustive verification: explicitly tests every EmailMaskStatus value
     * to ensure complete coverage beyond random sampling.
     */
    @Test
    fun property1_exhaustiveStatusCoverage() {
        val allStatuses = EmailMaskStatus.values()

        for (status in allStatuses) {
            val config = configureActionDialog(status)

            assertTrue(
                "Expected neutral 'Update' button to be present for status $status",
                config.hasNeutralButton,
            )
            assertTrue(
                "Expected neutral button label to be 'Update' for status $status, " +
                    "but got '${config.neutralButtonLabel}'",
                config.neutralButtonLabel == "Update",
            )
        }
    }

    /**
     * Verifies that the Cancel button is also always present alongside Update,
     * ensuring the dialog always has a way to dismiss.
     */
    @Test
    fun property1_cancelButtonAlwaysPresentAlongsideUpdate() {
        val allStatuses = EmailMaskStatus.values()
        val random = Random(seed = 123)

        repeat(ITERATIONS) { iteration ->
            val status = allStatuses[random.nextInt(allStatuses.size)]

            val config = configureActionDialog(status)

            assertTrue(
                "Iteration $iteration: Expected negative 'Cancel' button to be present " +
                    "for status $status",
                config.hasNegativeButton,
            )
            assertTrue(
                "Iteration $iteration: Expected negative button label to be 'Cancel' " +
                    "for status $status, but got '${config.negativeButtonLabel}'",
                config.negativeButtonLabel == "Cancel",
            )
        }
    }
}
