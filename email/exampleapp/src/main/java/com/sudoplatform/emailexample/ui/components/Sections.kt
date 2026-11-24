/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sudoplatform.emailexample.ui.theme.SecondaryText

/** An upper case heading above a group of [ItemCell] rows. */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text.uppercase(),
        color = SecondaryText,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier.padding(top = 16.dp, bottom = 8.dp, start = 12.dp),
    )
}

/** Explanatory text below a group of [ItemCell] rows. */
@Composable
fun SectionFooter(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        color = SecondaryText,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier.padding(top = 6.dp, start = 12.dp, end = 12.dp),
    )
}
