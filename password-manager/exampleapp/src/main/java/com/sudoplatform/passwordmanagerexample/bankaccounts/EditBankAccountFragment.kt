/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.bankaccounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.sudoplatform.passwordmanagerexample.App
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.createLoadingAlertDialog
import com.sudoplatform.passwordmanagerexample.showAlertDialog
import com.sudoplatform.passwordmanagerexample.toSecureField
import com.sudoplatform.passwordmanagerexample.vaultItems.VaultItemsFragment
import com.sudoplatform.sudopasswordmanager.SudoPasswordManagerException
import com.sudoplatform.sudopasswordmanager.models.Vault
import com.sudoplatform.sudopasswordmanager.models.VaultBankAccount
import com.sudoplatform.sudopasswordmanager.models.VaultItemNote
import com.sudoplatform.sudopasswordmanager.models.VaultItemValue
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_create_edit_bank_account.editText_accountName
import kotlinx.android.synthetic.main.fragment_create_edit_bank_account.editText_accountNumber
import kotlinx.android.synthetic.main.fragment_create_edit_bank_account.editText_accountType
import kotlinx.android.synthetic.main.fragment_create_edit_bank_account.editText_bankName
import kotlinx.android.synthetic.main.fragment_create_edit_bank_account.editText_notes
import kotlinx.android.synthetic.main.fragment_create_edit_bank_account.editText_routingNumber
import kotlinx.android.synthetic.main.fragment_create_edit_bank_account.label_createdAt
import kotlinx.android.synthetic.main.fragment_create_edit_bank_account.label_updatedAt
import kotlinx.android.synthetic.main.fragment_create_edit_bank_account.view.toolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This [EditBankAccountFragment] presents a screen that accepts the values of a set of bank account credentials.
 *
 * - Links From: [VaultItemsFragment] when the user clicks the Create Vault Item button
 */
class EditBankAccountFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** The [Application] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: EditBankAccountFragmentArgs by navArgs()

    /** The [Vault] to which the [VaultBankAccount] will be added. */
    private lateinit var vault: Vault

    /** The [VaultBankAccount] to be shown and edited */
    private lateinit var vaultBankAccount: VaultBankAccount

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_create_edit_bank_account, container, false)
        val toolbar = (view.toolbar as Toolbar)
        toolbar.title = getString(R.string.edit_bank_account)
        toolbar.inflateMenu(R.menu.nav_menu_with_save_button)
        toolbar.setOnMenuItemClickListener {
            when (it?.itemId) {
                R.id.save -> {
                    saveBankAccount()
                }
            }
            true
        }
        app = requireActivity().application as App

        vault = args.vault
        vaultBankAccount = args.vaultBankAccount

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        loadFromVaultBankAccount(vaultBankAccount)
    }

    private fun saveBankAccount() {
        val name = editText_accountName.text.toString().trim()
        if (name.isEmpty()) {
            showAlertDialog(
                titleResId = R.string.enter_bank_account_name,
                positiveButtonResId = android.R.string.ok
            )
            return
        }

        val bankAccount = toVaultBankAccount()

        launch {
            try {
                showLoading(R.string.saving_bank_account)
                withContext(Dispatchers.IO) {
                    app.sudoPasswordManager.update(bankAccount, vault)
                    app.sudoPasswordManager.update(vault)
                }
                hideLoading()
                navController.popBackStack()
            } catch (e: SudoPasswordManagerException) {
                hideLoading()
                showAlertDialog(
                    titleResId = R.string.saving_bank_account_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { saveBankAccount() },
                    negativeButtonResId = android.R.string.cancel,
                    onNegative = { navController.popBackStack() }
                )
            }
        }
    }

    private fun toVaultBankAccount(): VaultBankAccount {
        val notes = editText_notes.toSecureField()?.let { VaultItemNote(it) }
        val accountNumber = editText_accountNumber.toSecureField()?.let { VaultItemValue(it) }

        return vaultBankAccount.copy(
            name = editText_accountName.text.toString().trim(),
            bankName = editText_bankName.text.toString().trim(),
            accountNumber = accountNumber,
            routingNumber = editText_routingNumber.text.toString().trim(),
            accountType = editText_accountType.text.toString().trim(),
            notes = notes
        )
    }

    private fun loadFromVaultBankAccount(vaultBankAccount: VaultBankAccount) {
        val dateFormat = SimpleDateFormat(getString(R.string.login_date_format), Locale.getDefault())
        label_createdAt.isVisible = true
        label_updatedAt.isVisible = true
        label_createdAt.setText(getString(R.string.created_at, dateFormat.format(vaultBankAccount.createdAt)))
        label_updatedAt.setText(getString(R.string.updated_at, dateFormat.format(vaultBankAccount.updatedAt)))
        editText_accountName.setText(vaultBankAccount.name)
        vaultBankAccount.bankName?.let { editText_bankName.setText(it) }
        vaultBankAccount.accountNumber?.let { editText_accountNumber.setText(it.getValue()) }
        vaultBankAccount.routingNumber?.let { editText_routingNumber.setText(it) }
        vaultBankAccount.accountType?.let { editText_accountType.setText(it) }
        vaultBankAccount.notes?.let { editText_notes.setText(it.getValue()) }
    }

    /**
     * Sets toolbar items and edit text field to enabled/disabled.
     *
     * @param isEnabled If true, toolbar items and edit text field will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        editText_accountName.isEnabled = isEnabled
        editText_bankName.isEnabled = isEnabled
        editText_accountNumber.isEnabled = isEnabled
        editText_routingNumber.isEnabled = isEnabled
        editText_accountType.isEnabled = isEnabled
        editText_notes.isEnabled = isEnabled
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
