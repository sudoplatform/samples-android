/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.driverslicenses

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
import com.sudoplatform.passwordmanagerexample.databinding.FragmentCreateEditDriversLicenseBinding
import com.sudoplatform.passwordmanagerexample.passwordgenerator.PasswordGeneratorDialog
import com.sudoplatform.passwordmanagerexample.showAlertDialog
import com.sudoplatform.passwordmanagerexample.toSecureField
import com.sudoplatform.passwordmanagerexample.util.ObjectDelegate
import com.sudoplatform.sudopasswordmanager.SudoPasswordManagerException
import com.sudoplatform.sudopasswordmanager.models.Vault
import com.sudoplatform.sudopasswordmanager.models.VaultDriversLicense
import com.sudoplatform.sudopasswordmanager.models.VaultItemNote
import com.sudoplatform.sudopasswordmanager.models.VaultItemValue
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
 * This [EditDriversLicenseFragment] presents a screen that shows the values of driver's license credentials
 * and allows the user to edit and save them.
 *
 * - Links From: [VaultItemsFragment] when the user clicks the a row containing some driver's license credentials.
 * - Links To: [PasswordGeneratorDialog] when the user clicks the generate password button
 */
class EditDriversLicenseFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** View binding to the views defined in the layout */
    private val bindingDelegate = ObjectDelegate<FragmentCreateEditDriversLicenseBinding>()
    private val binding by bindingDelegate

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** The [Application] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: EditDriversLicenseFragmentArgs by navArgs<EditDriversLicenseFragmentArgs>()

    /** The [Vault] containing the [VaultDriversLicense]. */
    private lateinit var vault: Vault

    /** The [VaultDriversLicense] to be shown and edited. */
    private lateinit var vaultDriversLicense: VaultDriversLicense

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingDelegate.attach(FragmentCreateEditDriversLicenseBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.edit_drivers_license)
            inflateMenu(R.menu.nav_menu_with_save_button)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.save -> {
                        saveDriversLicense()
                    }
                }
                true
            }
        }
        app = requireActivity().application as App

        vault = args.vault
        vaultDriversLicense = args.vaultDriversLicense

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        loadFromVaultDriversLicense(vaultDriversLicense)
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    private fun toVaultDriversLicense(): VaultDriversLicense {
        val notes = binding.editTextNotes.toSecureField()?.let { VaultItemNote(it) }
        val number = binding.editTextNumber.toSecureField()?.let { VaultItemValue(it) }
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
            doiDatePicker.dayOfMonth
        )
        val expiresDatePicker = binding.datePickerExpires
        val expiresCalendar: Calendar = Calendar.getInstance()
        expiresCalendar.set(
            expiresDatePicker.year,
            expiresDatePicker.month,
            expiresDatePicker.dayOfMonth
        )

        return vaultDriversLicense.copy(
            name = binding.editTextDriversLicenseName.text.toString().trim(),
            country = binding.editTextCountry.text.toString().trim(),
            number = number,
            firstName = binding.editTextFirstName.text.toString().trim(),
            lastName = binding.editTextLastName.text.toString().trim(),
            gender = binding.editTextGender.text.toString().trim(),
            dateOfBirth = dobCalendar.time,
            dateOfIssue = doiCalendar.time,
            expires = expiresCalendar.time,
            state = binding.editTextState.text.toString().trim(),
            notes = notes,
            hexColor = hexColor,
            favorite = binding.switchFavorite.addAsFavoriteSwitch.isChecked,
        )
    }

    private fun loadFromVaultDriversLicense(vaultDriversLicense: VaultDriversLicense) {
        val dateFormat =
            SimpleDateFormat(getString(R.string.login_date_format), Locale.getDefault())
        binding.labelCreatedAt.isVisible = true
        binding.labelUpdatedAt.isVisible = true
        binding.labelCreatedAt.setText(
            getString(
                R.string.created_at,
                dateFormat.format(vaultDriversLicense.createdAt)
            )
        )
        binding.labelUpdatedAt.setText(
            getString(
                R.string.updated_at,
                dateFormat.format(vaultDriversLicense.updatedAt)
            )
        )
        binding.editTextDriversLicenseName.setText(vaultDriversLicense.name)
        vaultDriversLicense.country?.let { binding.editTextCountry.setText(it) }
        vaultDriversLicense.number?.let { binding.editTextNumber.setText(it.getValue()) }
        vaultDriversLicense.firstName?.let { binding.editTextFirstName.setText(it) }
        vaultDriversLicense.lastName?.let { binding.editTextLastName.setText(it) }
        vaultDriversLicense.gender?.let { binding.editTextGender.setText(it) }
        vaultDriversLicense.dateOfBirth?.let {
            val dobCalendar = Calendar.getInstance()
            dobCalendar.time = it
            binding.datePickerDateOfBirth.init(
                dobCalendar.get(Calendar.YEAR),
                dobCalendar.get(Calendar.MONTH),
                dobCalendar.get(Calendar.DAY_OF_MONTH)
            ) { _, _, _, _ -> }
        }
        vaultDriversLicense.dateOfIssue?.let {
            val doiCalendar = Calendar.getInstance()
            doiCalendar.time = it
            binding.datePickerDateOfIssue.init(
                doiCalendar.get(Calendar.YEAR),
                doiCalendar.get(Calendar.MONTH),
                doiCalendar.get(Calendar.DAY_OF_MONTH)
            ) { _, _, _, _ -> }
        }
        vaultDriversLicense.expires?.let {
            val expiresCalendar = Calendar.getInstance()
            expiresCalendar.time = it
            binding.datePickerExpires.init(
                expiresCalendar.get(Calendar.YEAR),
                expiresCalendar.get(Calendar.MONTH),
                expiresCalendar.get(Calendar.DAY_OF_MONTH)
            ) { _, _, _, _ -> }
        }
        vaultDriversLicense.state?.let { binding.editTextState.setText(it) }
        vaultDriversLicense.notes?.let { binding.editTextNotes.setText(it.getValue()) }
        view?.findViewWithTag<RadioButton>(
            colorArray.indexOf(vaultDriversLicense.hexColor).toString()
        )?.let { binding.colorRow.radioGroupColors.check(it.id) }
        vaultDriversLicense.favorite?.let { binding.switchFavorite.addAsFavoriteSwitch.isChecked = it }
    }

    private fun saveDriversLicense() {

        val name = binding.editTextDriversLicenseName.text.toString().trim()
        if (name.isEmpty()) {
            showAlertDialog(
                titleResId = R.string.enter_a_name_for_the_driver_s_license,
                positiveButtonResId = android.R.string.ok
            )
            return
        }

        val driversLicense = toVaultDriversLicense()

        launch {
            try {
                showLoading(R.string.saving_drivers_license)
                withContext(Dispatchers.IO) {
                    app.sudoPasswordManager.update(driversLicense, vault)
                    app.sudoPasswordManager.update(vault)
                }
                hideLoading()
                navController.popBackStack()
            } catch (e: SudoPasswordManagerException) {
                hideLoading()
                showAlertDialog(
                    titleResId = R.string.save_drivers_license_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { saveDriversLicense() },
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
        binding.editTextCountry.isEnabled = isEnabled
        binding.editTextNumber.isEnabled = isEnabled
        binding.editTextFirstName.isEnabled = isEnabled
        binding.editTextLastName.isEnabled = isEnabled
        binding.editTextGender.isEnabled = isEnabled
        binding.datePickerDateOfBirth.isEnabled = isEnabled
        binding.datePickerDateOfIssue.isEnabled = isEnabled
        binding.datePickerExpires.isEnabled = isEnabled
        binding.editTextState.isEnabled = isEnabled
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
