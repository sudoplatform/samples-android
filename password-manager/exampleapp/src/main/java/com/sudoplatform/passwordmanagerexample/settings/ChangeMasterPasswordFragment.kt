/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.sudoplatform.passwordmanagerexample.App
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.createLoadingAlertDialog
import com.sudoplatform.passwordmanagerexample.databinding.FragmentChangeMasterPasswordBinding
import com.sudoplatform.passwordmanagerexample.showAlertDialog
import com.sudoplatform.passwordmanagerexample.util.ObjectDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * This [ChangeMasterPasswordFragment] presents a screen that prompts for the
 * current master password and the new master password. When the user hits the
 * save button the master password is changed.
 *
 * Links From:
 *  - [SettingsFragment]: Change password is chosen from the settings screen.
 */
class ChangeMasterPasswordFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** View binding to the views defined in the layout */
    private val bindingDelegate = ObjectDelegate<FragmentChangeMasterPasswordBinding>()
    private val binding by bindingDelegate

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** The App that contains the [SudoPasswordManagerClient] */
    private lateinit var app: App

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bindingDelegate.attach(FragmentChangeMasterPasswordBinding.inflate(inflater, container, false))
        app = requireActivity().application as App
        with(binding.toolbar.root) {
            title = getString(R.string.change_master_password)
            inflateMenu(R.menu.nav_menu_with_save_button)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.save -> {
                        changeMasterPassword()
                    }
                }
                true
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
        binding.currentPasswordText.requestFocus()
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    private fun changeMasterPassword() {

        val currentPassword = binding.currentPasswordText.text.toString().trim()
        val newPassword = binding.newPasswordText.text.toString().trim()

        if (currentPassword.isBlank()) {
            showAlertDialog(
                titleResId = R.string.change_master_password,
                messageResId = R.string.enter_current_password_error,
                positiveButtonResId = android.R.string.ok,
                onPositive = {}
            )
            return
        }
        if (newPassword.isBlank()) {
            showAlertDialog(
                titleResId = R.string.change_master_password,
                messageResId = R.string.enter_new_password_error,
                positiveButtonResId = android.R.string.ok,
                onPositive = {}
            )
            return
        }
        if (currentPassword == newPassword) {
            showAlertDialog(
                titleResId = R.string.change_master_password,
                messageResId = R.string.master_password_same_error,
                positiveButtonResId = android.R.string.ok,
                onPositive = {}
            )
            return
        }

        launch {
            try {
                showLoading(R.string.changing_password)
                withContext(Dispatchers.IO) {
                    app.sudoPasswordManager.changeMasterPassword(currentPassword, newPassword)
                }
                hideLoading()
                showAlertDialog(
                    titleResId = R.string.success,
                    positiveButtonResId = android.R.string.ok,
                    onPositive = { navController.popBackStack() }
                )
            } catch (error: Exception) {
                app.logger.error("Failed to change master password: $error")
                hideLoading()
                showAlertDialog(
                    titleResId = R.string.change_master_password,
                    message = getString(R.string.change_password_failure, error.localizedMessage),
                    negativeButtonResId = android.R.string.ok,
                    onNegative = { }
                )
            }
        }
    }

    /**
     * Sets menu buttons and toolbar items to enabled/disabled.
     *
     * @param isEnabled If true, buttons and toolbar items will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        binding.currentPasswordText.isEnabled = isEnabled
        binding.newPasswordText.isEnabled = isEnabled
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
