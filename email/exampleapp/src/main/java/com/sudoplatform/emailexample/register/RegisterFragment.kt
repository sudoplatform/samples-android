/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.sudoplatform.emailexample.App
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.databinding.FragmentRegisterBinding
import com.sudoplatform.emailexample.mainmenu.MainMenuFragment
import com.sudoplatform.emailexample.util.ObjectDelegate
import com.sudoplatform.sudologging.AndroidUtilsLogDriver
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudouser.FederatedSignInResult
import com.sudoplatform.sudouser.RegistrationChallengeType
import com.sudoplatform.sudouser.SignInResult
import com.sudoplatform.sudouser.SudoUserClient
import com.sudoplatform.sudouser.TESTAuthenticationProvider
import com.sudoplatform.sudouser.exceptions.AuthenticationException
import com.sudoplatform.sudouser.exceptions.RegisterException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * This [RegisterFragment] presents a screen to allow the user to register or login.
 *
 * Links To:
 *  - [MainMenuFragment]: If a user successfully registers or logs in, the [MainMenuFragment] will
 *   be presented so that the user can view and create a Sudo.
 */
class RegisterFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** [Logger] used to log errors during registration. */
    private val errorLogger = Logger("emailSample", AndroidUtilsLogDriver(LogLevel.ERROR))

    /** The [App] that holds references to the APIs this fragment needs. */
    lateinit var app: App

    /** View binding to the views defined in the layout */
    private val bindingDelegate = ObjectDelegate<FragmentRegisterBinding>()
    private val binding by bindingDelegate

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingDelegate.attach(FragmentRegisterBinding.inflate(inflater, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        binding.buttonRegister.setOnClickListener {
            registerAndSignIn()
            app.sudoProfilesClient.getSymmetricKeyId() ?: app.sudoProfilesClient.generateEncryptionKey()
        }

        // proceed to signIn operation if already registered
        app = requireActivity().application as App
        if (app.sudoUserClient.isRegistered()) {
            registerAndSignIn()
        }
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        hideLoading()
        setItemsEnabled(true)
        val data = this.requireActivity().intent.data
        if (data != null) {
            launch {
                val result = withContext(Dispatchers.IO) {
                    suspendCoroutine<FederatedSignInResult> { cont ->
                        app.sudoUserClient.processFederatedSignInTokens(data) { result ->
                            cont.resume(result)
                        }
                    }
                }
                when (result) {
                    is FederatedSignInResult.Success -> {
                        navController.navigate(
                            RegisterFragmentDirections.actionRegisterFragmentToMainMenuFragment()
                        )
                    }
                    is FederatedSignInResult.Failure -> {
                        showRegistrationFailure(result.error)
                    }
                }
            }
        }
    }

    /** Perform registration and sign in from the [SudoUserClient]. */
    private fun registerAndSignIn() {
        setItemsEnabled(false)
        showLoading()
        app = requireActivity().application as App
        val challengeTypes = app.sudoUserClient.getSupportedRegistrationChallengeType()
        if (challengeTypes.contains(RegistrationChallengeType.FSSO)) {
            launch {
                val result = withContext(Dispatchers.IO) {
                    suspendCoroutine<SignInResult> { cont ->
                        app.sudoUserClient.presentFederatedSignInUI { result ->
                            cont.resume(result)
                        }
                    }
                }
                when (result) {
                    is SignInResult.Success -> {
                        navController.navigate(
                            RegisterFragmentDirections.actionRegisterFragmentToMainMenuFragment()
                        )
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
            navController.navigate(
                RegisterFragmentDirections.actionRegisterFragmentToMainMenuFragment()
            )
            return
        }
        launch {
            try {
                withContext(Dispatchers.IO) {
                    app.sudoUserClient.signInWithKey()
                }
                navController.navigate(
                    RegisterFragmentDirections.actionRegisterFragmentToMainMenuFragment()
                )
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
            errorLogger.error(getString(R.string.registration_keys_failure))
            errorLogger.outputError(Error(e))
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
        hideLoading()
        setItemsEnabled(true)
        Toast.makeText(
            requireContext(),
            getString(R.string.register_failure, e.localizedMessage),
            Toast.LENGTH_LONG
        ).show()
    }

    /**
     * Sets the register button to enabled/disabled and modifies associated text.
     *
     * @param isEnabled If true, the register button will be enabled with its corresponding text.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        if (isEnabled) {
            binding.buttonRegister.text = getString(R.string.register_login)
        } else {
            binding.buttonRegister.text = ""
        }
        binding.buttonRegister.isEnabled = isEnabled
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
    }
}
