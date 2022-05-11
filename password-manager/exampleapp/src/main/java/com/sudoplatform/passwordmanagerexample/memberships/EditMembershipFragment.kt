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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.sudoplatform.passwordmanagerexample.App
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.createLoadingAlertDialog
import com.sudoplatform.passwordmanagerexample.creditcards.colorArray
import com.sudoplatform.passwordmanagerexample.databinding.FragmentCreateEditMembershipBinding
import com.sudoplatform.passwordmanagerexample.passwordgenerator.PasswordGeneratorDialog
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.CoroutineContext

/**
 * This [EditMembershipFragment] presents a screen that shows the values of membership credentials
 * and allows the user to edit and save them.
 *
 * - Links From: [VaultItemsFragment] when the user clicks the a row containing some membership credentials.
 * - Links To: [PasswordGeneratorDialog] when the user clicks the generate password button
 */
class EditMembershipFragment : Fragment(), CoroutineScope {

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
    private val args: EditMembershipFragmentArgs by navArgs<EditMembershipFragmentArgs>()

    /** The [Vault] containing the [VaultMembership]. */
    private lateinit var vault: Vault

    /** The [VaultMembership] to be shown and edited. */
    private lateinit var vaultMembership: VaultMembership

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingDelegate.attach(FragmentCreateEditMembershipBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.edit_membership)
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
        vaultMembership = args.vaultMembership

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        loadFromVaultMembership(vaultMembership)
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
        val hexColor =
            colorArray[
                Integer.parseInt(
                    view
                        ?.findViewById<RadioButton>(binding.colorRow.radioGroupColors.checkedRadioButtonId)
                        ?.tag as String
                )
            ]
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

        return vaultMembership.copy(
            name = binding.editTextMembershipName.text.toString().trim(),
            address = binding.editTextAddress.text.toString().trim(),
            email = binding.editTextEmail.text.toString().trim(),
            firstName = binding.editTextFirstName.text.toString().trim(),
            lastName = binding.editTextLastName.text.toString().trim(),
            memberID = binding.editTextMemberId.text.toString().trim(),
            phone = binding.editTextPhone.text.toString().trim(),
            website = binding.editTextWebsite.text.toString().trim(),
            memberSince = memberSinceCalendar.time,
            expires = expiresCalendar.time,
            password = password,
            notes = notes,
            hexColor = hexColor,
            favorite = binding.switchFavorite.addAsFavoriteSwitch.isChecked,
        )
    }

    private fun loadFromVaultMembership(vaultMembership: VaultMembership) {
        val dateFormat =
            SimpleDateFormat(getString(R.string.login_date_format), Locale.getDefault())
        binding.labelCreatedAt.isVisible = true
        binding.labelUpdatedAt.isVisible = true
        binding.labelCreatedAt.setText(
            getString(
                R.string.created_at,
                dateFormat.format(vaultMembership.createdAt)
            )
        )
        binding.labelUpdatedAt.setText(
            getString(
                R.string.updated_at,
                dateFormat.format(vaultMembership.updatedAt)
            )
        )
        binding.editTextMembershipName.setText(vaultMembership.name)
        vaultMembership.firstName?.let { binding.editTextFirstName.setText(it) }
        vaultMembership.lastName?.let { binding.editTextLastName.setText(it) }
        vaultMembership.address?.let { binding.editTextAddress.setText(it) }
        vaultMembership.email?.let { binding.editTextEmail.setText(it) }
        vaultMembership.memberID?.let { binding.editTextMemberId.setText(it) }
        vaultMembership.password?.let { binding.editTextPassword.setText(it.getValue()) }
        vaultMembership.phone?.let { binding.editTextPhone.setText(it) }
        vaultMembership.website?.let { binding.editTextWebsite.setText(it) }
        vaultMembership.memberSince?.let {
            val memberSinceCalendar = Calendar.getInstance()
            memberSinceCalendar.time = it
            binding.datePickerMemberSince.init(
                memberSinceCalendar.get(Calendar.YEAR),
                memberSinceCalendar.get(Calendar.MONTH),
                memberSinceCalendar.get(Calendar.DAY_OF_MONTH)
            ) { _, _, _, _ -> }
        }
        vaultMembership.expires?.let {
            val expiresCalendar = Calendar.getInstance()
            expiresCalendar.time = it
            binding.datePickerExpires.init(
                expiresCalendar.get(Calendar.YEAR),
                expiresCalendar.get(Calendar.MONTH),
                expiresCalendar.get(Calendar.DAY_OF_MONTH)
            ) { _, _, _, _ -> }
        }
        vaultMembership.notes?.let { binding.editTextNotes.setText(it.getValue()) }
        view?.findViewWithTag<RadioButton>(
            colorArray.indexOf(vaultMembership.hexColor).toString()
        )?.let { binding.colorRow.radioGroupColors.check(it.id) }
        vaultMembership.favorite?.let { binding.switchFavorite.addAsFavoriteSwitch.isChecked = it }
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
                showLoading(R.string.saving_membership)
                withContext(Dispatchers.IO) {
                    app.sudoPasswordManager.update(membership, vault)
                    app.sudoPasswordManager.update(vault)
                }
                hideLoading()
                navController.popBackStack()
            } catch (e: SudoPasswordManagerException) {
                hideLoading()
                showAlertDialog(
                    titleResId = R.string.save_membership_failure,
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
        binding.editTextPassword.isEnabled = isEnabled
        binding.editTextPhone.isEnabled = isEnabled
        binding.datePickerMemberSince.isEnabled = isEnabled
        binding.datePickerExpires.isEnabled = isEnabled
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
