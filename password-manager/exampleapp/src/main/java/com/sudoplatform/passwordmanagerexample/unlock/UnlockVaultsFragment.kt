/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.unlock

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.sudoplatform.passwordmanagerexample.App
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.createLoadingAlertDialog
import com.sudoplatform.passwordmanagerexample.databinding.FragmentUnlockVaultsBinding
import com.sudoplatform.passwordmanagerexample.register.RegisterFragment
import com.sudoplatform.passwordmanagerexample.settings.renderRescueKitToFile
import com.sudoplatform.passwordmanagerexample.settings.saveSecretCodeToClipboard
import com.sudoplatform.passwordmanagerexample.settings.shareRescueKit
import com.sudoplatform.passwordmanagerexample.showAlertDialog
import com.sudoplatform.passwordmanagerexample.util.ObjectDelegate
import com.sudoplatform.sudopasswordmanager.PasswordManagerRegistrationStatus
import com.sudoplatform.sudopasswordmanager.SudoPasswordManagerException
import com.sudoplatform.sudouser.SudoUserClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * This [UnlockVaultsFragment] presents a screen that does one of the following:
 * - accepts the master password from the user to unlock the vaults
 * - accepts a new master password entered twice to set the master password
 * - accepts the master password and the secret code to unlock the vaults.
 *
 * Links From:
 *  - [RegisterFragment]: A user successfully registers or signs in to the app.
 *
 * Links To:
 *  - [SudosFragment]: Once the master password has been entered.
 *  - [RegisterFragment]: If the user presses the deregister button.
 */
class UnlockVaultsFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** View binding to the views defined in the layout */
    private val bindingDelegate = ObjectDelegate<FragmentUnlockVaultsBinding>()
    private val binding by bindingDelegate

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** The App that contains the [SudoUserClient] and [SudoPasswordManagerClient] */
    private lateinit var app: App

    /** The latest registration status, set after calling getRegistrationStatus() on the [SudoPasswordManagerClient] */
    private var latestRegistrationStatus: PasswordManagerRegistrationStatus? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bindingDelegate.attach(FragmentUnlockVaultsBinding.inflate(inflater, container, false))
        app = requireActivity().application as App
        return binding.root
    }

    private suspend fun getRegistrationStatus(): PasswordManagerRegistrationStatus? {
        try {
            showLoading(R.string.checking_registration_status)
            val status = withContext(Dispatchers.IO) {
                app.sudoPasswordManager.getRegistrationStatus()
            }
            hideLoading()
            latestRegistrationStatus = status
            return status
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            hideLoading()
            showAlertDialog(
                titleResId = R.string.unlock_vaults_title,
                messageResId = R.string.getregistrationstatus_failed,
                positiveButtonResId = android.R.string.ok,
                onPositive = { }
            )
        }
        return null
    }

    private fun setupMenu(toolbar: Toolbar) {
        toolbar.inflateMenu(R.menu.nav_menu_unlock_menu)

        val sharedPreferences = context?.getSharedPreferences("SignIn", Context.MODE_PRIVATE)
        val usedFSSO = sharedPreferences?.getBoolean("usedFSSO", false)

        if (usedFSSO == true) { toolbar.menu?.getItem(0)?.title = getString(R.string.sign_out) }

        // change the action item and hide deregister menu item based on registration status
        when (latestRegistrationStatus) {
            PasswordManagerRegistrationStatus.NOT_REGISTERED -> {
                toolbar.menu?.getItem(0)?.isVisible = false
                toolbar.menu?.getItem(1)?.title = getString(R.string.save)
            }
            PasswordManagerRegistrationStatus.REGISTERED -> {
                toolbar.menu?.getItem(0)?.isVisible = true
                toolbar.menu?.getItem(1)?.title = getString(R.string.unlock)
            }
            PasswordManagerRegistrationStatus.MISSING_SECRET_CODE -> {
                toolbar.menu?.getItem(0)?.isVisible = false
                toolbar.menu?.getItem(1)?.title = getString(R.string.unlock)
            }
        }

        toolbar.setOnMenuItemClickListener {
            when (it?.itemId) {
                R.id.deregister -> {
                    if (usedFSSO == true) {
                        launch {
                            app.doFSSOSSignout()
                        }
                    } else {
                        showAlertDialog(
                            titleResId = R.string.deregister,
                            messageResId = R.string.deregister_confirmation,
                            positiveButtonResId = R.string.deregister,
                            onPositive = { deregister() },
                            negativeButtonResId = android.R.string.cancel
                        )
                    }
                }
                R.id.save -> {
                    when (latestRegistrationStatus) {
                        PasswordManagerRegistrationStatus.NOT_REGISTERED -> {
                            register(EditorInfo.IME_ACTION_DONE)
                        }
                        PasswordManagerRegistrationStatus.REGISTERED -> {
                            unlockWithPassword(EditorInfo.IME_ACTION_DONE)
                        }
                        PasswordManagerRegistrationStatus.MISSING_SECRET_CODE -> {
                            unlockWithSecretCode(EditorInfo.IME_ACTION_DONE)
                        }
                    }
                }
            }
            true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
        setupMenu(binding.toolbar.root)

        launch {
            when (getRegistrationStatus()) {
                PasswordManagerRegistrationStatus.NOT_REGISTERED -> {
                    binding.toolbar.root.title = getString(R.string.master_password_title)
                    binding.bottomText.isVisible = true
                    binding.topText.hint = getString(R.string.enter_new_master_password_hint)
                    binding.bottomText.hint = getString(R.string.confirm_master_password_hint)
                    binding.bottomText.setOnEditorActionListener { _, actionId, _ ->
                        register(actionId)
                    }
                }
                PasswordManagerRegistrationStatus.REGISTERED -> {
                    binding.toolbar.root.title = getString(R.string.unlock_vaults_title)
                    binding.bottomText.isVisible = false
                    binding.topText.hint = getString(R.string.enter_master_password_hint)
                    binding.topText.setOnEditorActionListener { _, actionId, _ ->
                        unlockWithPassword(actionId)
                    }
                }
                PasswordManagerRegistrationStatus.MISSING_SECRET_CODE -> {
                    binding.toolbar.root.title = getString(R.string.unlock_vaults_title)
                    binding.bottomText.isVisible = true
                    binding.topText.hint = getString(R.string.enter_secret_code)
                    binding.bottomText.hint = getString(R.string.enter_master_password_hint)
                    binding.bottomText.setOnEditorActionListener { _, actionId, _ ->
                        unlockWithSecretCode(actionId)
                    }
                }
                else -> { /* Error getting status */ }
            }
            binding.topText.requestFocus()
        }
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    private fun register(actionId: Int): Boolean {
        if (actionId != EditorInfo.IME_ACTION_DONE) {
            return false
        }

        val password1 = binding.topText.text.toString().trim()
        val password2 = binding.bottomText.text.toString().trim()

        if (password1.isBlank() || password2.isBlank()) {
            showAlertDialog(
                titleResId = R.string.master_password_title,
                messageResId = R.string.enter_master_password_error,
                positiveButtonResId = android.R.string.ok,
                onPositive = {}
            )
            return true
        }
        if (password1 != password2) {
            showAlertDialog(
                titleResId = R.string.master_password_title,
                messageResId = R.string.master_password_mismatch_error,
                positiveButtonResId = android.R.string.ok,
                onPositive = {}
            )
            return true
        }

        launch {
            try {
                showLoading(R.string.registering)
                withContext(Dispatchers.IO) {
                    app.sudoPasswordManager.register(password1)
                    app.sudoPasswordManager.unlock(password1)
                }
                hideLoading()
                promptToSaveSecretCode()
            } catch (error: Exception) {
                app.logger.error("Failed to register: $error")
                hideLoading()
                showAlertDialog(
                    titleResId = R.string.master_password_title,
                    message = getString(R.string.register_failure, error.localizedMessage),
                    negativeButtonResId = android.R.string.ok,
                    onNegative = { }
                )
            }
        }
        return true
    }

    private fun navigateToSudosFragment() {
        navController.navigate(UnlockVaultsFragmentDirections.actionUnlockVaultsFragmentToSudosFragment())
    }

    private fun unlockWithPassword(actionId: Int): Boolean {
        if (actionId != EditorInfo.IME_ACTION_DONE) {
            return false
        }

        val password = binding.topText.text.toString().trim()

        if (password.isBlank()) {
            showAlertDialog(
                titleResId = R.string.master_password_title,
                messageResId = R.string.enter_master_password_error,
                positiveButtonResId = android.R.string.ok,
                onPositive = { binding.topText.requestFocus() }
            )
            return true
        }

        unlock(password, null)

        return true
    }

    private fun unlockWithSecretCode(actionId: Int): Boolean {
        if (actionId != EditorInfo.IME_ACTION_DONE) {
            return false
        }

        val secretCode = binding.topText.text.toString().trim()
        val password = binding.bottomText.text.toString().trim()

        if (password.isBlank() || secretCode.isBlank()) {
            showAlertDialog(
                titleResId = R.string.unlock_vaults_title,
                messageResId = R.string.enter_password_and_code_error,
                positiveButtonResId = android.R.string.ok,
                onPositive = {
                    if (password.isBlank()) {
                        binding.bottomText.requestFocus()
                    } else {
                        binding.topText.requestFocus()
                    }
                }
            )
            return true
        }

        unlock(password, secretCode)

        return true
    }

    private fun unlock(password: String, secretCode: String?) {
        launch {
            try {
                showLoading(R.string.unlocking)
                withContext(Dispatchers.IO) {
                    app.sudoPasswordManager.unlock(password, secretCode)
                }
                hideLoading()
                navigateToSudosFragment()
            } catch (error: Exception) {
                error.printStackTrace()
                app.logger.error("Failed to unlock the vaults: $error")
                hideLoading()
                val messageId = when (error) {
                    is SudoPasswordManagerException.InvalidPasswordOrMissingSecretCodeException -> R.string.wrong_password_or_secret
                    else -> R.string.unlock_failure
                }
                showAlertDialog(
                    titleResId = R.string.unlock_vaults_title,
                    messageResId = messageId,
                    positiveButtonResId = android.R.string.ok,
                    onPositive = {
                        binding.topText.requestFocus()
                        binding.topText.selectAll()
                    }
                )
            }
        }
    }

    /** Perform de-registration from the [SudoUserClient] and clear all local data. */
    private fun deregister() {
        launch {
            try {
                showLoading(R.string.deregistering)
                withContext(Dispatchers.IO) {
                    app.sudoUserClient.deregister()
                    app.sudoPasswordManager.reset()
                }
                hideLoading()
                navController.navigate(UnlockVaultsFragmentDirections.actionUnlockVaultsFragmentToRegisterFragment())
            } catch (error: Exception) {
                app.logger.error("Failed to deregister: $error")
                Toast.makeText(
                    requireContext(),
                    getString(R.string.deregister_failure, error.localizedMessage),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun promptToSaveSecretCode() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.save_secret_code_title)
            .setMessage(R.string.save_secret_code_message)
            .setPositiveButton(R.string.copy_to_clipboard) { _, _ ->
                copySecretCodeToClipboard()
            }
            .setNegativeButton(R.string.download_rescue_kit) { _, _ ->
                downloadRescueKit()
            }
            .setNeutralButton(R.string.not_now) { _, _ -> }
            .setTitle(R.string.save_secret_code_title)
            .setMessage(R.string.save_secret_code_message)
            .setOnDismissListener {
                navigateToSudosFragment()
            }
            .show()
    }

    private fun copySecretCodeToClipboard() {
        val context = requireContext()
        saveSecretCodeToClipboard(
            app.sudoPasswordManager.getSecretCode(),
            context
        )
        Toast.makeText(
            context,
            getString(R.string.secret_code_copied),
            Toast.LENGTH_LONG
        ).show()
    }

    private fun downloadRescueKit() {
        launch {
            try {
                val rescueKitFile = renderRescueKitToFile(requireContext(), app.sudoPasswordManager)
                shareRescueKit(requireContext(), rescueKitFile)
            } catch (e: Exception) {
                app.logger.outputError(Error(e))
                Toast.makeText(
                    requireContext(),
                    getString(R.string.save_pdf_failure, e.localizedMessage),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Sets menu buttons and toolbar items to enabled/disabled.
     *
     * @param isEnabled If true, buttons and toolbar items will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        binding.toolbar.root.menu.getItem(0)?.isEnabled = isEnabled
        binding.topText.isEnabled = isEnabled
        binding.bottomText.isEnabled = isEnabled
    }

    /** Displays the loading [AlertDialog] indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int) {
        loading = createLoadingAlertDialog(textResId)
        loading?.show()
        setItemsEnabled(false)
    }

    /** Dismisses the loading [AlertDialog] indicating that an operation has finished. */
    private fun hideLoading() {
        loading?.dismiss()
        if (bindingDelegate.isAttached()) {
            setItemsEnabled(true)
        }
    }
}
