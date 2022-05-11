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
import android.widget.RadioButton
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.sudoplatform.passwordmanagerexample.App
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.createLoadingAlertDialog
import com.sudoplatform.passwordmanagerexample.creditcards.colorArray
import com.sudoplatform.passwordmanagerexample.databinding.FragmentCreateEditBankAccountBinding
import com.sudoplatform.passwordmanagerexample.showAlertDialog
import com.sudoplatform.passwordmanagerexample.toSecureField
import com.sudoplatform.passwordmanagerexample.util.ObjectDelegate
import com.sudoplatform.passwordmanagerexample.vaultItems.VaultItemsFragment
import com.sudoplatform.sudopasswordmanager.SudoPasswordManagerException
import com.sudoplatform.sudopasswordmanager.models.Vault
import com.sudoplatform.sudopasswordmanager.models.VaultBankAccount
import com.sudoplatform.sudopasswordmanager.models.VaultItemNote
import com.sudoplatform.sudopasswordmanager.models.VaultItemValue
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
 * This [EditBankAccountFragment] presents a screen that accepts the values of a set of bank account credentials.
 *
 * - Links From: [VaultItemsFragment] when the user clicks the Create Vault Item button
 */
class EditBankAccountFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** View binding to the views defined in the layout */
    private val bindingDelegate = ObjectDelegate<FragmentCreateEditBankAccountBinding>()
    private val binding by bindingDelegate

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
        bindingDelegate.attach(
            FragmentCreateEditBankAccountBinding.inflate(
                inflater,
                container,
                false
            )
        )
        with(binding.toolbar.root) {
            title = getString(R.string.edit_bank_account)
            inflateMenu(R.menu.nav_menu_with_save_button)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.save -> {
                        saveBankAccount()
                    }
                }
                true
            }
        }
        app = requireActivity().application as App

        vault = args.vault
        vaultBankAccount = args.vaultBankAccount

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        loadFromVaultBankAccount(vaultBankAccount)
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    private fun saveBankAccount() {
        val name = binding.editTextAccountName.text.toString().trim()
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
        val notes = binding.editTextNotes.toSecureField()?.let { VaultItemNote(it) }
        val accountNumber =
            binding.editTextAccountNumber.toSecureField()?.let { VaultItemValue(it) }
        val hexColor =
            colorArray[
                Integer.parseInt(
                    view
                        ?.findViewById<RadioButton>(binding.colorRow.radioGroupColors.checkedRadioButtonId)
                        ?.tag as String
                )
            ]

        return vaultBankAccount.copy(
            name = binding.editTextAccountName.text.toString().trim(),
            bankName = binding.editTextBankName.text.toString().trim(),
            accountNumber = accountNumber,
            routingNumber = binding.editTextRoutingNumber.text.toString().trim(),
            accountType = binding.editTextAccountType.text.toString().trim(),
            notes = notes,
            hexColor = hexColor,
            favorite = binding.switchFavorite.addAsFavoriteSwitch.isChecked,
        )
    }

    private fun loadFromVaultBankAccount(vaultBankAccount: VaultBankAccount) {
        val dateFormat =
            SimpleDateFormat(getString(R.string.login_date_format), Locale.getDefault())
        binding.labelCreatedAt.isVisible = true
        binding.labelUpdatedAt.isVisible = true
        binding.labelCreatedAt.setText(
            getString(
                R.string.created_at,
                dateFormat.format(vaultBankAccount.createdAt)
            )
        )
        binding.labelUpdatedAt.setText(
            getString(
                R.string.updated_at,
                dateFormat.format(vaultBankAccount.updatedAt)
            )
        )
        binding.editTextAccountName.setText(vaultBankAccount.name)
        vaultBankAccount.bankName?.let { binding.editTextBankName.setText(it) }
        vaultBankAccount.accountNumber?.let { binding.editTextAccountNumber.setText(it.getValue()) }
        vaultBankAccount.routingNumber?.let { binding.editTextRoutingNumber.setText(it) }
        vaultBankAccount.accountType?.let { binding.editTextAccountType.setText(it) }
        vaultBankAccount.notes?.let { binding.editTextNotes.setText(it.getValue()) }
        view?.findViewWithTag<RadioButton>(
            colorArray.indexOf(vaultBankAccount.hexColor).toString()
        )?.let { binding.colorRow.radioGroupColors.check(it.id) }
        vaultBankAccount.favorite?.let { binding.switchFavorite.addAsFavoriteSwitch.isChecked = it }
    }

    /**
     * Sets toolbar items and edit text field to enabled/disabled.
     *
     * @param isEnabled If true, toolbar items and edit text field will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        binding.switchFavorite.addAsFavoriteSwitch.isEnabled = isEnabled
        binding.editTextAccountName.isEnabled = isEnabled
        binding.editTextBankName.isEnabled = isEnabled
        binding.editTextAccountNumber.isEnabled = isEnabled
        binding.editTextRoutingNumber.isEnabled = isEnabled
        binding.editTextAccountType.isEnabled = isEnabled
        binding.editTextNotes.isEnabled = isEnabled
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
