/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sudoplatform.telephonyexample.ui.TestTags
import com.sudoplatform.telephonyexample.ui.theme.SecondaryText
import kotlinx.coroutines.delay

/** Loads that finish within this long never show the spinner, so it doesn't flash. */
private const val SHOW_DELAY_MILLIS = 300L

/**
 * A centred spinner with a caption below it, shown while a screen has nothing to display until
 * its data arrives. It fades in after a short delay.
 */
@Composable
fun InPlaceLoading(
    caption: String,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(SHOW_DELAY_MILLIS)
        visible = true
    }
    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(visible = visible, enter = fadeIn()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(modifier = Modifier.testTag(TestTags.LOADING))
                Text(
                    caption,
                    color = SecondaryText,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}
