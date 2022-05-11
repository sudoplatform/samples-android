/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.memberships

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
import com.sudoplatform.passwordmanagerexample.databinding.FragmentCreateEditMembershipBinding
import com.sudoplatform.passwordmanagerexample.showAlertDialog
import com.sudoplatform.passwordmanagerexample.toSecureField
import com.sudoplatform.passwordmanagerexample.util.ObjectDelegate
import com.sudoplatform.sudopasswordmanager.SudoPasswordManagerException
import com.sudoplatform.sudopasswordmanager.models.Vault
import com.sudoplatform.sudopasswordmanager.models.VaultItemNote
import com.sudoplatform.sudopasswordmanager.models.VaultItemPassword
import com.sudoplatform.sudopasswordmanager.models.VaultMembership
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
 * This [CreateMembershipFragment] presents a screen that accepts the values of a set of membership credentials.
 *
 * - Links From: [VaultItemsFragment] when the user clicks the Create Membership button
 * - Links To: [PasswordGeneratorDialog] when the user clicks the generate password button
 */
class CreateMembershipFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** View binding to the views defined in the layout */
    private val bindingDelegate = ObjectDelegate<FragmentCreateEditMembershipBinding>()
    private val binding by bindingDelegate

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** The [Application] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: CreateMembershipFragmentArgs by navArgs()

    /** The [Vault] to which the [VaultMembership] will be added. */
    private lateinit var vault: Vault

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingDelegate.attach(FragmentCreateEditMembershipBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.create_membership)
            inflateMenu(R.menu.nav_menu_with_save_button)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.save -> {
                        saveMembership()
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

    private fun toVaultMembership(): VaultMembership {
        val notes = binding.editTextNotes.toSecureField()?.let { VaultItemNote(it) }
        val password = binding.editTextPassword.toSecureField()?.let { VaultItemPassword(it) }
        val memberSinceDatePicker = binding.datePickerMemberSince
        val memberSinceCalendar: Calendar = Calendar.getInstance()
        memberSinceCalendar.set(
            memberSinceDatePicker.year,
            memberSinceDatePicker.month,
            memberSinceDatePicker.dayOfMonth
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
        return VaultMembership(
            id = UUID.randomUUID().toString(),
            favorite = binding.switchFavorite.addAsFavoriteSwitch.isChecked,
            name = binding.editTextMembershipName.text.toString().trim(),
            address = binding.editTextAddress.text.toString().trim(),
            website = binding.editTextWebsite.text.toString().trim(),
            email = binding.editTextEmail.text.toString().trim(),
            memberID = binding.editTextMemberId.text.toString().trim(),
            firstName = binding.editTextFirstName.text.toString().trim(),
            lastName = binding.editTextLastName.text.toString().trim(),
            phone = binding.editTextPhone.text.toString().trim(),
            memberSince = memberSinceCalendar.time,
            expires = expiresCalendar.time,
            password = password,
            notes = notes,
            hexColor = hexColor,
        )
    }

    private fun saveMembership() {

        val name = binding.editTextMembershipName.text.toString().trim()
        if (name.isEmpty()) {
            showAlertDialog(
                titleResId = R.string.enter_a_name_for_the_membership,
                positiveButtonResId = android.R.string.ok
            )
            return
        }

        val membership = toVaultMembership()

        launch {
            try {
                showLoading(R.string.creating_membership)
                withContext(Dispatchers.IO) {
                    app.sudoPasswordManager.add(membership, vault)
                }
                hideLoading()
                navController.popBackStack()
            } catch (e: SudoPasswordManagerException) {
                hideLoading()
                showAlertDialog(
                    titleResId = R.string.create_membership_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { saveMembership() },
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
        binding.editTextMembershipName.isEnabled = isEnabled
        binding.editTextAddress.isEnabled = isEnabled
        binding.editTextEmail.isEnabled = isEnabled
        binding.editTextFirstName.isEnabled = isEnabled
        binding.editTextLastName.isEnabled = isEnabled
        binding.editTextMemberId.isEnabled = isEnabled
        binding.datePickerMemberSince.isEnabled = isEnabled
        binding.datePickerExpires.isEnabled = isEnabled
        binding.editTextPassword.isEnabled = isEnabled
        binding.editTextPhone.isEnabled = isEnabled
        binding.editTextWebsite.isEnabled = isEnabled
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
