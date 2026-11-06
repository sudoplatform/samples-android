/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.userdetails

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sudoplatform.telephonyexample.App
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Date

data class UserDetailsUiState(
    /**
     * The user's SudoUser user name, which support needs to diagnose an issue with a specific
     * user, without the identity provider prefix of a federated user, e.g. "Auth0_auth0|".
     */
    val userId: String? = null,
    /** The user's subject, which services record as the owner of the user's data. */
    val subject: String? = null,
    val usedFsso: Boolean = false,
    /** When the user last signed in, from the ID token's `auth_time` claim. */
    val signedInAt: Date? = null,
    /** When the current ID token expires, from its `exp` claim. */
    val idTokenExpiresAt: Date? = null,
    /** When the refresh token expires, after which the user has to sign in again. */
    val refreshTokenExpiresAt: Date? = null,
    val loadError: String? = null,
)

/** Shows the signed in user's account identifiers and session times. */
class UserDetailsViewModel(
    private val app: App,
) : ViewModel() {
    companion object {
        val Factory =
            viewModelFactory {
                initializer { UserDetailsViewModel(this[APPLICATION_KEY] as App) }
            }
    }

    private val _uiState = MutableStateFlow(UserDetailsUiState())
    val uiState: StateFlow<UserDetailsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        val client = app.sudoUserClient
        _uiState.value =
            try {
                UserDetailsUiState(
                    userId = client.getUserName()?.substringAfterLast('|'),
                    subject = client.getSubject(),
                    usedFsso =
                        app
                            .getSharedPreferences(App.SIGN_IN_PREFERENCES, Context.MODE_PRIVATE)
                            .getBoolean(App.FSSO_USED_PREFERENCE, false),
                    signedInAt = dateFromClaim(client.getUserClaim("auth_time")),
                    idTokenExpiresAt = dateFromClaim(client.getUserClaim("exp")),
                    refreshTokenExpiresAt = client.getRefreshTokenExpiry(),
                )
            } catch (e: Exception) {
                app.logger.error("Failed to get user details: $e")
                UserDetailsUiState(loadError = e.localizedMessage ?: e.toString())
            }
    }

    fun dismissLoadError() {
        _uiState.update { it.copy(loadError = null) }
    }

    /** A date from a JWT time claim, which is seconds since the Unix epoch. */
    private fun dateFromClaim(claim: Any?): Date? = (claim as? Number)?.let { Date(it.toLong() * 1000) }
}
