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
import com.sudoplatform.passwordmanagerexample.passwordgenerator.PasswordGeneratorDialog.Companion.GENERATED_PASSWORD
import com.sudoplatform.passwordmanagerexample.showAlertDialog
import com.sudoplatform.passwordmanagerexample.toSecureField
import com.sudoplatform.sudopasswordmanager.SudoPasswordManagerException
import com.sudoplatform.sudopasswordmanager.models.Vault
import com.sudoplatform.sudopasswordmanager.models.VaultItemNote
import com.sudoplatform.sudopasswordmanager.models.VaultItemPassword
import com.sudoplatform.sudopasswordmanager.models.VaultLogin
import java.util.UUID
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
 * This [CreateLoginFragment] presents a screen that accepts the values of a set of login credentials.
 *
 * - Links From: [VaultItemsFragment] when the user clicks the Create Login button
 * - Links To: [PasswordGeneratorDialog] when the user clicks the generate password button
 */
class CreateLoginFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** The [Application] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: CreateLoginFragmentArgs by navArgs()

    /** The [Vault] to which the [VaultLogin] will be added. */
    private lateinit var vault: Vault

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_edit_login, container, false)
        val toolbar = (view.toolbar as Toolbar)
        toolbar.title = getString(R.string.create_login)
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

        // Handle the result coming back from the password generator fragment
        setFragmentResultListener(GENERATED_PASSWORD) { resultKey, result ->
            if (resultKey == GENERATED_PASSWORD) {
                result.getString(GENERATED_PASSWORD)?.let {
                    editText_password.setText(it)
                }
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        button_passwordGenerator.setOnClickListener {
            navController.navigate(CreateLoginFragmentDirections.actionCreateLoginFragmentToPasswordGeneratorDialogFragment())
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
        return VaultLogin(
            id = UUID.randomUUID().toString(),
            name = editText_loginName.text.toString().trim(),
            user = editText_username.text.toString().trim(),
            url = editText_webAddress.text.toString().trim(),
            notes = notes,
            password = password
        )
    }

    private fun fromVaultLogin(vaultLogin: VaultLogin) {
        label_createdAt.isVisible = false
        label_updatedAt.isVisible = false
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
                showLoading(R.string.creating_login)
                withContext(Dispatchers.IO) {
                    app.sudoPasswordManager.add(login, vault)
                }
                hideLoading()
                navController.popBackStack()
            } catch (e: SudoPasswordManagerException) {
                hideLoading()
                showAlertDialog(
                    titleResId = R.string.create_login_failure,
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
