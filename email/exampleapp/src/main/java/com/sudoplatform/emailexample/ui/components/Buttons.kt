/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.ui.TestTags
import com.sudoplatform.emailexample.ui.theme.DefaultButtonGrey

/**
 * Square, primary-coloured call to action (the register screen's "Register / Login" button).
 * While [isLoading] the label is replaced by a spinner and clicks are ignored.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        shape = RectangleShape,
        contentPadding = PaddingValues(horizontal = 8.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 6.dp, disabledElevation = 2.dp),
        // Keep the primary colour while busy; the spinner already says the button is unavailable.
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                disabledContainerColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = Color.White,
            ),
        modifier = modifier,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(24.dp).testTag(TestTags.LOADING),
            )
        } else {
            Text(text.uppercase(), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** A grey, slightly raised secondary button (the register screen's "Reset" / "Sign Out"). */
@Composable
fun DefaultButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(2.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 6.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = DefaultButtonGrey,
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        modifier = modifier,
    ) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Full width white row with start-aligned primary text, used for "+ Create"-style actions under
 * lists.
 */
@Composable
fun MenuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RectangleShape,
        contentPadding = PaddingValues(horizontal = 12.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = Color.White,
                disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
            ),
        modifier = modifier.fillMaxWidth().height(55.dp),
    ) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** A copy icon button, shown beside the value it copies. */
@Composable
fun CopyButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painterResource(R.drawable.ic_content_copy_24px),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}
