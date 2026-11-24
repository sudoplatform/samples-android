/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaliases

import com.sudoplatform.sudoemail.types.EmailAliasStatus
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Property-based test for Update button availability across all alias statuses.
 *
 * Feature: update-email-alias, Property 1: Update button is available for all alias statuses
 *
 * For any EmailAliasStatus value (ENABLED, DISABLED, LOCKED, PENDING, UNKNOWN),
 * the action dialog options from aliasActionOptions should always include Update,
 * regardless of which other actions are available.
 *
 * **Validates: Requirements 1.2**
 */
class UpdateButtonAvailabilityPropertyTest {
    companion object {
        private const val ITERATIONS = 100
    }

    /**
     * Property 1: Update button is available for all alias statuses.
     *
     * For any EmailAliasStatus value, the action dialog options always include Update. This
     * test randomly selects statuses over 100 iterations to verify the property holds universally.
     *
     * Feature: update-email-alias, Property 1: Update button is available for all alias statuses
     * **Validates: Requirements 1.2**
     */
    @Test
    fun property1_updateButtonAvailableForAllStatuses() {
        val allStatuses = EmailAliasStatus.values()
        val random = Random(seed = 42)

        repeat(ITERATIONS) { iteration ->
            // Randomly select a status for this iteration
            val status = allStatuses[random.nextInt(allStatuses.size)]

            val options = aliasActionOptions(status)

            assertTrue(
                "Iteration $iteration: Expected 'Update' to be offered for status $status, " +
                    "but got $options",
                options.contains(EmailAliasAction.UPDATE),
            )
        }
    }

    /**
     * Exhaustive verification: explicitly tests every EmailAliasStatus value
     * to ensure complete coverage beyond random sampling.
     */
    @Test
    fun property1_exhaustiveStatusCoverage() {
        val allStatuses = EmailAliasStatus.values()

        for (status in allStatuses) {
            val options = aliasActionOptions(status)

            assertTrue(
                "Expected 'Update' to be offered for status $status, but got $options",
                options.contains(EmailAliasAction.UPDATE),
            )
        }
    }

    /**
     * Verifies that the Cancel button is also always present alongside Update,
     * ensuring the dialog always has a way to dismiss.
     */
    @Test
    fun property1_cancelButtonAlwaysPresentAlongsideUpdate() {
        val allStatuses = EmailAliasStatus.values()
        val random = Random(seed = 123)

        repeat(ITERATIONS) { iteration ->
            val status = allStatuses[random.nextInt(allStatuses.size)]

            val options = aliasActionOptions(status)

            assertTrue(
                "Iteration $iteration: Expected 'Cancel' to be offered for status $status, " +
                    "but got $options",
                options.contains(EmailAliasAction.CANCEL),
            )
        }
    }
}
