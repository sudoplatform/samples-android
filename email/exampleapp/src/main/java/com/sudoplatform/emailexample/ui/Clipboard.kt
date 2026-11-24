/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast

/**
 * Copy [text] to the clipboard under [label]. From Android 13 the system confirms the copy
 * itself, so the [copiedMessage] toast is only shown on earlier versions.
 */
fun Context.copyToClipboard(
    label: String,
    text: String,
    copiedMessage: String,
) {
    val clipboard = getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(this, copiedMessage, Toast.LENGTH_SHORT).show()
    }
}
