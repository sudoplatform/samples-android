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
import android.widget.RadioButton
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.sudoplatform.passwordmanagerexample.App
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.createLoadingAlertDialog
import com.sudoplatform.passwordmanagerexample.creditcards.colorArray
import com.sudoplatform.passwordmanagerexample.databinding.FragmentCreateEditLoginBinding
import com.sudoplatform.passwordmanagerexample.passwordgenerator.PasswordGeneratorDialog
import com.sudoplatform.passwordmanagerexample.showAlertDialog
import com.sudoplatform.passwordmanagerexample.toSecureField
import com.sudoplatform.passwordmanagerexample.util.ObjectDelegate
import com.sudoplatform.sudopasswordmanager.SudoPasswordManagerException
import com.sudoplatform.sudopasswordmanager.models.Vault
import com.sudoplatform.sudopasswordmanager.models.VaultItemNote
import com.sudoplatform.sudopasswordmanager.models.VaultItemPassword
import com.sudoplatform.sudopasswordmanager.models.VaultLogin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.CoroutineContext

/**
 * This [EditLoginFragment] presents a screen that shows the values of login credentials
 * and allows the user to edit and save them.
 *
 * - Links From: [VaultItemsFragment] when the user clicks the a row containing some login credentials.
 * - Links To: [PasswordGeneratorDialog] when the user clicks the generate password button
 */
class EditLoginFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** View binding to the views defined in the layout */
    private val bindingDelegate = ObjectDelegate<FragmentCreateEditLoginBinding>()
    private val binding by bindingDelegate

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
        bindingDelegate.attach(FragmentCreateEditLoginBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.edit_login)
            inflateMenu(R.menu.nav_menu_with_save_button)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.save -> {
                        saveLogin()
                    }
                }
                true
            }
        }
        app = requireActivity().application as App

        vault = args.vault
        vaultLogin = args.vaultLogin

        // Handle the result coming back from the password generator fragment
        setFragmentResultListener(PasswordGeneratorDialog.GENERATED_PASSWORD) { resultKey, result ->
            if (resultKey == PasswordGeneratorDialog.GENERATED_PASSWORD) {
                result.getString(PasswordGeneratorDialog.GENERATED_PASSWORD)?.let {
                    binding.editTextPassword.setText(it)
                }
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        loadFromVaultLogin(vaultLogin)

        binding.buttonPasswordGenerator.setOnClickListener {
            navController.navigate(EditLoginFragmentDirections.actionEditLoginFragmentToPasswordGeneratorDialogFragment())
        }
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    private fun toVaultLogin(): VaultLogin {
        val notes = binding.editTextNotes.toSecureField()?.let { VaultItemNote(it) }
        val password = binding.editTextPassword.toSecureField()?.let { VaultItemPassword(it) }
        val hexColor =
            colorArray[
                Integer.parseInt(
                    view
                        ?.findViewById<RadioButton>(binding.colorRow.radioGroupColors.checkedRadioButtonId)
                        ?.tag as String
                )
            ]

        return vaultLogin.copy(
            name = binding.editTextLoginName.text.toString().trim(),
            user = binding.editTextUsername.text.toString().trim(),
            url = binding.editTextWebAddress.text.toString().trim(),
            notes = notes,
            password = password,
            hexColor = hexColor,
            favorite = binding.switchFavorite.addAsFavoriteSwitch.isChecked,
        )
    }

    private fun loadFromVaultLogin(vaultLogin: VaultLogin) {
        val dateFormat =
            SimpleDateFormat(getString(R.string.login_date_format), Locale.getDefault())
        binding.labelCreatedAt.isVisible = true
        binding.labelUpdatedAt.isVisible = true
        binding.labelCreatedAt.setText(
            getString(
                R.string.created_at,
                dateFormat.format(vaultLogin.createdAt)
            )
        )
        binding.labelUpdatedAt.setText(
            getString(
                R.string.updated_at,
                dateFormat.format(vaultLogin.updatedAt)
            )
        )
        binding.editTextLoginName.setText(vaultLogin.name)
        vaultLogin.url?.let { binding.editTextWebAddress.setText(it) }
        vaultLogin.user?.let { binding.editTextUsername.setText(it) }
        vaultLogin.password?.let { binding.editTextPassword.setText(it.getValue()) }
        vaultLogin.notes?.let { binding.editTextNotes.setText(it.getValue()) }
        view?.findViewWithTag<RadioButton>(
            colorArray.indexOf(vaultLogin.hexColor).toString()
        )?.let { binding.colorRow.radioGroupColors.check(it.id) }
        vaultLogin.favorite?.let { binding.switchFavorite.addAsFavoriteSwitch.isChecked = it }
    }

    private fun saveLogin() {

        val name = binding.editTextLoginName.text.toString().trim()
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
        binding.switchFavorite.addAsFavoriteSwitch.isEnabled = isEnabled
        binding.editTextUsername.isEnabled = isEnabled
        binding.editTextWebAddress.isEnabled = isEnabled
        binding.editTextLoginName.isEnabled = isEnabled
        binding.editTextNotes.isEnabled = isEnabled
        binding.editTextPassword.isEnabled = isEnabled
        binding.colorRow.radioGroupColors.isEnabled = isEnabled
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
