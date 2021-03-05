/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.passwordmanagerexample.settings

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.sudopasswordmanager.SudoPasswordManagerClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Extension functions to help with the handling and rendering of the Rescue Kit PDF.
 *
 * @since 2020-12-21
 */
suspend fun renderRescueKitToFile(context: Context, client: SudoPasswordManagerClient): File {
    val pdf = client.renderRescueKit(context)
    val rescueKitFile = File(context.applicationContext.cacheDir, context.getString(R.string.rescuekit_pdf))
    withContext(Dispatchers.IO) {
        FileOutputStream(rescueKitFile).use {
            pdf.writeTo(it)
        }
    }
    pdf.close()
    return rescueKitFile
}

fun shareRescueKit(context: Context, rescueKitFile: File) {
    val sendIntent = Intent()
    sendIntent.action = Intent.ACTION_SEND
    sendIntent.type = "application/pdf"
    val uri = FileProvider.getUriForFile(
        context,
        context.packageName + ".provider",
        rescueKitFile
    )
    sendIntent.putExtra(Intent.EXTRA_STREAM, uri)
    sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(sendIntent)
}
