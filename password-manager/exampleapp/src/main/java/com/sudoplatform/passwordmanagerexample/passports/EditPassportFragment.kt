/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.passports

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
import com.sudoplatform.passwordmanagerexample.databinding.FragmentCreateEditPassportBinding
import com.sudoplatform.passwordmanagerexample.passwordgenerator.PasswordGeneratorDialog
import com.sudoplatform.passwordmanagerexample.showAlertDialog
import com.sudoplatform.passwordmanagerexample.toSecureField
import com.sudoplatform.passwordmanagerexample.util.ObjectDelegate
import com.sudoplatform.sudopasswordmanager.SudoPasswordManagerException
import com.sudoplatform.sudopasswordmanager.models.Vault
import com.sudoplatform.sudopasswordmanager.models.VaultItemNote
import com.sudoplatform.sudopasswordmanager.models.VaultItemValue
import com.sudoplatform.sudopasswordmanager.models.VaultPassport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.CoroutineContext

/**
 * This [EditPassportFragment] presents a screen that shows the values of passport credentials
 * and allows the user to edit and save them.
 *
 * - Links From: [VaultItemsFragment] when the user clicks the a row containing some passport credentials.
 * - Links To: [PasswordGeneratorDialog] when the user clicks the generate password button
 */
class EditPassportFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** View binding to the views defined in the layout */
    private val bindingDelegate = ObjectDelegate<FragmentCreateEditPassportBinding>()
    private val binding by bindingDelegate

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** The [Application] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: EditPassportFragmentArgs by navArgs<EditPassportFragmentArgs>()

    /** The [Vault] containing the [VaultPassport]. */
    private lateinit var vault: Vault

    /** The [VaultPassport] to be shown and edited. */
    private lateinit var vaultPassport: VaultPassport

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingDelegate.attach(FragmentCreateEditPassportBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.edit_passport)
            inflateMenu(R.menu.nav_menu_with_save_button)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.save -> {
                        savePassport()
                    }
                }
                true
            }
        }
        app = requireActivity().application as App

        vault = args.vault
        vaultPassport = args.vaultPassport

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        loadFromVaultPassport(vaultPassport)
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    private fun toVaultPassport(): VaultPassport {
        val notes = binding.editTextNotes.toSecureField()?.let { VaultItemNote(it) }
        val passportNumber = binding.editTextPassportNumber.toSecureField()?.let { VaultItemValue(it) }
        val hexColor =
            colorArray[
                Integer.parseInt(
                    view
                        ?.findViewById<RadioButton>(binding.colorRow.radioGroupColors.checkedRadioButtonId)
                        ?.tag as String
                )
            ]
        val dobDatePicker = binding.datePickerDateOfBirth
        val dobCalendar: Calendar = Calendar.getInstance()
        dobCalendar.set(
            dobDatePicker.year,
            dobDatePicker.month,
            dobDatePicker.dayOfMonth
        )
        val doiDatePicker = binding.datePickerDateOfIssue
        val doiCalendar: Calendar = Calendar.getInstance()
        doiCalendar.set(
            doiDatePicker.year,
            doiDatePicker.month,
            dobDatePicker.dayOfMonth
        )
        val expiresDatePicker = binding.datePickerExpires
        val expiresCalendar: Calendar = Calendar.getInstance()
        expiresCalendar.set(
            expiresDatePicker.year,
            expiresDatePicker.month,
            expiresDatePicker.dayOfMonth
        )

        return vaultPassport.copy(
            name = binding.editTextPassportName.text.toString().trim(),
            firstName = binding.editTextFirstName.text.toString().trim(),
            lastName = binding.editTextLastName.text.toString().trim(),
            gender = binding.editTextGender.text.toString().trim(),
            placeOfBirth = binding.editTextPlaceOfBirth.text.toString().trim(),
            issuingCountry = binding.editTextPlaceOfBirth.text.toString().trim(),
            dateOfBirth = dobCalendar.time,
            dateOfIssue = doiCalendar.time,
            expires = expiresCalendar.time,
            passportNumber = passportNumber,
            notes = notes,
            hexColor = hexColor,
            favorite = binding.switchFavorite.addAsFavoriteSwitch.isChecked,
        )
    }

    private fun loadFromVaultPassport(vaultPassport: VaultPassport) {
        val dateFormat =
            SimpleDateFormat(getString(R.string.login_date_format), Locale.getDefault())
        binding.labelCreatedAt.isVisible = true
        binding.labelUpdatedAt.isVisible = true
        binding.labelCreatedAt.setText(
            getString(
                R.string.created_at,
                dateFormat.format(vaultPassport.createdAt)
            )
        )
        binding.labelUpdatedAt.setText(
            getString(
                R.string.updated_at,
                dateFormat.format(vaultPassport.updatedAt)
            )
        )
        binding.editTextPassportName.setText(vaultPassport.name)
        vaultPassport.firstName?.let { binding.editTextFirstName.setText(it) }
        vaultPassport.lastName?.let { binding.editTextLastName.setText(it) }
        vaultPassport.gender?.let { binding.editTextGender.setText(it) }
        vaultPassport.issuingCountry?.let { binding.editTextIssuingCountry.setText(it) }
        vaultPassport.placeOfBirth?.let { binding.editTextPlaceOfBirth.setText(it) }
        vaultPassport.passportNumber?.let { binding.editTextPassportNumber.setText(it.getValue()) }
        vaultPassport.dateOfBirth?.let {
            val dobCalendar = Calendar.getInstance()
            dobCalendar.time = it
            binding.datePickerDateOfBirth.init(
                dobCalendar.get(Calendar.YEAR),
                dobCalendar.get(Calendar.MONTH),
                dobCalendar.get(Calendar.DAY_OF_MONTH)
            ) { _, _, _, _ -> }
        }
        vaultPassport.dateOfIssue?.let {
            val doiCalendar = Calendar.getInstance()
            doiCalendar.time = it
            binding.datePickerDateOfIssue.init(
                doiCalendar.get(Calendar.YEAR),
                doiCalendar.get(Calendar.MONTH),
                doiCalendar.get(Calendar.DAY_OF_MONTH)
            ) { _, _, _, _ -> }
        }
        vaultPassport.expires?.let {
            val expiresCalendar = Calendar.getInstance()
            expiresCalendar.time = it
            binding.datePickerExpires.init(
                expiresCalendar.get(Calendar.YEAR),
                expiresCalendar.get(Calendar.MONTH),
                expiresCalendar.get(Calendar.DAY_OF_MONTH)
            ) { _, _, _, _ -> }
        }
        vaultPassport.notes?.let { binding.editTextNotes.setText(it.getValue()) }
        view?.findViewWithTag<RadioButton>(
            colorArray.indexOf(vaultPassport.hexColor).toString()
        )?.let { binding.colorRow.radioGroupColors.check(it.id) }
        vaultPassport.favorite?.let { binding.switchFavorite.addAsFavoriteSwitch.isChecked = it }
    }

    private fun savePassport() {

        val name = binding.editTextPassportName.text.toString().trim()
        if (name.isEmpty()) {
            showAlertDialog(
                titleResId = R.string.enter_a_name_for_the_passport,
                positiveButtonResId = android.R.string.ok
            )
            return
        }

        val passport = toVaultPassport()

        launch {
            try {
                showLoading(R.string.saving_passport)
                withContext(Dispatchers.IO) {
                    app.sudoPasswordManager.update(passport, vault)
                    app.sudoPasswordManager.update(vault)
                }
                hideLoading()
                navController.popBackStack()
            } catch (e: SudoPasswordManagerException) {
                hideLoading()
                showAlertDialog(
                    titleResId = R.string.save_passport_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { savePassport() },
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
        binding.editTextPassportName.isEnabled = isEnabled
        binding.editTextGender.isEnabled = isEnabled
        binding.editTextIssuingCountry.isEnabled = isEnabled
        binding.editTextFirstName.isEnabled = isEnabled
        binding.editTextLastName.isEnabled = isEnabled
        binding.editTextPassportNumber.isEnabled = isEnabled
        binding.editTextPlaceOfBirth.isEnabled = isEnabled
        binding.datePickerDateOfBirth.isEnabled = isEnabled
        binding.datePickerDateOfIssue.isEnabled = isEnabled
        binding.datePickerExpires.isEnabled = isEnabled
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
