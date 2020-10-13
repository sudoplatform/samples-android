/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
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
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.sudoplatform.emailexample.App
import com.sudoplatform.emailexample.MissingFragmentArgumentException
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.createLoadingAlertDialog
import com.sudoplatform.emailexample.showAlertDialog
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudoemail.types.CachePolicy
import com.sudoplatform.sudoemail.types.EmailAddress
import com.sudoplatform.sudoprofiles.Sudo
import java.util.Timer
import java.util.TimerTask
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_provision_email_address.*
import kotlinx.android.synthetic.main.fragment_provision_email_address.view.*
import kotlinx.android.synthetic.main.fragment_provision_email_address.view.toolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** Toolbar [Menu] displaying title and create button. */
    private lateinit var toolbarMenu: Menu

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private lateinit var loading: AlertDialog

    /** A mutable list of available email addresses. */
    private var availableAddresses = mutableListOf<String>()

    /** A [Sudo] identifier used to provision an [EmailAddress]. */
    private lateinit var sudoId: String

    /** The [Sudo] label used to present to the user. */
    private lateinit var sudoLabel: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_provision_email_address, container, false)
        val toolbar = (view.toolbar as Toolbar)
        toolbar.title = getString(R.string.provision_email_address)

        toolbar.inflateMenu(R.menu.nav_menu_with_create_button)
        toolbar.setOnMenuItemClickListener {
            when (it?.itemId) {
                R.id.create -> {
                    provisionEmailAddress()
                }
            }
            true
        }
        toolbarMenu = toolbar.menu
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureAddressFieldListener()
        setSudoLabelText()
        navController = Navigation.findNavController(view)

        view.learnMoreButton.setOnClickListener {
            learnMore()
        }
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        super.onDestroy()
    }

    /** Provisions an [EmailAddress] from the [SudoEmailClient] based on the selected address. */
    private fun provisionEmailAddress() {
        val localPart = addressField.text.toString().trim()
        if (localPart.isEmpty()) {
            showAlertDialog(
                titleResId = R.string.enter_local_part,
                positiveButtonResId = android.R.string.ok
            )
            return
        }
        val app = requireActivity().application as App
        sudoId = requireArguments().getString(getString(R.string.sudo_id))
            ?: throw MissingFragmentArgumentException("Sudo identifier missing")
        launch {
            try {
                showLoading(R.string.provisioning_email_address)
                if (availableAddresses.isNotEmpty()) {
                    val address = availableAddresses.first()
                    withContext(Dispatchers.IO) {
                        app.sudoEmailClient.provisionEmailAddress(address, sudoId)
                    }
                    showAlertDialog(
                        titleResId = R.string.success,
                        positiveButtonResId = android.R.string.ok,
                        onPositive = {
                            val bundle = bundleOf(
                                getString(R.string.sudo_id) to sudoId,
                                getString(R.string.sudo_label) to sudoLabel
                            )
                            navController.navigate(R.id.action_provisionEmailAddressFragment_to_emailAddressesFragment, bundle)
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
     * @param localParts A list of local parts to check for address availability.
     */
    private fun checkEmailAddressAvailability(localParts: List<String>) {
        val app = requireActivity().application as App
        launch {
            try {
                val supportedDomains = app.sudoEmailClient.getSupportedEmailDomains(
                    CachePolicy.REMOTE_ONLY
                )
                val emailAddresses = app.sudoEmailClient.checkEmailAddressAvailability(
                    localParts,
                    supportedDomains
                )
                availableAddresses.clear()
                availableAddresses.addAll(emailAddresses)
                if (availableAddresses.isEmpty()) {
                    setAvailabilityLabel(
                        textResId = R.string.email_address_unavailable,
                        colorResId = R.color.colorNegativeMsg
                    )
                } else {
                    addressHolder?.text = availableAddresses.first()
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
        availabilityLabel?.isVisible = false
        addressField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            var timer = Timer()
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                availabilityLabel?.isVisible = false
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
        availabilityLabel?.isVisible = true
        availabilityLabel?.text = if (textResId != null) getString(textResId) else text
        availabilityLabel?.setTextColor(ContextCompat.getColor(requireContext(), colorResId))
    }

    /** Set the [Sudo] label text containing the [Sudo] alias. */
    private fun setSudoLabelText() {
        sudoLabel = requireArguments().getString(getString(R.string.sudo_label))
            ?: throw MissingFragmentArgumentException("Sudo identifier missing")
        sudoLabelText?.text = sudoLabel
    }

    /**
     * Sets buttons and input field enabled/disabled.
     *
     * @param isEnabled If true, buttons and input field will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        toolbarMenu.getItem(0)?.isEnabled = isEnabled
        addressField?.isEnabled = isEnabled
        learnMoreButton?.isEnabled = isEnabled
    }

    /** Displays the loading [AlertDialog] indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int) {
        loading = createLoadingAlertDialog(textResId)
        loading.show()
        setItemsEnabled(false)
    }

    /** Dismisses the loading [AlertDialog] indicating that an operation has finished. */
    private fun hideLoading() {
        loading.dismiss()
        setItemsEnabled(true)
    }
}
