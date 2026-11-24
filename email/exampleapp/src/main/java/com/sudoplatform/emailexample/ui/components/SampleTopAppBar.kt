/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.ui.TestTags

/**
 * The blue toolbar used at the top of every screen after registration.
 *
 * @param onNavigateBack when non-null, shows a back arrow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleTopAppBar(
    title: String,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    StatusBarIcons(darkIcons = false)
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag(TestTags.NAVIGATE_BACK)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
                }
            }
        },
        actions = actions,
        expandedHeight = 56.dp,
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White,
            ),
    )
}

/** A toolbar text action, rendered in upper case. */
@Composable
fun ToolbarTextAction(
    text: String,
    onClick: () -> Unit,
    testTag: String = "",
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.testTag(testTag),
    ) {
        Text(
            text.uppercase(),
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/** A toolbar icon action. */
@Composable
fun ToolbarIconAction(
    @DrawableRes iconResId: Int,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.testTag(testTag),
    ) {
        Icon(painterResource(iconResId), contentDescription = contentDescription)
    }
}
