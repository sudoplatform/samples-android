/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.register

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sudoplatform.sudoentitlements.SudoEntitlementsClient
import com.sudoplatform.sudouser.FederatedSignInResult
import com.sudoplatform.sudouser.SignInResult
import com.sudoplatform.sudouser.SudoUserClient
import com.sudoplatform.sudouser.TESTAuthenticationProvider
import com.sudoplatform.sudouser.exceptions.SudoUserException
import com.sudoplatform.telephonyexample.App
import com.sudoplatform.telephonyexample.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

enum class RegistrationMethod { TEST, FSSO }

data class RegisterUiState(
    val registrationMethod: RegistrationMethod = RegistrationMethod.TEST,
    val isLoading: Boolean = false,
    /** True if the last sign in used FSSO, in which case the corner button signs out rather than resets. */
    val usedFsso: Boolean = false,
    val configurationError: String? = null,
)

sealed interface RegisterEvent {
    data object NavigateToSudos : RegisterEvent

    data class ShowMessage(
        @StringRes val messageResId: Int,
        val detail: String,
    ) : RegisterEvent
}

/**
 * Registers and signs the user in with either a TEST registration key or Federated Single Sign
 * On, then redeems entitlements before handing over to the Sudos screen.
 */
class RegisterViewModel(
    private val app: App,
) : ViewModel() {
    companion object {
        val Factory =
            viewModelFactory {
                initializer { RegisterViewModel(this[APPLICATION_KEY] as App) }
            }

        private const val IDENTITY_ID_CLAIM = "custom:identityId"
        private const val FEDERATED_ID_REGISTRATION_TIMEOUT_MS = 15_000L
    }

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _events = Channel<RegisterEvent>(Channel.BUFFERED)
    val events: Flow<RegisterEvent> = _events.receiveAsFlow()

    private val sudoUserClient: SudoUserClient get() = app.sudoUserClient

    private val preferences get() = app.getSharedPreferences(App.SIGN_IN_PREFERENCES, Context.MODE_PRIVATE)

    /** True between presenting the FSSO browser tab and the app being resumed. */
    private var awaitingFederatedRedirect = false

    fun selectRegistrationMethod(method: RegistrationMethod) {
        _uiState.update { it.copy(registrationMethod = method) }
    }

    /**
     * Resume a session when the screen becomes visible: complete an FSSO redirect if one was
     * delivered, otherwise skip straight to the Sudos screen when already signed in, or sign in
     * automatically when already registered.
     */
    fun resumeSession(
        activity: Activity,
        federatedSignInUri: Uri?,
    ) {
        if (reportInitializationError()) {
            return
        }
        _uiState.update { it.copy(usedFsso = preferences.getBoolean(App.FSSO_USED_PREFERENCE, false)) }

        // An FSSO redirect is delivered while sign in is still in progress, so handle it first.
        when {
            federatedSignInUri != null -> {
                awaitingFederatedRedirect = false
                processFederatedSignIn(activity, federatedSignInUri)
            }
            // Returning from the sign in page without a redirect means the user closed it.
            awaitingFederatedRedirect -> {
                awaitingFederatedRedirect = false
                setLoading(false)
            }
            _uiState.value.isLoading -> return
            sudoUserClient.isSignedIn() -> viewModelScope.launch { completeSignIn() }
            sudoUserClient.isRegistered() -> signIn()
        }
    }

    /** Perform registration and sign in with the selected [RegistrationMethod]. */
    fun registerAndSignIn(activity: Activity) {
        if (reportInitializationError()) {
            return
        }
        when (_uiState.value.registrationMethod) {
            RegistrationMethod.TEST ->
                if (sudoUserClient.isRegistered()) {
                    signIn()
                } else {
                    registerWithTestKey()
                }
            RegistrationMethod.FSSO -> {
                // Federated sign in needs a `federatedSignIn` section in sudoplatformconfig.json.
                if (!isFederatedSignInConfigured()) {
                    _events.trySend(
                        RegisterEvent.ShowMessage(R.string.register_failure, app.getString(R.string.fsso_not_configured)),
                    )
                    return
                }
                setLoading(true)
                awaitingFederatedRedirect = true
                sudoUserClient.presentFederatedSignInUI(activity) { result ->
                    when (result) {
                        is SignInResult.Success -> {
                            setUsedFsso(true)
                            viewModelScope.launch { completeSignIn() }
                        }
                        is SignInResult.Failure -> fail(R.string.register_failure, result.error)
                    }
                }
            }
        }
    }

    /** Sign out of FSSO, or for TEST registrations deregister so the next login starts afresh. */
    fun signOutOrReset() {
        if (reportInitializationError()) {
            return
        }
        viewModelScope.launch {
            try {
                if (_uiState.value.usedFsso) {
                    app.signOutFederated(removeData = false)
                    _uiState.update { it.copy(usedFsso = false) }
                } else {
                    withContext(Dispatchers.IO) {
                        if (sudoUserClient.isRegistered()) {
                            app.deregister()
                        }
                    }
                }
            } catch (e: SudoUserException) {
                _events.send(RegisterEvent.ShowMessage(R.string.register_failure, e.describe()))
            }
        }
    }

    fun dismissConfigurationError() {
        _uiState.update { it.copy(configurationError = null) }
    }

    private fun reportInitializationError(): Boolean {
        val error = app.initializationError ?: return false
        _uiState.update { it.copy(configurationError = error.describe()) }
        return true
    }

    private fun registerWithTestKey() {
        setLoading(true)
        viewModelScope.launch {
            val (privateKey, keyId) =
                try {
                    withContext(Dispatchers.IO) {
                        Pair(readAsset("register_key.private"), readAsset("register_key.id"))
                    }
                } catch (e: IOException) {
                    app.logger.error("Failed to load TEST registration keys: ${e.localizedMessage}")
                    fail(R.string.registration_keys_failure, e)
                    return@launch
                }

            val authProvider =
                TESTAuthenticationProvider(
                    "testRegisterAudience",
                    privateKey,
                    null,
                    app.keyManager,
                    keyId,
                )
            try {
                withContext(Dispatchers.IO) {
                    sudoUserClient.registerWithAuthenticationProvider(authProvider, "dummy_rid")
                }
                signIn()
            } catch (e: SudoUserException) {
                fail(R.string.register_failure, e)
            }
        }
    }

    private fun signIn() {
        setLoading(true)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (!sudoUserClient.isSignedIn()) {
                        sudoUserClient.signInWithKey()
                    }
                }
                setUsedFsso(false)
                completeSignIn()
            } catch (e: SudoUserException) {
                fail(R.string.signin_failure, e)
            }
        }
    }

    private fun processFederatedSignIn(
        activity: Activity,
        uri: Uri,
    ) {
        setLoading(true)
        viewModelScope.launch {
            val result =
                withContext(Dispatchers.IO) {
                    suspendCoroutine { cont ->
                        sudoUserClient.processFederatedSignInTokens(uri) { cont.resume(it) }
                    }
                }
            when (result) {
                is FederatedSignInResult.Success -> {
                    setUsedFsso(true)
                    if (sudoUserClient.userClaimOrNull(IDENTITY_ID_CLAIM) == null && !registerFederatedIdentity(activity)) {
                        return@launch
                    }
                    completeSignIn()
                }
                is FederatedSignInResult.Failure -> fail(R.string.register_failure, result.error)
            }
        }
    }

    /**
     * Temporary workaround: `processFederatedSignInTokens` doesn't register the federated identity,
     * but `presentFederatedSignInUI` does, and with the session the redirect just cached it
     * completes without showing the sign in page.
     */
    private suspend fun registerFederatedIdentity(activity: Activity): Boolean {
        val result =
            withTimeoutOrNull(FEDERATED_ID_REGISTRATION_TIMEOUT_MS) {
                suspendCoroutine { cont ->
                    sudoUserClient.presentFederatedSignInUI(activity) { cont.resume(it) }
                }
            }
        return when (result) {
            null -> {
                app.logger.warning("Federated identity registration did not complete, continuing without it")
                true
            }
            is SignInResult.Success -> true
            is SignInResult.Failure -> {
                fail(R.string.register_failure, result.error)
                false
            }
        }
    }

    private fun SudoUserClient.userClaimOrNull(name: String): Any? =
        try {
            getUserClaim(name)
        } catch (e: Exception) {
            null
        }

    /**
     * Redeem entitlements, then continue to the Sudos screen even if redemption fails. An expired
     * session is cleared, then TEST registrations sign in again and FSSO users are asked to.
     */
    private suspend fun completeSignIn() {
        setLoading(true)
        try {
            try {
                redeemEntitlements()
            } catch (e: SudoUserException.NotAuthorizedException) {
                withContext(Dispatchers.IO) { sudoUserClient.clearAuthTokens() }
                if (_uiState.value.usedFsso) {
                    setLoading(false)
                    _events.send(RegisterEvent.ShowMessage(R.string.signin_failure, app.getString(R.string.session_expired)))
                    return
                }
                withContext(Dispatchers.IO) { sudoUserClient.signInWithKey() }
                redeemEntitlements()
            }
        } catch (e: SudoUserException) {
            fail(R.string.signin_failure, e)
            return
        }
        try {
            withContext(Dispatchers.IO) { app.prepareForSignedInUser() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fail(R.string.signin_failure, e)
            return
        }
        _events.send(RegisterEvent.NavigateToSudos)
        setLoading(false)
    }

    private suspend fun redeemEntitlements() {
        try {
            withContext(Dispatchers.IO) {
                app.sudoEntitlementsClient.redeemEntitlements()
            }
        } catch (e: SudoEntitlementsClient.EntitlementsException) {
            app.logger.outputError(Error(e))
            _events.send(RegisterEvent.ShowMessage(R.string.register_failure, e.describe()))
        }
    }

    private fun fail(
        @StringRes messageResId: Int,
        error: Throwable,
    ) {
        setLoading(false)
        _events.trySend(RegisterEvent.ShowMessage(messageResId, error.describe()))
    }

    private fun setLoading(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }

    private fun setUsedFsso(usedFsso: Boolean) {
        preferences.edit { putBoolean(App.FSSO_USED_PREFERENCE, usedFsso) }
        _uiState.update { it.copy(usedFsso = usedFsso) }
    }

    private fun isFederatedSignInConfigured(): Boolean =
        try {
            JSONObject(readAsset("sudoplatformconfig.json")).has("federatedSignIn")
        } catch (e: Exception) {
            false
        }

    private fun readAsset(name: String): String =
        app.assets
            .open(name)
            .bufferedReader()
            .use { it.readText().trim() }

    private fun Throwable.describe(): String = localizedMessage ?: toString()
}
