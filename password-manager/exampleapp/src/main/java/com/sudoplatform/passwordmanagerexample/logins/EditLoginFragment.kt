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
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.sudoplatform.passwordmanagerexample.App
import com.sudoplatform.passwordmanagerexample.MissingFragmentArgumentException
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.RETURN_ACTION_ARGUMENT
import com.sudoplatform.passwordmanagerexample.VAULT_ARGUMENT
import com.sudoplatform.passwordmanagerexample.VAULT_LOGIN_ARGUMENT
import com.sudoplatform.passwordmanagerexample.createLoadingAlertDialog
import com.sudoplatform.passwordmanagerexample.showAlertDialog
import com.sudoplatform.sudopasswordmanager.SudoPasswordManagerException
import com.sudoplatform.sudopasswordmanager.models.SecureFieldValue
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
 * - Links From: [LoginsFragment] when the user clicks the a row containing some login credentials.
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
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        with(requireArguments()) {
            vault = getParcelable(VAULT_ARGUMENT)
                ?: throw MissingFragmentArgumentException("Vault argument is missing")
            vaultLogin = getParcelable<VaultLogin>(VAULT_LOGIN_ARGUMENT)
                ?: throw MissingFragmentArgumentException("VaultLogin argument is missing")
        }

        loadFromVaultLogin(vaultLogin)

        button_passwordGenerator.setOnClickListener {
            val args = bundleOf(
                VAULT_ARGUMENT to vault,
                VAULT_LOGIN_ARGUMENT to toVaultLogin(),
                RETURN_ACTION_ARGUMENT to R.id.action_passwordGeneratorDialogFragment_to_editLoginFragment
            )
            navController.navigate(R.id.action_editLoginFragment_to_passwordGeneratorDialogFragment, args)
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
     * If a [TextView] has a non blank value then wrap it in a [SecureFieldValue] otherwise return null
     */
    private fun TextView.toSecureField(): SecureFieldValue? {
        val text = this.text.toString().trim()
        if (text.isNotEmpty()) {
            return SecureFieldValue(text)
        } else {
            return null
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
