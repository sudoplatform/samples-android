/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.register

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.sudoplatform.emailexample.App
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.databinding.FragmentRegisterBinding
import com.sudoplatform.emailexample.mainmenu.MainMenuFragment
import com.sudoplatform.emailexample.util.ObjectDelegate
import com.sudoplatform.sudoentitlements.SudoEntitlementsClient
import com.sudoplatform.sudologging.AndroidUtilsLogDriver
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudouser.FederatedSignInResult
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

enum class RegistrationMethod { TEST, FSSO }

/**
 * This [RegisterFragment] presents a screen to allow the user to register or login.
 *
 * Links To:
 *  - [MainMenuFragment]: If a user successfully registers or logs in, the [MainMenuFragment] will
 *   be presented so that the user can view and create a Sudo.
 */
class RegisterFragment : Fragment(), CoroutineScope, AdapterView.OnItemSelectedListener {

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

    /** A list of registration methods. */
    private val registrationMethodList: ArrayList<String> = ArrayList()

    /** A reference to the [ArrayAdapter] holding the registration method data. */
    private lateinit var registrationMethodAdapter: ArrayAdapter<String>

    /** The selected registration method. */
    private var selectedRegistrationMethod = RegistrationMethod.TEST

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        app = requireActivity().application as App
        bindingDelegate.attach(FragmentRegisterBinding.inflate(inflater, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        binding.registrationMethodSpinner.onItemSelectedListener = this
        registrationMethodList.clear()
        registrationMethodList.addAll(
            listOf(
                getString(R.string.test_registration),
                getString(R.string.federated_signin),
            ),
        )
        registrationMethodAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            registrationMethodList,
        )
        registrationMethodAdapter.notifyDataSetChanged()
        binding.registrationMethodSpinner.adapter = registrationMethodAdapter

        binding.buttonRegister.setOnClickListener {
            registerAndSignIn()
            app.sudoProfilesClient.getSymmetricKeyId() ?: app.sudoProfilesClient.generateEncryptionKey()
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

        // Show the sign-out button if FSSO is a supported registration type
        val prefs = requireContext().getSharedPreferences(App.SIGN_IN_PREFERENCES, Context.MODE_PRIVATE)
        val usedFSSO = prefs.getBoolean(App.FSSO_USED_PREFERENCE, false)
        if (usedFSSO) {
            binding.buttonSignOut.text = getString(R.string.sign_out)
            binding.buttonSignOut.setOnClickListener {
                launch {
                    app.doFSSOSignout()
                }
            }
        } else {
            binding.buttonSignOut.text = getString(R.string.reset)
            binding.buttonSignOut.setOnClickListener {
                launch {
                    withContext(Dispatchers.IO) {
                        app.notificationHandler.unregister()
                        if (app.sudoUserClient.isRegistered()) {
                            app.sudoUserClient.deregister()
                        }
                    }
                }
            }
        }

        val federatedSignInUri = this.requireActivity().intent.data
        if (federatedSignInUri != null) {
            showLoading()
            launch {
                val result = withContext(Dispatchers.IO) {
                    suspendCoroutine { cont ->
                        app.sudoUserClient.processFederatedSignInTokens(federatedSignInUri) { result ->
                            cont.resume(result)
                        }
                    }
                }
                when (result) {
                    is FederatedSignInResult.Success -> {
                        setUsedFssoFlag(true)
                        launch {
                            redeemEntitlements()
                            withContext(Dispatchers.IO) {
                                app.notificationHandler.register()
                            }
                            navController.navigate(
                                RegisterFragmentDirections.actionRegisterFragmentToMainMenuFragment(),
                            )
                        }
                    }
                    is FederatedSignInResult.Failure -> {
                        showRegistrationFailure(result.error)
                    }
                }
            }
        } else {
            if (app.sudoUserClient.isSignedIn()) {
                launch {
                    redeemEntitlements()
                    withContext(Dispatchers.IO) {
                        app.notificationHandler.register()
                    }
                    navController.navigate(
                        RegisterFragmentDirections.actionRegisterFragmentToMainMenuFragment(),
                    )
                }
            } else if (app.sudoUserClient.isRegistered()) {
                registerAndSignIn()
            }
        }
    }

    /** Redeem entitlements from the [SudoEntitlementsClient]. */
    private suspend fun redeemEntitlements() {
        try {
            withContext(Dispatchers.IO) {
                app.sudoEntitlementsClient.redeemEntitlements()
            }
        } catch (e: SudoEntitlementsClient.EntitlementsException) {
            app.logger.outputError(Error(e))
            showRegistrationFailure(e)
        }
    }

    /** Perform registration and sign in from the [SudoUserClient]. */
    private fun registerAndSignIn() {
        setItemsEnabled(false)
        showLoading()
        if (selectedRegistrationMethod == RegistrationMethod.TEST) {
            if (app.sudoUserClient.isRegistered()) {
                // If already registered, sign in
                signIn()
            } else {
                loadRegistrationKeys()
            }
        } else {
            val activity = this.activity ?: return
            app.sudoUserClient.presentFederatedSignInUI(activity) { result ->
                when (result) {
                    is SignInResult.Success -> {
                        setUsedFssoFlag(true)
                        launch {
                            redeemEntitlements()
                            navController.navigate(
                                RegisterFragmentDirections.actionRegisterFragmentToMainMenuFragment(),
                            )
                        }
                    }
                    is SignInResult.Failure -> {
                        showRegistrationFailure(result.error)
                    }
                }
            }
        }
    }

    private fun setUsedFssoFlag(usedFsso: Boolean) {
        requireContext().getSharedPreferences(App.SIGN_IN_PREFERENCES, Context.MODE_PRIVATE)?.edit {
            putBoolean(App.FSSO_USED_PREFERENCE, usedFsso)
            commit()
        }
    }

    /** Performs sign in if the user is already registered. */
    private fun signIn() {
        setUsedFssoFlag(false)
        if (app.sudoUserClient.isSignedIn()) {
            launch {
                redeemEntitlements()
                withContext(Dispatchers.IO) {
                    app.notificationHandler.register()
                }
            }
        } else {
            launch {
                try {
                    withContext(Dispatchers.IO) {
                        app.sudoUserClient.signInWithKey()
                        app.notificationHandler.register()
                    }
                    setUsedFssoFlag(false)
                    launch {
                        redeemEntitlements()
                        navController.navigate(
                            RegisterFragmentDirections.actionRegisterFragmentToMainMenuFragment(),
                        )
                    }
                } catch (e: AuthenticationException) {
                    hideLoading()
                    setItemsEnabled(true)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.signin_failure, e.localizedMessage),
                        Toast.LENGTH_LONG,
                    ).show()
                }
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
            keyId,
        )
        // register with auth provider
        launch {
            try {
                withContext(Dispatchers.IO) {
                    app.sudoUserClient.registerWithAuthenticationProvider(
                        authProvider,
                        "dummy_rid",
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
        Toast.makeText(
            requireContext(),
            getString(R.string.register_failure, e.localizedMessage),
            Toast.LENGTH_LONG,
        ).show()
    }

    /**
     * Sets the register button to enabled/disabled and modifies associated text.
     *
     * @param isEnabled [Boolean] If true, the register button will be enabled with its
     *  corresponding text.
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
        if (bindingDelegate.isAttached()) {
            binding.progressBar.visibility = View.GONE
        }
    }

    /** Sets the registration method */
    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        selectedRegistrationMethod = if (pos == 0) RegistrationMethod.TEST else RegistrationMethod.FSSO
    }
    override fun onNothingSelected(parent: AdapterView<*>) { /* no-op */ }
}
