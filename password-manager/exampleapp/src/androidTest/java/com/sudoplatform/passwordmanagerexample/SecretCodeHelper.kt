/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.passwordmanagerexample

import java.io.File
import java.io.FileReader
import java.io.FileWriter

private const val SECRET_CODE_FILENAME = "secret_code.txt"

/**
 * Save the secret code to disc in case we need to enter it in a test to unlock.
 *
 * @since 2020-10-13
 */
fun saveSecretCode() {

    val app = AppHolder.app
        ?: throw AssertionError("AppHolder does not hold a reference to the App")

    if (app.sudoUserClient.isSignedIn()) {
        val secretCode = app.sudoPasswordManager.getSecretCode()
        if (secretCode?.isNotBlank() == true) {
            val secretCodeFile = File(app.filesDir, SECRET_CODE_FILENAME)
            FileWriter(secretCodeFile).use {
                it.write(secretCode)
            }
        }
    }
}

fun loadSecretCode(): String? {
    val app = AppHolder.app
        ?: throw AssertionError("AppHolder does not hold a reference to the App")

    val secretCodeFile = File(app.filesDir, SECRET_CODE_FILENAME)
    return FileReader(secretCodeFile).use {
        it.readText()
    }
}
