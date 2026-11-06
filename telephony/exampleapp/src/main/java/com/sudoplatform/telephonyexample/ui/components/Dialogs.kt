/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sudoplatform.telephonyexample.R
import com.sudoplatform.telephonyexample.ui.TestTags
import com.sudoplatform.telephonyexample.ui.theme.PrimaryText
import com.sudoplatform.telephonyexample.ui.theme.SecondaryText

private val dialogShape = RoundedCornerShape(2.dp)

/**
 * A blocking, non-cancelable dialog showing a spinner and a message while an operation runs.
 */
@Composable
fun LoadingDialog(title: String) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(shape = dialogShape, color = Color.White) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(24.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(36.dp).testTag(TestTags.LOADING))
                Text(
                    title,
                    color = PrimaryText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 20.dp),
                )
            }
        }
    }
}

/**
 * An alert with a title, optional message and up to two buttons.
 */
@Composable
fun MessageDialog(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    message: String? = null,
    dismissText: String? = null,
    onDismiss: () -> Unit = onDismissRequest,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = dialogShape,
        containerColor = Color.White,
        tonalElevation = 0.dp,
        title = { Text(title, color = PrimaryText, style = MaterialTheme.typography.titleLarge) },
        text = message?.let { { Text(it, color = SecondaryText, style = MaterialTheme.typography.bodyLarge) } },
        confirmButton = { DialogTextButton(confirmText, onConfirm, TestTags.DIALOG_CONFIRM) },
        dismissButton = dismissText?.let { { DialogTextButton(it, onDismiss, TestTags.DIALOG_DISMISS) } },
    )
}

/** A failure alert offering **TRY AGAIN** and **CANCEL**. Either button closes it. */
@Composable
fun RetryDialog(
    title: String,
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    MessageDialog(
        title = title,
        message = message,
        confirmText = stringResource(R.string.try_again),
        onConfirm = {
            onDismiss()
            onRetry()
        },
        dismissText = stringResource(R.string.cancel),
        onDismissRequest = onDismiss,
    )
}

@Composable
private fun DialogTextButton(
    text: String,
    onClick: () -> Unit,
    testTag: String,
) {
    TextButton(onClick = onClick, modifier = Modifier.testTag(testTag)) {
        Text(text.uppercase(), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge)
    }
}
