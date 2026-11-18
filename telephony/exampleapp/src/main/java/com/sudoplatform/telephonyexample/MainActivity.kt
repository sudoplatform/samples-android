/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sudoplatform.telephonyexample.notifications.MessageTarget
import com.sudoplatform.telephonyexample.ui.theme.TelephonyExampleTheme

/**
 * This is the first activity that is invoked when the app is launched.
 * It hosts the Compose navigation graph, receives Federated Single Sign On redirects, and opens
 * the message for a tapped notification.
 */
class MainActivity : ComponentActivity() {
    /** An FSSO redirect not yet handed to the register screen. */
    private var federatedSignInUri: Uri? by mutableStateOf(null)

    /** A message from a tapped notification, not yet opened. */
    private var pendingMessage: MessageTarget? by mutableStateOf(null)

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Screens draw behind the system bars and pad their content to fit.
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        if (savedInstanceState == null) {
            federatedSignInUri = intent?.federatedSignInUri()
            pendingMessage = intent?.messageTarget()
            requestNotificationPermissionIfNeeded()
        }
        setContent {
            TelephonyExampleTheme {
                AppNavHost(
                    federatedSignInUri = federatedSignInUri,
                    onFederatedSignInUriConsumed = { federatedSignInUri = null },
                    pendingMessage = pendingMessage,
                    onPendingMessageConsumed = { pendingMessage = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.federatedSignInUri()?.let { federatedSignInUri = it }
        intent.messageTarget()?.let { pendingMessage = it }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** The message to open if this intent came from a notification, unless relaunched from Recents. */
    private fun Intent.messageTarget(): MessageTarget? =
        MessageTarget.fromIntent(this).takeIf { flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0 }

    /** The redirect URI if this intent carries an FSSO sign in result, not a sign out redirect. */
    private fun Intent.federatedSignInUri(): Uri? =
        data?.takeIf { it.getQueryParameter("code") != null || it.getQueryParameter("error") != null }
}
