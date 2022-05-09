/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.register

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.safetynet.SafetyNet
import com.sudoplatform.direlayexample.App
import com.sudoplatform.direlayexample.R
import com.sudoplatform.direlayexample.databinding.FragmentRegisterBinding
import com.sudoplatform.direlayexample.postboxes.PostboxesFragment
import com.sudoplatform.direlayexample.util.ObjectDelegate
import com.sudoplatform.sudoentitlements.SudoEntitlementsClient
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
import java.util.UUID
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

enum class RegistrationMethod { TEST, FSSO, SAFETY_NET }

/**
 * This [RegisterFragment] presents a start display
 *
 * Links To:
 *  - [PostboxesFragment]: When the "register" button is clicked, registration and sign in will be attempted,
 *   on completion, the [PostboxesFragment] will be presented so that the user can view and create Postboxes.
 */
class RegisterFragment : Fragment(), CoroutineScope, AdapterView.OnItemSelectedListener {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** [Logger] used to log errors during registration. */
    private val errorLogger = Logger("diSample", AndroidUtilsLogDriver(LogLevel.ERROR))

    /** The [App] that holds references to the APIs this fragment needs. */
    lateinit var app: App

    /** The API Key for querying SafetyNet. */
    private lateinit var safetyNetApiKey: String

    /** A flag for whether the safetyNet API key is available for usage. */
    private var safetyNetApiKeyAvailable = true

    /** A list of registration methods. */
    private val registrationMethodList: ArrayList<String> = ArrayList()

    /** A reference to the [ArrayAdapter] holding the registration method data. */
    private lateinit var registrationMethodAdapter: ArrayAdapter<String>

    /** The selected registration method. */
    private var selectedRegistrationMethod = RegistrationMethod.TEST

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

        app = requireActivity().application as App

        // check if safetyNet API available
        try {
            safetyNetApiKey =
                resources.openRawResource(
                    resources.getIdentifier(
                        "safetynet_api_dev",
                        "raw",
                        app.packageName
                    )
                )
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
                    .trim()
        } catch (e: Resources.NotFoundException) {
            safetyNetApiKeyAvailable = false
        }

