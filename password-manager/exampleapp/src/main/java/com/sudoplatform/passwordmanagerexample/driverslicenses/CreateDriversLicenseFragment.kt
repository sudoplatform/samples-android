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
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.sudoplatform.passwordmanagerexample.App
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.createLoadingAlertDialog
import com.sudoplatform.passwordmanagerexample.creditcards.colorArray
import com.sudoplatform.passwordmanagerexample.databinding.FragmentCreateEditDriversLicenseBinding
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
import java.util.Calendar
import java.util.UUID
import kotlin.coroutines.CoroutineContext

/**
 * This [CreateDriversLicenseFragment] presents a screen that accepts the values of a set of driver's license credentials.
 *
 * - Links From: [VaultItemsFragment] when the user clicks the Create DriversLicense button
 * - Links To: [PasswordGeneratorDialog] when the user clicks the generate password button
 */
class CreateDriversLicenseFragment : Fragment(), CoroutineScope {

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
    private val args: CreateDriversLicenseFragmentArgs by navArgs()

    /** The [Vault] to which the [VaultDriversLicense] will be added. */
    private lateinit var vault: Vault

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingDelegate.attach(FragmentCreateEditDriversLicenseBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.create_drivers_license)
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

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
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

        val hexColor = colorArray[
            Integer.parseInt(
                view
                    ?.findViewById<RadioButton>(binding.colorRow.radioGroupColors.checkedRadioButtonId)
                    ?.tag as String
            )
        ]
        return VaultDriversLicense(
            id = UUID.randomUUID().toString(),
            favorite = binding.switchFavorite.addAsFavoriteSwitch.isChecked,
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
        )
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
                showLoading(R.string.creating_drivers_license)
                withContext(Dispatchers.IO) {
                    app.sudoPasswordManager.add(driversLicense, vault)
                }
                hideLoading()
                navController.popBackStack()
            } catch (e: SudoPasswordManagerException) {
                hideLoading()
                showAlertDialog(
                    titleResId = R.string.create_drivers_license_failure,
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
