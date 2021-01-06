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
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.sudoplatform.passwordmanagerexample.App
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.createLoadingAlertDialog
import com.sudoplatform.passwordmanagerexample.showAlertDialog
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_change_master_password.*
import kotlinx.android.synthetic.main.fragment_change_master_password.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        val view = inflater.inflate(R.layout.fragment_change_master_password, container, false)
        app = requireActivity().application as App
        val toolbar = (view.toolbar as Toolbar)
        toolbar.title = getString(R.string.change_master_password)
        toolbar.inflateMenu(R.menu.nav_menu_with_save_button)
        toolbar.setOnMenuItemClickListener {
            when (it?.itemId) {
                R.id.save -> {
                    changeMasterPassword()
                }
            }
            true
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
        view.currentPasswordText.requestFocus()
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        super.onDestroy()
    }

    private fun changeMasterPassword() {

        val currentPassword = currentPasswordText.text.toString().trim()
        val newPassword = newPasswordText.text.toString().trim()

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
        currentPasswordText.isEnabled = isEnabled
        newPasswordText.isEnabled = isEnabled
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
