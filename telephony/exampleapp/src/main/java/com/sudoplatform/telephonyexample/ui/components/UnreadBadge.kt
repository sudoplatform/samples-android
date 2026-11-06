/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.ui.components

import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.sudoplatform.telephonyexample.R

/**
 * The unread indicator, shared by phone numbers and messages so they read the same. With a
 * [count] it shows how many messages are unread; without one it is a dot marking one unread
 * message.
 */
@Composable
fun UnreadBadge(
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    val description = pluralStringResource(R.plurals.unread_messages, count ?: 1, count ?: 1)
    Badge(
        modifier =
            modifier
                .semantics { contentDescription = description },
        containerColor = MaterialTheme.colorScheme.primary,
        // Badge draws the small dot only when it has no content.
        content = count?.let { { Text("$it") } },
    )
}
