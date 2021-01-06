/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.passwordmanagerexample.sudos

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment
import com.sudoplatform.passwordmanagerexample.R

/** Navigates to a Sudo Platform web page when the "Learn More" button is pressed. */
fun Fragment.learnMoreAboutSudos() {
    val openUrl = Intent(Intent.ACTION_VIEW)
    openUrl.data = Uri.parse(getString(R.string.create_sudo_doc_url))
    startActivity(openUrl)
}
