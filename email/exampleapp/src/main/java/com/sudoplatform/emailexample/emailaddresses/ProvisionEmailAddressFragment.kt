/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaddresses

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import com.sudoplatform.emailexample.databinding.FragmentProvisionEmailAddressBinding
import com.sudoplatform.emailexample.showAlertDialog
import com.sudoplatform.emailexample.util.ObjectDelegate
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudoemail.types.CachePolicy
import com.sudoplatform.sudoemail.types.EmailAddress
import com.sudoplatform.sudoemail.types.inputs.CheckEmailAddressAvailabilityInput
import com.sudoplatform.sudoemail.types.inputs.ProvisionEmailAddressInput
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
 * This [ProvisionEmailAddressFragment] presents a form so that a user can provision an [EmailAddress].
 *
 * - Links From:
 *  - [EmailAddressesFragment]: A user taps the "Create Email Address" button at the bottom of the
 *   list.
 *
 * - Links To:
 *  - [EmailAddressesFragment]: If a user successfully provisions an [EmailAddress], they will be
 *   returned to this view.
 */
class ProvisionEmailAddressFragment : Fragment(), CoroutineScope {

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
    private val bindingDelegate = ObjectDelegate<FragmentProvisionEmailAddressBinding>()
    private val binding by bindingDelegate

    /** Toolbar [Menu] displaying title and create button. */
    private lateinit var toolbarMenu: Menu

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** A mutable list of available email addresses. */
    private var availableAddresses = mutableListOf<String>()

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: ProvisionEmailAddressFragmentArgs by navArgs()

    /** A [Sudo] used to retrieve the ownership proof. */
    private lateinit var sudo: Sudo

    /** The ownership proof used to tie a [Sudo] to an [EmailAddress]. */
    private lateinit var ownershipProof: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingDelegate.attach(FragmentProvisionEmailAddressBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.provision_email_address)
            inflateMenu(R.menu.nav_menu_with_create_button)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.create -> {
                        provisionEmailAddress()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureAddressFieldListener()
        setSudoLabelText()
        navController = Navigation.findNavController(view)

        binding.learnMoreButton.setOnClickListener {
            learnMore()
        }
        getOwnershipProof()
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    /** Provisions an [EmailAddress] from the [SudoEmailClient] based on the selected address. */
    private fun provisionEmailAddress() {
        val localPart = binding.addressField.text.toString().trim()
        if (localPart.isEmpty()) {
            showAlertDialog(
                titleResId = R.string.enter_local_part,
                positiveButtonResId = android.R.string.ok
            )
            return
        }
        launch {
            try {
                showLoading(R.string.provisioning_email_address)
                if (availableAddresses.isNotEmpty()) {
                    val address = availableAddresses.first()
                    withContext(Dispatchers.IO) {
                        val input = ProvisionEmailAddressInput(
                            emailAddress = address,
                            ownershipProofToken = ownershipProof
                        )
                        app.sudoEmailClient.provisionEmailAddress(input)
                    }
                    showAlertDialog(
                        titleResId = R.string.success,
                        positiveButtonResId = android.R.string.ok,
                        onPositive = {
                            navController.navigate(
                                ProvisionEmailAddressFragmentDirections
                                    .actionProvisionEmailAddressFragmentToEmailAddressesFragment(
                                        sudo
                                    )
                            )
                        }
                    )
                }
            } catch (e: SudoEmailClient.EmailAddressException) {
                showAlertDialog(
                    titleResId = R.string.provision_email_address_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { provisionEmailAddress() },
                    negativeButtonResId = android.R.string.cancel
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
     */
    private fun checkEmailAddressAvailability(localParts: List<String>) {
        launch {
            try {
                val supportedDomains = app.sudoEmailClient.getSupportedEmailDomains(
                    CachePolicy.REMOTE_ONLY
                )
                val emailAddresses = app.sudoEmailClient.checkEmailAddressAvailability(
                    CheckEmailAddressAvailabilityInput(
                        localParts,
                        supportedDomains
                    )
                )
                availableAddresses.clear()
                availableAddresses.addAll(emailAddresses)
                if (availableAddresses.isEmpty()) {
                    setAvailabilityLabel(
                        textResId = R.string.email_address_unavailable,
                        colorResId = R.color.colorNegativeMsg
                    )
                } else {
                    binding.addressHolder.text = availableAddresses.first()
                    setAvailabilityLabel(
                        textResId = R.string.email_address_available,
                        colorResId = R.color.colorPositiveMsg
                    )
                }
            } catch (e: SudoEmailClient.EmailAddressException) {
                setAvailabilityLabel(
                    text = e.localizedMessage ?: "$e",
                    colorResId = R.color.colorNegativeMsg
                )
            }
        }
    }

    /**
     * Retrieve the ownership proof used to bind the [Sudo] and [EmailAddress] together.
     */
    private fun getOwnershipProof() {
        launch {
            try {
                ownershipProof = withContext(Dispatchers.IO) {
                    app.sudoProfilesClient.getOwnershipProof(sudo, EMAIL_AUDIENCE)
                }
            } catch (e: SudoProfileException) {
                showAlertDialog(
                    titleResId = R.string.ownership_proof_error,
                    message = e.localizedMessage ?: "$e",
                    negativeButtonResId = android.R.string.cancel
                )
            }
        }
    }

    /** Navigates to a Sudo Platform web page when the "Learn More" button is pressed. */
    private fun learnMore() {
        val openUrl = Intent(Intent.ACTION_VIEW)
        openUrl.data = Uri.parse(getString(R.string.provision_email_address_doc_url))
        startActivity(openUrl)
    }

    /**
     * Configures the local parts [EditText] field to listen for changes to its value and perform
     * email address availability checks.
     */
    private fun configureAddressFieldListener() {
        binding.availabilityLabel.isVisible = false
        binding.addressField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) { /* no-op */ }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { /* no-op */ }
            var timer = Timer()
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                binding.availabilityLabel.isVisible = false
                toolbarMenu.getItem(0)?.isEnabled = false
                timer.cancel()
                timer = Timer()
                timer.schedule(
                    object : TimerTask() {
                        override fun run() {
                            checkEmailAddressAvailability(listOf(p0.toString()))
                        }
                    },
                    CHECK_DELAY
                )
            }
        })
    }

    /**
     * Set the availability label text containing an indication of the availability of an email
     * address.
     */
    private fun setAvailabilityLabel(
        text: String = "",
        @StringRes textResId: Int? = null,
        @ColorRes colorResId: Int
    ) {
        toolbarMenu.getItem(0)?.isEnabled = true
        binding.availabilityLabel.isVisible = true
        binding.availabilityLabel.text = if (textResId != null) getString(textResId) else text
        binding.availabilityLabel.setTextColor(ContextCompat.getColor(requireContext(), colorResId))
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
        binding.addressField.isEnabled = isEnabled
        binding.learnMoreButton.isEnabled = isEnabled
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
