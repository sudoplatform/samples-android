/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.register

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.sudoplatform.passwordmanagerexample.App
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.sudoentitlements.SudoEntitlementsClient
import com.sudoplatform.sudouser.FederatedSignInResult
import com.sudoplatform.sudouser.RegistrationChallengeType
import com.sudoplatform.sudouser.SignInResult
import com.sudoplatform.sudouser.TESTAuthenticationProvider
import com.sudoplatform.sudouser.exceptions.AuthenticationException
import com.sudoplatform.sudouser.exceptions.RegisterException
import java.io.IOException
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_register.buttonRegister
import kotlinx.android.synthetic.main.fragment_register.buttonSignOut
import kotlinx.android.synthetic.main.fragment_register.progressBar
import kotlinx.android.synthetic.main.fragment_register.view.buttonRegister
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This [RegisterFragment] presents a screen to allow the user to register or login.
 *
 * Links To:
 *  - [CreateMasterPasswordFragment]: If a user successfully registers or logs in and if
 *  no master password has already been set.
 *  - [UnlockVaultsFragment]: If a user is already registered and logged in and a master password
 *  was previously set.
 */
class RegisterFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    lateinit var app: App

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        app = requireActivity().application as App
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        view.buttonRegister.setOnClickListener {
            registerAndSignIn()
        }
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        hideLoading()
        setItemsEnabled(true)

        // Show the sign-out button if FSSO is a supported registration type
        val challengeTypes = app.sudoUserClient.getSupportedRegistrationChallengeType()
        if (challengeTypes.contains(RegistrationChallengeType.FSSO)) {
            this.buttonSignOut.setOnClickListener {
                launch {
                    app.doFSSOSSignout()
                }
            }

            this.buttonSignOut.visibility = View.VISIBLE
        }

        val federatedSignInUri = requireActivity().intent.data
        if (federatedSignInUri != null) {
            showLoading()
            app.sudoUserClient.processFederatedSignInTokens(federatedSignInUri) { result ->
                when (result) {
                    is FederatedSignInResult.Success -> {
                        redeemEntitlements()
                        navigateToNextFragment()
                    }
                    is FederatedSignInResult.Failure -> {
                        showRegistrationFailure(result.error)
                    }
                }
            }
        } else {
            if (app.sudoUserClient.isSignedIn()) {
                redeemEntitlements()
                navigateToNextFragment()
            } else if (app.sudoUserClient.isRegistered()) {
                registerAndSignIn()
            }
        }
    }

    private fun redeemEntitlements() {
        launch {
            try {
                showLoading()
                withContext(Dispatchers.IO) {
                    app.sudoEntitlementsClient.getEntitlements()
                        ?: app.sudoEntitlementsClient.redeemEntitlements()
                }
                hideLoading()
            } catch (e: SudoEntitlementsClient.EntitlementsException) {
                hideLoading()
                app.logger.outputError(Error(e))
                showRegistrationFailure(e)
            }
        }
    }

    private fun navigateToNextFragment() {
        if (app.sudoPasswordManager.isLocked()) {
            navController.navigate(R.id.action_registerFragment_to_unlockVaultsFragment)
        } else {
            navController.navigate(R.id.action_registerFragment_to_sudosFragment)
        }
    }

    /** Perform registration and sign in from the [SudoUserClient]. */
    private fun registerAndSignIn() {
        setItemsEnabled(false)
        showLoading()
        val challengeTypes = app.sudoUserClient.getSupportedRegistrationChallengeType()
        if (challengeTypes.contains(RegistrationChallengeType.FSSO)) {
            val sharedPreferences = context?.getSharedPreferences("SignIn", Context.MODE_PRIVATE)?.edit()
            sharedPreferences?.putBoolean("usedFSSO", true)
            sharedPreferences?.apply()
            app.sudoUserClient.presentFederatedSignInUI { result ->
                when (result) {
                    is SignInResult.Success -> {
                        redeemEntitlements()
                        navigateToNextFragment()
                    }
                    is SignInResult.Failure -> {
                        showRegistrationFailure(result.error)
                    }
                }
            }
        } else {
            if (app.sudoUserClient.isRegistered()) {
                // If already registered, sign in
                signIn()
            } else {
                loadRegistrationKeys()
            }
        }
    }

    /** Performs sign in if the user is already registered. */
    private fun signIn() {
        if (app.sudoUserClient.isSignedIn()) {
            redeemEntitlements()
            navigateToNextFragment()
            return
        }
        launch {
            try {
                withContext(Dispatchers.IO) {
                    app.sudoUserClient.signInWithKey()
                }
                redeemEntitlements()

                val sharedPreferences = context?.getSharedPreferences("SignIn", Context.MODE_PRIVATE)
                val preferenceEditor = sharedPreferences?.edit()
                preferenceEditor?.putBoolean("usedFSSO", false)
                preferenceEditor?.commit()

                navigateToNextFragment()
            } catch (e: AuthenticationException) {
                hideLoading()
                setItemsEnabled(true)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.signin_failure, e.localizedMessage),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /** Loads registration keys from the assets directory used as part of registration. */
    private fun loadRegistrationKeys() {
        val privateKey: String
        val keyId: String

        try {
            privateKey =
                app.assets.open("register_key.private").bufferedReader().use {
                    it.readText().trim()
                }
            keyId =
                app.assets.open("register_key.id").bufferedReader().use {
                    it.readText().trim()
                }
        } catch (e: IOException) {
            app.logger.error(getString(R.string.registration_keys_failure))
            app.logger.outputError(Error(e))
            showRegistrationFailure(e)
            return
        }

        val authProvider = TESTAuthenticationProvider(
            "testRegisterAudience",
            privateKey,
            null,
            app.keyManager,
            keyId
        )
        // register with auth provider
        launch {
            try {
                withContext(Dispatchers.IO) {
                    app.sudoUserClient.registerWithAuthenticationProvider(
                        authProvider,
                        "dummy_rid"
                    )
                }
                signIn()
            } catch (e: RegisterException) {
                showRegistrationFailure(e)
            }
        }
    }

    /** Displays a Toast with a registration failure. */
    private val showRegistrationFailure = { e: Throwable ->
        // Don't attempt to show an error the fragment is no longer displayed
        if (isVisible) {
            hideLoading()
            setItemsEnabled(true)
            Toast.makeText(
                requireContext(),
                getString(R.string.register_failure, e.localizedMessage),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Sets the register button to enabled/disabled and modifies associated text.
     *
     * @param isEnabled If true, the register button will be enabled with its corresponding text.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        if (isEnabled) {
            buttonRegister?.text = getString(R.string.register_login)
        } else {
            buttonRegister?.text = ""
        }
        buttonRegister?.isEnabled = isEnabled
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading() {
        progressBar?.visibility = View.VISIBLE
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        progressBar?.visibility = View.GONE
    }
}
