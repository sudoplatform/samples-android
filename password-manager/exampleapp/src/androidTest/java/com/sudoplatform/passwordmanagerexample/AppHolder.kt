/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample

import com.sudoplatform.sudopasswordmanager.PasswordManagerRegistrationStatus
import com.sudoplatform.sudoprofiles.ListOption
import kotlinx.coroutines.runBlocking

/**
 * A singleton used to hold a reference to the [App] so that test code can
 * access the [SudoPasswordManagerClient] API held in it.
 *
 * @since 2020-10-13
 */
object AppHolder {

    var app: App? = null
        private set

    fun holdApp(app: App) {
        this.app = app
    }

    fun getRegistrationStatus() = runBlocking<PasswordManagerRegistrationStatus> {
        val app = app
            ?: return@runBlocking PasswordManagerRegistrationStatus.NOT_REGISTERED
        if (app.sudoUserClient.isSignedIn()) {
            app.sudoPasswordManager.getRegistrationStatus()
        } else {
            PasswordManagerRegistrationStatus.NOT_REGISTERED
        }
    }

    fun resetPasswordManager() = runBlocking {
        val app = app
            ?: throw AssertionError("AppHolder does not have a reference to the App under test")
        with(app.sudoPasswordManager) {
            lock()
            reset()
        }
    }

    fun isLocked() = runBlocking<Boolean> {
        val app = app
            ?: throw AssertionError("AppHolder does not have a reference to the App under test")
        app.sudoPasswordManager.isLocked()
    }

    fun deleteSudos() = runBlocking {
        val app = app
            ?: throw AssertionError("AppHolder does not have a reference to the App under test")
        app.sudoProfilesClient.listSudos(ListOption.REMOTE_ONLY)
            .forEach { sudo ->
                app.sudoProfilesClient.deleteSudo(sudo)
            }
    }
}
