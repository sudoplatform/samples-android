/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmasks

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.sudoplatform.emailexample.App
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.createLoadingAlertDialog
import com.sudoplatform.emailexample.databinding.FragmentProvisionEmailMaskBinding
import com.sudoplatform.emailexample.showAlertDialog
import com.sudoplatform.emailexample.util.ObjectDelegate
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudoemail.types.EmailAddress
import com.sudoplatform.sudoemail.types.ListAPIResult
import com.sudoplatform.sudoemail.types.inputs.CheckEmailAddressAvailabilityInput
import com.sudoplatform.sudoemail.types.inputs.ListEmailAddressesForSudoIdInput
import com.sudoplatform.sudoemail.types.inputs.ProvisionEmailMaskInput
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudoprofiles.exceptions.SudoProfileException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Timer
import java.util.TimerTask
import kotlin.coroutines.CoroutineContext

/**
 * This [ProvisionEmailMaskFragment] presents a form so that a user can provision an Email Mask.
 *
 * - Links From:
 *  - [EmailAddressesFragment]: A user taps the "Create Email Mask" button at the bottom of the
 *   list.
 *
 * - Links To:
 *  - [EmailAddressesFragment]: If a user successfully provisions an Email Mask, they will be
 *   returned to this view.
 */
class ProvisionEmailMaskFragment :
    Fragment(),
    CoroutineScope {
    companion object {
        /**
         * A delay between executing the address availability checks to allow a
         * user to finish typing.
         */
        const val CHECK_DELAY = 1000L

        /** Audience used to retrieve the ownership proof token. */
        const val EMAIL_AUDIENCE = "sudoplatform.email.email-address"
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentProvisionEmailMaskBinding>()
    private val binding by bindingDelegate

    /** Toolbar [Menu] displaying title and create button. */
    private lateinit var toolbarMenu: Menu

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: ProvisionEmailMaskFragmentArgs by navArgs()

    /** A [Sudo] used to retrieve the ownership proof. */
    private lateinit var sudo: Sudo

    /** The ownership proof used to tie a [Sudo] to an [EmailAddress]. */
    private lateinit var ownershipProof: String

    /** A list of provisioned email addresses for the current Sudo. */
    private var emailAddressList = mutableListOf<EmailAddress>()

    /** A list of available mask domains. */
    private var maskDomainsList = mutableListOf<String>()

    /** The currently selected real email address. */
    private var selectedRealEmailAddress: EmailAddress? = null

    /** The currently selected mask domain. */
    private var selectedMaskDomain: String? = null

    /** A mutable list of available email addresses. */
    private var availableAddresses = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingDelegate.attach(FragmentProvisionEmailMaskBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.provision_email_mask)
            inflateMenu(R.menu.nav_menu_with_create_button)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.create -> {
                        provisionEmailMask()
                    }
                }
                true
            }
            toolbarMenu = menu
        }
        app = requireActivity().application as App
        sudo = args.sudo!!
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
        setSudoLabelText()
        configureLocalPartFieldListener()
        toolbarMenu.getItem(0)?.isEnabled = false

        getOwnershipProof()
        loadEmailAddresses()
        loadMaskDomains()
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    /**
     * Retrieve the ownership proof used to bind the [Sudo] and [EmailAddress] together.
     */
    private fun getOwnershipProof() {
        launch {
            try {
                ownershipProof =
                    withContext(Dispatchers.IO) {
                        app.sudoProfilesClient.getOwnershipProof(sudo, EMAIL_AUDIENCE)
                    }
            } catch (e: SudoProfileException) {
                showAlertDialog(
                    titleResId = R.string.ownership_proof_error,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { getOwnershipProof() },
                    negativeButtonResId = android.R.string.cancel,
                )
                toolbarMenu.getItem(0)?.isEnabled = false
            }
        }
    }

    /**
     * Load provisioned email addresses for the current Sudo and populate the real email spinner.
     */
    private fun loadEmailAddresses() {
        launch {
            try {
                showLoading(R.string.load_email_addresses_failure)
                val emailAddresses =
                    withContext(Dispatchers.IO) {
                        val input =
                            ListEmailAddressesForSudoIdInput(
                                sudoId = sudo.id!!,
                            )
                        app.sudoEmailClient.listEmailAddressesForSudoId(input)
                    }
                when (emailAddresses) {
                    is ListAPIResult.Success -> {
                        emailAddressList.clear()
                        emailAddressList.addAll(emailAddresses.result.items)
                        if (emailAddressList.isEmpty()) {
                            showAlertDialog(
                                titleResId = R.string.no_email_addresses_for_mask,
                                positiveButtonResId = android.R.string.ok,
                            )
                            setItemsEnabled(false)
                        } else {
                            populateRealEmailSpinner()
                        }
                    }
                    is ListAPIResult.Partial -> {
                        val cause =
                            emailAddresses.result.failed
                                .first()
                                .cause
                        showAlertDialog(
                            titleResId = R.string.load_email_addresses_failure,
                            message = cause.localizedMessage ?: "$cause",
                            positiveButtonResId = R.string.try_again,
                            onPositive = { loadEmailAddresses() },
                            negativeButtonResId = android.R.string.cancel,
                        )
                    }
                }
            } catch (e: SudoEmailClient.EmailAddressException) {
                showAlertDialog(
                    titleResId = R.string.load_email_addresses_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { loadEmailAddresses() },
                    negativeButtonResId = android.R.string.cancel,
                )
            }
            hideLoading()
        }
    }

    /**
     * Load available mask domains and populate the mask domain spinner.
     */
    private fun loadMaskDomains() {
        launch {
            try {
                @Suppress("DEPRECATION")
                val domains =
                    withContext(Dispatchers.IO) {
                        app.sudoEmailClient.getEmailMaskDomains()
                    }
                maskDomainsList.clear()
                maskDomainsList.addAll(domains)
                if (maskDomainsList.isEmpty()) {
                    showAlertDialog(
                        titleResId = R.string.no_mask_domains_available,
                        positiveButtonResId = android.R.string.ok,
                    )
                    toolbarMenu.getItem(0)?.isEnabled = false
                } else {
                    populateMaskDomainSpinner()
                }
            } catch (e: SudoEmailClient.EmailAddressException) {
                showAlertDialog(
                    titleResId = R.string.load_mask_domains_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { loadMaskDomains() },
                    negativeButtonResId = android.R.string.cancel,
                )
            }
        }
    }

    /**
     * Populate the real email address spinner with the loaded email addresses.
     */
    private fun populateRealEmailSpinner() {
        if (!bindingDelegate.isAttached()) return
        val placeholder = getString(R.string.choose_email_address)
        val emailStrings = listOf(placeholder) + emailAddressList.map { it.emailAddress }
        val adapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                emailStrings,
            )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.realEmailAddressSpinner.adapter = adapter
        binding.realEmailAddressSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    if (position == 0) {
                        selectedRealEmailAddress = null
                    } else {
                        selectedRealEmailAddress = emailAddressList[position - 1]
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selectedRealEmailAddress = null
                }
            }
        selectedRealEmailAddress = null
    }

    /**
     * Populate the mask domain spinner with the loaded mask domains.
     */
    private fun populateMaskDomainSpinner() {
        if (!bindingDelegate.isAttached()) return
        val adapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                maskDomainsList,
            )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.maskDomainSpinner.adapter = adapter
        binding.maskDomainSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    selectedMaskDomain = maskDomainsList[position]
                    val localPart =
                        binding.maskLocalPartField.text
                            .toString()
                            .trim()
                    if (localPart.isNotEmpty()) {
                        checkEmailAddressAvailability(
                            listOf(localPart),
                            listOf(selectedMaskDomain ?: return),
                        )
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selectedMaskDomain = null
                }
            }
        if (maskDomainsList.isNotEmpty()) {
            selectedMaskDomain = maskDomainsList[0]
        }
    }

    /**
     * Configures the local parts EditText field to listen for changes to its value and perform
     * email address availability checks with a debounce delay.
     */
    private fun configureLocalPartFieldListener() {
        binding.availabilityLabel.isVisible = false
        binding.maskLocalPartField.addTextChangedListener(
            object : TextWatcher {
                override fun afterTextChanged(p0: Editable?) { /* no-op */ }

                override fun beforeTextChanged(
                    p0: CharSequence?,
                    p1: Int,
                    p2: Int,
                    p3: Int,
                ) { /* no-op */ }

                var timer = Timer()

                override fun onTextChanged(
                    p0: CharSequence?,
                    p1: Int,
                    p2: Int,
                    p3: Int,
                ) {
                    binding.availabilityLabel.isVisible = false
                    toolbarMenu.getItem(0)?.isEnabled = false
                    timer.cancel()
                    timer = Timer()
                    val localPart = p0.toString().trim()
                    if (localPart.isNotEmpty()) {
                        timer.schedule(
                            object : TimerTask() {
                                override fun run() {
                                    checkEmailAddressAvailability(
                                        listOf(localPart),
                                        listOf(selectedMaskDomain ?: return),
                                    )
                                }
                            },
                            CHECK_DELAY,
                        )
                    }
                }
            },
        )
    }

    /** Provisions an Email Mask from the [SudoEmailClient] based on the form inputs. */
    private fun provisionEmailMask() {
        val localPart =
            binding.maskLocalPartField.text
                .toString()
                .trim()
        if (localPart.isEmpty()) {
            showAlertDialog(
                titleResId = R.string.enter_mask_local_part,
                positiveButtonResId = android.R.string.ok,
            )
            return
        }
        val domain = selectedMaskDomain ?: return
        val realEmail = selectedRealEmailAddress ?: return
        val maskAddress = "$localPart@$domain"
        launch {
            try {
                showLoading(R.string.provisioning_email_mask)
                withContext(Dispatchers.IO) {
                    val input =
                        ProvisionEmailMaskInput(
                            realAddress = realEmail.emailAddress,
                            maskAddress = maskAddress,
                            ownershipProofToken = ownershipProof,
                        )
                    app.sudoEmailClient.provisionEmailMask(input)
                }
                navController.navigate(
                    ProvisionEmailMaskFragmentDirections
                        .actionProvisionEmailMaskFragmentToEmailAddressesFragment(
                            sudo,
                        ),
                )
            } catch (e: SudoEmailClient.EmailAddressException) {
                showAlertDialog(
                    titleResId = R.string.provision_email_mask_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { provisionEmailMask() },
                    negativeButtonResId = android.R.string.cancel,
                )
            }
            hideLoading()
        }
    }

    /**
     * Checks the availability of an email address based on its local parts and domain from the
     * [SudoEmailClient] and returns a list of the available email addresses to provision.
     *
     * @param localParts [List<String>] A list of local parts to check for address availability.
     * @param domains [List<String>] A list of domains to check for address availability.
     */
    private fun checkEmailAddressAvailability(
        localParts: List<String>,
        domains: List<String>,
    ) {
        launch {
            try {
                val emailAddresses =
                    withContext(Dispatchers.IO) {
                        app.sudoEmailClient.checkEmailAddressAvailability(
                            CheckEmailAddressAvailabilityInput(
                                localParts,
                                domains,
                            ),
                        )
                    }
                availableAddresses.clear()
                availableAddresses.addAll(emailAddresses)
                if (availableAddresses.isEmpty()) {
                    setAvailabilityLabel(
                        textResId = R.string.email_address_unavailable,
                        colorResId = R.color.colorNegativeMsg,
                    )
                } else {
                    setAvailabilityLabel(
                        textResId = R.string.email_address_available,
                        colorResId = R.color.colorPositiveMsg,
                    )
                }
            } catch (e: SudoEmailClient.EmailAddressException) {
                setAvailabilityLabel(
                    text = e.localizedMessage ?: "$e",
                    colorResId = R.color.colorNegativeMsg,
                )
            }
        }
    }

    /**
     * Set the availability label text containing an indication of the availability of an email
     * address.
     */
    private fun setAvailabilityLabel(
        text: String = "",
        @StringRes textResId: Int? = null,
        @ColorRes colorResId: Int,
    ) {
        if (bindingDelegate.isAttached()) {
            if (textResId != null && colorResId == R.color.colorPositiveMsg) {
                toolbarMenu.getItem(0)?.isEnabled = true
            } else {
                toolbarMenu.getItem(0)?.isEnabled = false
            }
            binding.availabilityLabel.isVisible = true
            binding.availabilityLabel.text = if (textResId != null) getString(textResId) else text
            binding.availabilityLabel.setTextColor(ContextCompat.getColor(requireContext(), colorResId))
        }
    }

    /** Set the [Sudo] label text containing the [Sudo] alias. */
    private fun setSudoLabelText() {
        binding.sudoLabelText.text = sudo.label
    }

    /**
     * Sets buttons and input field enabled/disabled.
     *
     * @param isEnabled [Boolean] If true, buttons and input field will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        toolbarMenu.getItem(0)?.isEnabled = isEnabled
        binding.maskLocalPartField.isEnabled = isEnabled
        binding.realEmailAddressSpinner.isEnabled = isEnabled
        binding.maskDomainSpinner.isEnabled = isEnabled
    }

    /** Displays the loading [AlertDialog] indicating that an operation is occurring. */
    private fun showLoading(
        @androidx.annotation.StringRes textResId: Int,
    ) {
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
