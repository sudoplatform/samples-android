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
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
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
import com.sudoplatform.sudopasswordmanager.models.VaultCreditCard
import com.sudoplatform.sudopasswordmanager.models.VaultItemNote
import com.sudoplatform.sudopasswordmanager.models.VaultItemValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_create_edit_credit_card.*
import kotlinx.android.synthetic.main.fragment_create_edit_credit_card.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This [CreateCreditCardFragment] presents a screen that accepts the values of a set of credit card credentials.
 *
 * - Links From: [VaultItemsFragment] when the user clicks the Create Vault Item button
 */
class CreateCreditCardFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** The [Application] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: CreateCreditCardFragmentArgs by navArgs()

    /** The [Vault] to which the [VaultCreditCard] will be added. */
    private lateinit var vault: Vault

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_create_edit_credit_card, container, false)
        val toolbar = (view.toolbar as Toolbar)
        toolbar.title = getString(R.string.create_credit_card)
        toolbar.inflateMenu(R.menu.nav_menu_with_save_button)
        toolbar.setOnMenuItemClickListener {
            when (it?.itemId) {
                R.id.save -> {
                    saveCreditCard()
                }
            }
            true
        }
        app = requireActivity().application as App
        vault = args.vault
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        handleExpiryFormatting()
    }

    private fun saveCreditCard() {
        val name = editText_cardName.text.toString().trim()
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
                showLoading(R.string.creating_credit_card)
                withContext(Dispatchers.IO) {
                    app.sudoPasswordManager.add(creditCard, vault)
                }
                hideLoading()
                navController.popBackStack()
            } catch (e: SudoPasswordManagerException) {
                hideLoading()
                showAlertDialog(
                    titleResId = R.string.create_credit_card_failure,
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
        val cardNumber = editText_cardNumber.toSecureField()?.let { VaultItemValue(it) }
        val securityCode = editText_securityCode.toSecureField()?.let { VaultItemValue(it) }
        val notes = editText_notes.toSecureField()?.let { VaultItemNote(it) }
        val expiryString = editText_expiry.text.toString().trim()

        // default to null if date parsing fails
        var expiryDate: Date? = null
        try {
            val dateFormat = SimpleDateFormat("MM/yy", Locale.getDefault())
            expiryDate = dateFormat.parse(expiryString)
        } catch (e: Exception) {
            // failed to parse expiry date string
        }
        return VaultCreditCard(
            id = UUID.randomUUID().toString(),
            name = editText_cardName.text.toString().trim(),
            cardName = editText_cardHolder.text.toString().trim(),
            cardType = editText_cardType.text.toString().trim(),
            cardNumber = cardNumber,
            expiresAt = expiryDate,
            securityCode = securityCode,
            notes = notes
        )
    }

    private fun handleExpiryFormatting() {
        // format the text to be a date string
        editText_expiry.addTextChangedListener(object : TextWatcher {
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
                    if (s!!.length > 5) { s.subSequence(0, 5) } else { s }
                )

                if (stringBuilder.lastIndex == 2) {
                    if (stringBuilder[2] != '/') {
                        stringBuilder.insert(2, "/")
                    }
                }

                // set ignore to true so that onTextChanged doesn't get stuck in a recursive loop
                ignoreTextChange = true
                editText_expiry.setText(stringBuilder.toString())
                editText_expiry.setSelection(stringBuilder.length)
            }
        })
    }

    /**
     * Sets toolbar items and edit text field to enabled/disabled.
     *
     * @param isEnabled If true, toolbar items and edit text field will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        editText_cardName.isEnabled = isEnabled
        editText_cardHolder.isEnabled = isEnabled
        editText_cardType.isEnabled = isEnabled
        editText_cardNumber.isEnabled = isEnabled
        editText_expiry.isEnabled = isEnabled
        editText_securityCode.isEnabled = isEnabled
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
