/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sudoplatform.telephonyexample.R
import com.sudoplatform.telephonyexample.ui.theme.PrimaryText
import com.sudoplatform.telephonyexample.ui.theme.SecondaryText

/**
 * A 60dp white list row. Rows are separated by a 2dp gap through which the grey screen
 * background shows.
 *
 * @param subtitle an optional second line, such as a state or a preview.
 * @param subtitleColor the colour of [subtitle], for example to mark a failure.
 * @param emphasised shows [text] in bold, for example for an unread message.
 * @param onClick when non-null the row is clickable and shows a trailing forward arrow.
 * @param onLongClick when non-null the row responds to a long press, such as copying its text.
 * @param textLeadingContent shown at the start of the [text] line, such as an unread indicator.
 * @param trailingContent row actions shown before the arrow, such as an edit button.
 */
@Composable
fun ItemCell(
    text: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleColor: Color = SecondaryText,
    emphasised: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    textLeadingContent: @Composable RowScope.() -> Unit = {},
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .padding(bottom = 2.dp)
                .fillMaxWidth()
                .height(60.dp)
                .background(Color.White)
                .then(
                    if (onClick != null || onLongClick != null) {
                        Modifier.combinedClickable(onClick = onClick ?: {}, onLongClick = onLongClick)
                    } else {
                        Modifier
                    },
                ).padding(horizontal = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                textLeadingContent()
                Text(
                    text,
                    color = if (emphasised) PrimaryText else SecondaryText,
                    fontWeight = if (emphasised) FontWeight.Bold else null,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (subtitle != null) {
                Text(
                    subtitle,
                    color = subtitleColor,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailingContent()
        if (onClick != null) {
            Icon(
                painterResource(R.drawable.ic_arrow_forward_24px),
                contentDescription = null,
                tint = Color.Unspecified,
            )
        }
    }
}
