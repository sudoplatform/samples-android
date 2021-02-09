/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.unlock

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
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
import com.sudoplatform.passwordmanagerexample.register.RegisterFragment
import com.sudoplatform.passwordmanagerexample.settings.renderRescueKitToFile
import com.sudoplatform.passwordmanagerexample.settings.saveSecretCodeToClipboard
import com.sudoplatform.passwordmanagerexample.settings.shareRescueKit
import com.sudoplatform.passwordmanagerexample.showAlertDialog
import com.sudoplatform.sudopasswordmanager.PasswordManagerRegistrationStatus
import com.sudoplatform.sudopasswordmanager.SudoPasswordManagerException
import com.sudoplatform.sudouser.SudoUserClient
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_unlock_vaults.bottomText
import kotlinx.android.synthetic.main.fragment_unlock_vaults.topText
import kotlinx.android.synthetic.main.fragment_unlock_vaults.view.bottomText
import kotlinx.android.synthetic.main.fragment_unlock_vaults.view.toolbar
import kotlinx.android.synthetic.main.fragment_unlock_vaults.view.topText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** Toolbar [Menu] displaying title and toolbar items. */
    private var toolbarMenu: Menu? = null

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
        val view = inflater.inflate(R.layout.fragment_unlock_vaults, container, false)
        app = requireActivity().application as App
        return view
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
        toolbarMenu = toolbar.menu
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        val toolbar = (view.toolbar as Toolbar)

        launch {
            when (getRegistrationStatus()) {
                PasswordManagerRegistrationStatus.NOT_REGISTERED -> {
                    toolbar.title = getString(R.string.master_password_title)
                    view.bottomText.isVisible = true
                    view.topText.hint = getString(R.string.enter_new_master_password_hint)
                    view.bottomText.hint = getString(R.string.confirm_master_password_hint)
                    view.bottomText.setOnEditorActionListener { _, actionId, _ ->
                        register(actionId)
                    }
                }
                PasswordManagerRegistrationStatus.REGISTERED -> {
                    toolbar.title = getString(R.string.unlock_vaults_title)
                    view.bottomText.isVisible = false
                    view.topText.hint = getString(R.string.enter_master_password_hint)
                    view.topText.setOnEditorActionListener { _, actionId, _ ->
                        unlockWithPassword(actionId)
                    }
                }
                PasswordManagerRegistrationStatus.MISSING_SECRET_CODE -> {
                    toolbar.title = getString(R.string.unlock_vaults_title)
                    view.bottomText.isVisible = true
                    view.topText.hint = getString(R.string.enter_secret_code)
                    view.bottomText.hint = getString(R.string.enter_master_password_hint)
                    view.bottomText.setOnEditorActionListener { _, actionId, _ ->
                        unlockWithSecretCode(actionId)
                    }
                }
                else -> { /* Error getting status */ }
            }
            setupMenu(toolbar)
            view.topText.requestFocus()
        }
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        super.onDestroy()
    }

    private fun register(actionId: Int): Boolean {
        if (actionId != EditorInfo.IME_ACTION_DONE) {
            return false
        }

        val password1 = topText.text.toString().trim()
        val password2 = bottomText.text.toString().trim()

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

        val password = topText.text.toString().trim()

        if (password.isBlank()) {
            showAlertDialog(
                titleResId = R.string.master_password_title,
                messageResId = R.string.enter_master_password_error,
                positiveButtonResId = android.R.string.ok,
                onPositive = { topText.requestFocus() }
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

        val secretCode = topText.text.toString().trim()
        val password = bottomText.text.toString().trim()

        if (password.isBlank() || secretCode.isBlank()) {
            showAlertDialog(
                titleResId = R.string.unlock_vaults_title,
                messageResId = R.string.enter_password_and_code_error,
                positiveButtonResId = android.R.string.ok,
                onPositive = {
                    if (password.isBlank()) {
                        bottomText.requestFocus()
                    } else {
                        topText.requestFocus()
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
                        topText.requestFocus()
                        topText.selectAll()
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
                navigateToSudosFragment()
            }
            .setNegativeButton(R.string.download_rescue_kit) { _, _ ->
                downloadRescueKit()
                navigateToSudosFragment()
            }
            .setNeutralButton(R.string.not_now) { _, _ ->
                navigateToSudosFragment()
            }
            .setTitle(R.string.save_secret_code_title)
            .setMessage(R.string.save_secret_code_message)
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
        toolbarMenu?.getItem(0)?.isEnabled = isEnabled
        topText.isEnabled = isEnabled
        bottomText.isEnabled = isEnabled
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
        setItemsEnabled(true)
    }
}