        // get registration options
        binding.registrationMethodSpinner.onItemSelectedListener = this
        val challengeTypes = app.sudoUserClient.getSupportedRegistrationChallengeType()
        registrationMethodList.clear()
        registrationMethodList.add(getString(R.string.test_registration))
        if (challengeTypes.contains(RegistrationChallengeType.FSSO)) {
            registrationMethodList.add(getString(R.string.federated_signin))
        }
        if (challengeTypes.contains(RegistrationChallengeType.SAFETY_NET)) {
            registrationMethodList.add(getString(R.string.safety_net_reg))
        }
        registrationMethodAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            registrationMethodList
        )
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

    private fun resetIntentData() {
        requireActivity().intent.replaceExtras(Bundle())
        requireActivity().intent.action = ""
        requireActivity().intent.data = null
        requireActivity().intent.flags = 0
    }

    private fun handleFSSOSignInRedirect(federatedSignInUri: Uri) {
        showLoading()
        setItemsEnabled(false)
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
                        resetIntentData()
                        navigateToSudos()
                    }
                }
                is FederatedSignInResult.Failure -> {
                    showRegistrationFailure(result.error)
                }
            }
        }
    }

    private fun handleFSSOSignOutRedirect() {
        launch {
            try {
                showLoading()
                setItemsEnabled(false)

                setUsedFssoFlag(false)
                setSignOutButtonToReset()
                app.sudoUserClient.globalSignOut()
                app.sudoUserClient.clearAuthTokens()
                resetIntentData()
            } catch (e: Exception) {
                errorLogger.error(e.localizedMessage)
            }
            hideLoading()
            setItemsEnabled(true)
        }
    }

    override fun onResume() {
        super.onResume()
        launch {
            showLoading()
            setItemsEnabled(false)

            // Show the sign-out button if FSSO is a supported registration type
            val prefs =
                requireContext().getSharedPreferences(App.SIGN_IN_PREFERENCES, Context.MODE_PRIVATE)
            val usedFSSO = prefs.getBoolean(App.FSSO_USED_PREFERENCE, false)
            if (usedFSSO) {
                binding.buttonSignOut.text = getString(R.string.sign_out)
                binding.buttonSignOut.setOnClickListener {
                    app.doFSSOSignout()
                }
            } else {
                setSignOutButtonToReset()
            }

            val federatedSignInUri = requireActivity().intent.data
            when (federatedSignInUri?.path) {
                "/signin" -> {
                    handleFSSOSignInRedirect(federatedSignInUri)
                }
                "/signout" -> {
                    handleFSSOSignOutRedirect()
                }
                else -> {
                    if (app.sudoUserClient.isSignedIn()) {
                        navigateToSudos()
                    } else if (app.sudoUserClient.isRegistered()) {
                        registerAndSignIn()
                    }
                }
            }
            hideLoading()
            setItemsEnabled(true)
        }
    }

    private fun setSignOutButtonToReset() {
        binding.buttonSignOut.text = getString(R.string.reset)
        binding.buttonSignOut.setOnClickListener {
            launch {
                withContext(Dispatchers.IO) {
                    if (app.sudoUserClient.isRegistered()) {
                        app.sudoUserClient.deregister()
                    }
                    app.sudoUserClient.reset()
                }
            }
        }
    }

    private suspend fun getAttestationResult(nonce: String): String? = suspendCoroutine { cont ->
        if (GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(app) ==
            ConnectionResult.SUCCESS
        ) {
            SafetyNet.getClient(app).attest(nonce.toByteArray(), safetyNetApiKey)
                .addOnSuccessListener {
                    cont.resume(it.jwsResult)
                }
                .addOnFailureListener {
                    cont.resume(null)
                }
        } else {
            cont.resume(null)
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
        app = requireActivity().application as App

        if (selectedRegistrationMethod == RegistrationMethod.FSSO) {
            showProgressUpdateToast("Authenticating with FSSO")
            val activity = this.activity ?: return
            app.sudoUserClient.presentFederatedSignInUI(activity) { result ->
                when (result) {
                    is SignInResult.Success -> {
                        setUsedFssoFlag(true)
                        launch {
                            navigateToSudos()
                        }
                    }
                    is SignInResult.Failure -> {
                        showRegistrationFailure(result.error)
                    }
                }
            }
        } else if (selectedRegistrationMethod == RegistrationMethod.SAFETY_NET) {
            if (safetyNetApiKeyAvailable) {
                showProgressUpdateToast("Attempting registration with SafetyNet")
                @SuppressWarnings("HardwareIds")
                val vendorId = Settings.Secure.getString(
                    app.contentResolver,
                    Settings.Secure.ANDROID_ID
                )
                launch {
                    try {
                        withContext(Dispatchers.IO) {
                            val attestationResult = getAttestationResult(vendorId)

                            if (attestationResult != null) {
                                app.sudoUserClient.registerWithSafetyNetAttestation(
                                    attestationResult,
                                    vendorId,
                                    UUID.randomUUID().toString()
                                )
                            } else {
                                errorLogger.error("safetyNet attestation result was null.")
                                throw Exception("safetyNet attestation result was null.")
                            }
                        }
                        signIn()
                    } catch (e: Exception) {
                        showRegistrationFailure(e)
                        if (e.localizedMessage?.contains("UserValidationFailed") == true) {
                            showProgressUpdateToast("UserValidationFailed exception: This device may have used up its max allowed safetyNet registrations.")
                        }
                    }
                }
            } else {
                showProgressUpdateToast("Cannot use safetyNet, no API key present.")
                hideLoading()
                setItemsEnabled(true)
            }
        } else {
            // Default to trying via TEST registration keys
            if (app.sudoUserClient.isRegistered()) {
                // If already registered, sign in
                signIn()
            } else {
                loadRegistrationKeys()
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
                navigateToSudos()
            }
            return
        }
        launch {
            try {
                withContext(Dispatchers.IO) {
                    app.sudoUserClient.signInWithKey()
                }
                navigateToSudos()
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

        showProgressUpdateToast("Attempting registration with TEST registration keys")

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
            app.keyManagement.keyManager,
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

    /** Display a Toast with a progress update on the state of registration/sign in */
    private val showProgressUpdateToast = { msg: String ->
        Toast.makeText(
            requireContext(),
            msg,
            Toast.LENGTH_LONG
        ).show()
    }

    /** Navigate to sudos screen and redeem entitlements */
    private suspend fun navigateToSudos() {
        redeemEntitlements()
        navController.navigate(
            RegisterFragmentDirections.actionRegisterFragmentToSudosFragment()
        )
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
        binding.buttonSignOut.isEnabled = isEnabled
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
    }

    /** Sets the registration method */
    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        selectedRegistrationMethod = when (registrationMethodList[pos]) {
            getString(R.string.safety_net_reg) -> RegistrationMethod.SAFETY_NET
            getString(R.string.federated_signin) -> RegistrationMethod.FSSO
            else -> RegistrationMethod.TEST
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>) { /* no-op */
    }
}
