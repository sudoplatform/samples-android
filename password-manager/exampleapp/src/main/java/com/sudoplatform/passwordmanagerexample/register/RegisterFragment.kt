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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.sudoplatform.passwordmanagerexample.App
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.databinding.FragmentRegisterBinding
import com.sudoplatform.passwordmanagerexample.util.ObjectDelegate
import com.sudoplatform.sudoentitlements.SudoEntitlementsClient
import com.sudoplatform.sudouser.FederatedSignInResult
import com.sudoplatform.sudouser.RegistrationChallengeType
import com.sudoplatform.sudouser.SignInResult
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
import java.util.ArrayList
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

enum class RegistrationMethod { TEST, FSSO }
/**
 * This [RegisterFragment] presents a screen to allow the user to register or login.
 *
 * Links To:
 *  - [CreateMasterPasswordFragment]: If a user successfully registers or logs in and if
 *  no master password has already been set.
 *  - [UnlockVaultsFragment]: If a user is already registered and logged in and a master password
 *  was previously set.
 */
class RegisterFragment : Fragment(), CoroutineScope, AdapterView.OnItemSelectedListener {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** View binding to the views defined in the layout */
    private val bindingDelegate = ObjectDelegate<FragmentRegisterBinding>()
    private val binding by bindingDelegate

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController
    private val registrationMethodList: ArrayList<String> = ArrayList()
    private lateinit var registrationMethodAdapter: ArrayAdapter<String>
    private var selectedRegistrationMethod = RegistrationMethod.TEST

    lateinit var app: App

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        app = requireActivity().application as App
        bindingDelegate.attach(FragmentRegisterBinding.inflate(inflater, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        binding.registrationMethodSpinner.onItemSelectedListener = this
        val challengeTypes = app.sudoUserClient.getSupportedRegistrationChallengeType()
        registrationMethodList.clear()
        registrationMethodList.add(getString(R.string.test_registration))
        if (challengeTypes.contains(RegistrationChallengeType.FSSO)) {
            registrationMethodList.add(getString(R.string.federated_signin))
        }
        registrationMethodAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, registrationMethodList)
        registrationMethodAdapter.notifyDataSetChanged()
        binding.registrationMethodSpinner.adapter = registrationMethodAdapter
        binding.buttonRegister.setOnClickListener {
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

        // Show the sign-out button if FSSO is a supported registration type
        val prefs = requireContext().getSharedPreferences(App.SIGN_IN_PREFERENCES, Context.MODE_PRIVATE)
        val usedFSSO = prefs.getBoolean(App.FSSO_USED_PREFERENCE, false)
        if (usedFSSO == true) {
            binding.buttonSignOut.setText(getString(R.string.sign_out))
            binding.buttonSignOut.setOnClickListener {
                launch {
                    app.doFSSOSSignout()
                }
            }
        } else {
            binding.buttonSignOut.setText(getString(R.string.reset))
            binding.buttonSignOut.setOnClickListener {
                launch {
                    withContext(Dispatchers.IO) {
                        if (app.sudoUserClient.isRegistered()) {
                            app.sudoUserClient.deregister()
                        }
                        app.sudoPasswordManager.reset()
                    }
                }
            }
        }

        val federatedSignInUri = requireActivity().intent.data
        if (federatedSignInUri != null) {
            showLoading()
            launch {
                val result = withContext(Dispatchers.IO) {
                    suspendCoroutine<FederatedSignInResult> { cont ->
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
                            navigateToNextFragment()
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
                    navigateToNextFragment()
                }
            } else if (app.sudoUserClient.isRegistered()) {
                registerAndSignIn()
            }
        }
    }

    private suspend fun redeemEntitlements() {
        try {
            // Redeem entitlements if they have not already been redeemed
            showLoading()
            withContext(Dispatchers.IO) {
                try {
                    // Check if entitlements must be redeemed. If they must be, this method will throw.
                    app.sudoEntitlementsClient.getEntitlementsConsumption()
                } catch (e: SudoEntitlementsClient.EntitlementsException.NoEntitlementsException) {
                    app.sudoEntitlementsClient.redeemEntitlements()
                }
            }
        } catch (e: SudoEntitlementsClient.EntitlementsException) {
            hideLoading()
            app.logger.outputError(Error(e))
            showRegistrationFailure(e)
        }
    }

    private fun navigateToNextFragment() {
        if (app.sudoPasswordManager.isLocked()) {
            // When testing with Espresso the ActivityScenario seems to do something that causes
            // the nav controller to already be on the unlock vaults fragment
            if (navController.currentDestination?.id != R.id.unlockVaultsFragment) {
                navController.navigate(RegisterFragmentDirections.actionRegisterFragmentToUnlockVaultsFragment())
            }
        } else {
            // When testing with Espresso the ActivityScenario seems to do something that causes
            // the nav controller to already be on the sudos fragment
            if (navController.currentDestination?.id != R.id.sudosFragment) {
                navController.navigate(RegisterFragmentDirections.actionRegisterFragmentToSudosFragment())
            }
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
                            navigateToNextFragment()
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
                navigateToNextFragment()
            }
            return
        }
        launch {
            try {
                withContext(Dispatchers.IO) {
                    app.sudoUserClient.signInWithKey()
                }
                setUsedFssoFlag(false)
                launch {
                    redeemEntitlements()
                    navigateToNextFragment()
                }
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
        hideLoading()
        if (isVisible) {
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

    override fun onNothingSelected(parent: AdapterView<*>) {
        // Don't care
    }
}
