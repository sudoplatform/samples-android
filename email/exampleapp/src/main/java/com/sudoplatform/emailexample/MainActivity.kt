/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * This is the first activity that is invoked when the app is launched.
 * It acts as the host to the navigation graph.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        askNotificationPermission()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.w("Main", "Granted notification permission")
            // FCM SDK (and your app) can post notifications.
        } else {
            AlertDialog.Builder(this, androidx.appcompat.R.style.AlertDialog_AppCompat)
                .setTitle("Alert")
                .setMessage("Notifications will not be displayed")
                .setNeutralButton("Dismiss") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
                Log.w("Main", "notification permission granted")
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                AlertDialog.Builder(this, androidx.appcompat.R.style.AlertDialog_AppCompat)
                    .setTitle("Alert")
                    .setMessage("This App requires Push Notification permission.")
                    .setPositiveButton("Allow") { _, _ ->
                        if (Build.VERSION.SDK_INT >= 33) {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    .setNegativeButton("Do not allow", null)
                    .show()
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
