/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample

import android.widget.TextView
import com.sudoplatform.sudopasswordmanager.models.SecureFieldValue

/**
 * Extensions of the Android TextView.
 */

/**
 * If a [TextView] has a non blank value then wrap it in a [SecureFieldValue] otherwise return null
 */
fun TextView.toSecureField(): SecureFieldValue? {
    val text = this.text.toString().trim()
    if (text.isNotEmpty()) {
        return SecureFieldValue(text)
    } else {
        return null
    }
}
