/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.logins

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.sudoplatform.passwordmanagerexample.App
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.createLoadingAlertDialog
import com.sudoplatform.passwordmanagerexample.passwordgenerator.PasswordGeneratorDialog
import com.sudoplatform.passwordmanagerexample.showAlertDialog
import com.sudoplatform.passwordmanagerexample.toSecureField
import com.sudoplatform.sudopasswordmanager.SudoPasswordManagerException
import com.sudoplatform.sudopasswordmanager.models.Vault
import com.sudoplatform.sudopasswordmanager.models.VaultItemNote
import com.sudoplatform.sudopasswordmanager.models.VaultItemPassword
import com.sudoplatform.sudopasswordmanager.models.VaultLogin
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_create_edit_login.*
import kotlinx.android.synthetic.main.fragment_create_edit_login.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This [EditLoginFragment] presents a screen that shows the values of login credentials
 * and allows the user to edit and save them.
 *
 * - Links From: [VaultItemsFragment] when the user clicks the a row containing some login credentials.
 * - Links To: [PasswordGeneratorDialog] when the user clicks the generate password button
 */
class EditLoginFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** The [Application] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: EditLoginFragmentArgs by navArgs()

    /** The [Vault] containing the [VaultLogin]. */
    private lateinit var vault: Vault

    /** The [VaultLogin] to be shown and edited. */
    private lateinit var vaultLogin: VaultLogin

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_edit_login, container, false)
        val toolbar = (view.toolbar as Toolbar)
        toolbar.title = getString(R.string.edit_login)
        toolbar.inflateMenu(R.menu.nav_menu_with_save_button)
        toolbar.setOnMenuItemClickListener {
            when (it?.itemId) {
                R.id.save -> {
                    saveLogin()
                }
            }
            true
        }
        app = requireActivity().application as App

        vault = args.vault
        vaultLogin = args.vaultLogin

        // Handle the result coming back from the password generator fragment
        setFragmentResultListener(PasswordGeneratorDialog.GENERATED_PASSWORD) { resultKey, result ->
            if (resultKey == PasswordGeneratorDialog.GENERATED_PASSWORD) {
                result.getString(PasswordGeneratorDialog.GENERATED_PASSWORD)?.let {
                    editText_password.setText(it)
                }
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        loadFromVaultLogin(vaultLogin)

        button_passwordGenerator.setOnClickListener {
            navController.navigate(EditLoginFragmentDirections.actionEditLoginFragmentToPasswordGeneratorDialogFragment())
        }
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        super.onDestroy()
    }

    private fun toVaultLogin(): VaultLogin {
        val notes = editText_notes.toSecureField()?.let { VaultItemNote(it) }
        val password = editText_password.toSecureField()?.let { VaultItemPassword(it) }
        return vaultLogin.copy(
            name = editText_loginName.text.toString().trim(),
            user = editText_username.text.toString().trim(),
            url = editText_webAddress.text.toString().trim(),
            notes = notes,
            password = password
        )
    }

    private fun loadFromVaultLogin(vaultLogin: VaultLogin) {
        val dateFormat = SimpleDateFormat(getString(R.string.login_date_format), Locale.getDefault())
        label_createdAt.isVisible = true
        label_updatedAt.isVisible = true
        label_createdAt.setText(getString(R.string.created_at, dateFormat.format(vaultLogin.createdAt)))
        label_updatedAt.setText(getString(R.string.updated_at, dateFormat.format(vaultLogin.updatedAt)))
        editText_loginName.setText(vaultLogin.name)
        vaultLogin.url?.let { editText_webAddress.setText(it) }
        vaultLogin.user?.let { editText_username.setText(it) }
        vaultLogin.password?.let { editText_password.setText(it.getValue()) }
        vaultLogin.notes?.let { editText_notes.setText(it.getValue()) }
    }

    private fun saveLogin() {

        val name = editText_loginName.text.toString().trim()
        if (name.isEmpty()) {
            showAlertDialog(
                titleResId = R.string.enter_login_name,
                positiveButtonResId = android.R.string.ok
            )
            return
        }

        val login = toVaultLogin()

        launch {
            try {
                showLoading(R.string.saving_login)
                withContext(Dispatchers.IO) {
                    app.sudoPasswordManager.update(login, vault)
                    app.sudoPasswordManager.update(vault)
                }
                hideLoading()
                navController.popBackStack()
            } catch (e: SudoPasswordManagerException) {
                hideLoading()
                showAlertDialog(
                    titleResId = R.string.save_login_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { saveLogin() },
                    negativeButtonResId = android.R.string.cancel,
                    onNegative = { navController.popBackStack() }
                )
            }
        }
    }

    /**
     * Sets toolbar items and edit text field to enabled/disabled.
     *
     * @param isEnabled If true, toolbar items and edit text field will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        editText_username.isEnabled = isEnabled
        editText_webAddress.isEnabled = isEnabled
        editText_loginName.isEnabled = isEnabled
        editText_notes.isEnabled = isEnabled
        editText_password.isEnabled = isEnabled
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
