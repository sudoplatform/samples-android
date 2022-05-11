/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.creditcards

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.sudoplatform.passwordmanagerexample.databinding.FragmentCreateEditCreditCardBinding
import com.sudoplatform.passwordmanagerexample.showAlertDialog
import com.sudoplatform.passwordmanagerexample.toSecureField
import com.sudoplatform.passwordmanagerexample.util.ObjectDelegate
import com.sudoplatform.passwordmanagerexample.vaultItems.VaultItemsFragment
import com.sudoplatform.sudopasswordmanager.SudoPasswordManagerException
import com.sudoplatform.sudopasswordmanager.models.Vault
import com.sudoplatform.sudopasswordmanager.models.VaultCreditCard
import com.sudoplatform.sudopasswordmanager.models.VaultItemNote
import com.sudoplatform.sudopasswordmanager.models.VaultItemValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.CoroutineContext

/**
 * This [EditCreditCardFragment] presents a screen that accepts the values of a set of credit card credentials.
 *
 * - Links From: [VaultItemsFragment] when the user clicks the Create Vault Item button
 */
class EditCreditCardFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** View binding to the views defined in the layout */
    private val bindingDelegate = ObjectDelegate<FragmentCreateEditCreditCardBinding>()
    private val binding by bindingDelegate

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** The [Application] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: EditCreditCardFragmentArgs by navArgs()

    /** The [Vault] to which the [VaultCreditCard] will be added. */
    private lateinit var vault: Vault

    /** The [VaultCreditCard] to be shown and edited */
    private lateinit var vaultCreditCard: VaultCreditCard

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        bindingDelegate.attach(
            FragmentCreateEditCreditCardBinding.inflate(
                inflater,
                container,
                false
            )
        )
        with(binding.toolbar.root) {
            title = getString(R.string.edit_credit_card)
            inflateMenu(R.menu.nav_menu_with_save_button)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.save -> {
                        saveCreditCard()
                    }
                }
                true
            }
        }
        app = requireActivity().application as App
        vault = args.vault
        vaultCreditCard = args.vaultCreditCard

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        loadFromVaultCreditCard(vaultCreditCard)

        handleExpiryFormatting()
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    private fun saveCreditCard() {
        val name = binding.editTextCardName.text.toString().trim()
        if (name.isEmpty()) {
            showAlertDialog(
                titleResId = R.string.enter_card_name,
                positiveButtonResId = android.R.string.ok
            )
            return
        }

        val creditCard = toVaultCreditCard()

        launch {
            try {
                showLoading(R.string.saving_credit_card)
                withContext(Dispatchers.IO) {
                    app.sudoPasswordManager.update(creditCard, vault)
                    app.sudoPasswordManager.update(vault)
                }
                hideLoading()
                navController.popBackStack()
            } catch (e: SudoPasswordManagerException) {
                hideLoading()
                showAlertDialog(
                    titleResId = R.string.saving_credit_card_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { saveCreditCard() },
                    negativeButtonResId = android.R.string.cancel,
                    onNegative = { navController.popBackStack() }
                )
            }
        }
    }

    private fun toVaultCreditCard(): VaultCreditCard {
        val cardNumber = binding.editTextCardNumber.toSecureField()?.let { VaultItemValue(it) }
        val securityCode = binding.editTextSecurityCode.toSecureField()?.let { VaultItemValue(it) }
        val notes = binding.editTextNotes.toSecureField()?.let { VaultItemNote(it) }
        val expiryString = binding.editTextExpiry.text.toString().trim()
        val hexColor =
            colorArray[
                Integer.parseInt(
                    view
                        ?.findViewById<RadioButton>(binding.colorRow.radioGroupColors.checkedRadioButtonId)
                        ?.tag as String
                )
            ]

        // default to null if date parsing fails
        var expiryDate: Date? = null
        try {
            val dateFormat = SimpleDateFormat("MM/yy", Locale.getDefault())
            expiryDate = dateFormat.parse(expiryString)
        } catch (e: Exception) {
            // failed to parse expiry date string
        }
        return vaultCreditCard.copy(
            name = binding.editTextCardName.text.toString().trim(),
            cardName = binding.editTextCardHolder.text.toString().trim(),
            cardType = binding.editTextCardType.text.toString().trim(),
            cardNumber = cardNumber,
            expiresAt = expiryDate,
            securityCode = securityCode,
            notes = notes,
            hexColor = hexColor,
            favorite = binding.switchFavorite.addAsFavoriteSwitch.isChecked,
        )
    }

    private fun loadFromVaultCreditCard(vaultCard: VaultCreditCard) {
        val dateFormat =
            SimpleDateFormat(getString(R.string.login_date_format), Locale.getDefault())
        binding.labelCreatedAt.isVisible = true
        binding.labelUpdatedAt.isVisible = true
        binding.labelCreatedAt.text = getString(
            R.string.created_at,
            dateFormat.format(vaultCard.createdAt)
        )
        binding.labelUpdatedAt.text = getString(
            R.string.updated_at,
            dateFormat.format(vaultCard.updatedAt)
        )

        binding.editTextCardName.setText(vaultCard.name)
        binding.editTextCardHolder.setText(vaultCard.cardName)
        vaultCard.cardType?.let { binding.editTextCardType.setText(it) }
        vaultCard.cardNumber?.let { binding.editTextCardNumber.setText(it.getValue()) }
        val expiryFormat = SimpleDateFormat("MM/yy", Locale.getDefault())
        vaultCard.expiresAt?.let { binding.editTextExpiry.setText(expiryFormat.format(it)) }
        vaultCard.securityCode?.let { binding.editTextSecurityCode.setText(it.getValue()) }
        vaultCard.notes?.let { binding.editTextNotes.setText(it.getValue()) }
        view?.findViewWithTag<RadioButton>(
            colorArray.indexOf(vaultCard.hexColor).toString()
        )?.let { binding.colorRow.radioGroupColors.check(it.id) }
        vaultCard.favorite?.let { binding.switchFavorite.addAsFavoriteSwitch.isChecked = it }
    }

    private fun handleExpiryFormatting() {
        // format the text to be a date string
        binding.editTextExpiry.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            var ignoreTextChange = false

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // don't do anything if ignoring text change
                if (ignoreTextChange) {
                    ignoreTextChange = false
                    return
                }
                val stringBuilder = StringBuilder("")
                // keep string length to MM/YY format
                stringBuilder.append(
                    if (s!!.length > 5) {
                        s.subSequence(0, 5)
                    } else {
                        s
                    }
                )

                if (stringBuilder.lastIndex == 2) {
                    if (stringBuilder[2] != '/') {
                        stringBuilder.insert(2, "/")
                    }
                }

                // set ignore to true so that onTextChanged doesn't get stuck in a recursive loop
                ignoreTextChange = true
                binding.editTextExpiry.setText(stringBuilder.toString())
                binding.editTextExpiry.setSelection(stringBuilder.length)
            }
        })
    }

    /**
     * Sets toolbar items and edit text field to enabled/disabled.
     *
     * @param isEnabled If true, toolbar items and edit text field will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        binding.switchFavorite.addAsFavoriteSwitch.isEnabled = isEnabled
        binding.editTextCardName.isEnabled = isEnabled
        binding.editTextCardHolder.isEnabled = isEnabled
        binding.editTextCardType.isEnabled = isEnabled
        binding.editTextCardNumber.isEnabled = isEnabled
        binding.editTextExpiry.isEnabled = isEnabled
        binding.editTextSecurityCode.isEnabled = isEnabled
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
