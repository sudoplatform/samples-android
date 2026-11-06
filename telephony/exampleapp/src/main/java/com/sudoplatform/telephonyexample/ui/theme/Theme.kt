/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val colorScheme =
    lightColorScheme(
        primary = SudoPrimary,
        onPrimary = Color.White,
        primaryContainer = SudoPrimaryDark,
        onPrimaryContainer = Color.White,
        secondary = SudoAccent,
        onSecondary = Color.White,
        tertiary = SudoAccent,
        onTertiary = Color.White,
        background = SudoBackground,
        onBackground = PrimaryText,
        surface = Color.White,
        onSurface = PrimaryText,
        surfaceVariant = Color.White,
        onSurfaceVariant = SecondaryText,
        surfaceContainer = Color.White,
        surfaceContainerHigh = Color.White,
        surfaceContainerHighest = Color.White,
        error = NegativeMessage,
        onError = Color.White,
    )

private val typography =
    Typography(
        titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium),
        bodyLarge = TextStyle(fontSize = 16.sp),
        bodyMedium = TextStyle(fontSize = 14.sp),
        labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp),
    )

@Composable
fun TelephonyExampleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content,
    )
}
