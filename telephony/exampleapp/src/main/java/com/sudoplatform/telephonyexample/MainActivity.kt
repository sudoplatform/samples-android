/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sudoplatform.telephonyexample.ui.theme.TelephonyExampleTheme

/**
 * This is the first activity that is invoked when the app is launched.
 * It hosts the Compose navigation graph and receives Federated Single Sign On redirects.
 */
class MainActivity : ComponentActivity() {
    /** An FSSO redirect not yet handed to the register screen. */
    private var federatedSignInUri: Uri? by mutableStateOf(null)

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
        }
        setContent {
            TelephonyExampleTheme {
                AppNavHost(
                    federatedSignInUri = federatedSignInUri,
                    onFederatedSignInUriConsumed = { federatedSignInUri = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.federatedSignInUri()?.let { federatedSignInUri = it }
    }

    /** The redirect URI if this intent carries an FSSO sign in result, not a sign out redirect. */
    private fun Intent.federatedSignInUri(): Uri? =
        data?.takeIf { it.getQueryParameter("code") != null || it.getQueryParameter("error") != null }
}
