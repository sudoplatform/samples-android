/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.identityverification

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.sudoplatform.sudoidentityverification.SudoIdentityVerificationException
import com.sudoplatform.sudoidentityverification.types.IdentityDataProcessingConsentContent
import com.sudoplatform.sudoidentityverification.types.inputs.IdentityDataProcessingConsentContentInput
import com.sudoplatform.sudoidentityverification.types.inputs.IdentityDataProcessingConsentInput
import com.sudoplatform.virtualcardsexample.App
import com.sudoplatform.virtualcardsexample.R
import com.sudoplatform.virtualcardsexample.createLoadingAlertDialog
import com.sudoplatform.virtualcardsexample.databinding.FragmentIdentityVerificationConsentBinding
import com.sudoplatform.virtualcardsexample.mainmenu.MainMenuFragment
import com.sudoplatform.virtualcardsexample.showAlertDialog
import com.sudoplatform.virtualcardsexample.util.ObjectDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * This [IdentityVerificationConsentFragment] presents a screen to display and manage
 * ID verification consent settings.
 *
 * - Links From:
 *  - [MainMenuFragment]: A user chooses the "Secure ID Verification Consent" option from the main menu which
 *   will show this view allowing the user to manage ID verification consent.
 */
class IdentityVerificationConsentFragment :
    Fragment(),
    CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentIdentityVerificationConsentBinding>()
    private val binding by bindingDelegate

    /** Toolbar [Menu] displaying title and toolbar items. */
    private lateinit var toolbarMenu: Menu

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** Stores the retrieved consent content for use when providing consent */
    private var retrievedConsentContent: IdentityDataProcessingConsentContent? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingDelegate.attach(FragmentIdentityVerificationConsentBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.secure_id_verification_consent)
            inflateMenu(R.menu.nav_menu_info)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.info -> {
                        showAlertDialog(
                            titleResId = R.string.secure_id_verification_consent,
                            messageResId = R.string.identity_verification_consent_learn_more,
                            positiveButtonResId = android.R.string.ok,
                            negativeButtonResId = R.string.learn_more,
                            onNegative = { learnMore() },
                        )
                    }
                }
                true
            }
            toolbarMenu = menu
        }
        app = requireActivity().application as App
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize the UI with current consent status

        // Set up click listeners for the displayed buttons
        binding.retrieveConsentTextButton.setOnClickListener {
            retrieveConsentText()
        }
        binding.provideConsentButton.setOnClickListener {
            provideConsent()
        }
        binding.withdrawConsentButton.setOnClickListener {
            withdrawConsent()
        }

        // Check consent requirement status when the view is created
        checkConsentRequirement()

        // Initialize the UI with current consent status
        updateConsentStatus()
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    /** Checks if consent is required for identity verification in the current environment */
    private fun checkConsentRequirement() {
        launch {
            try {
                showLoading(R.string.checking_consent_requirement)
                val isRequired =
                    withContext(Dispatchers.IO) {
                        app.identityVerificationClient.isConsentRequiredForVerification()
                    }
                hideLoading()

                val statusText =
                    if (isRequired) {
                        getString(R.string.consent_required)
                    } else {
                        getString(R.string.consent_not_required)
                    }

                binding.consentRequirementText.text = statusText
            } catch (e: SudoIdentityVerificationException) {
                hideLoading()
                binding.consentRequirementText.text = getString(R.string.consent_requirement_error)
                showAlertDialog(
                    titleResId = R.string.consent_requirement_error,
                    message = e.localizedMessage ?: getString(R.string.consent_requirement_unknown_error),
                    negativeButtonResId = android.R.string.cancel,
                )
            }
        }
    }

    /** Retrieves and displays the identity data processing consent text */
    private fun retrieveConsentText() {
        launch {
            try {
                showLoading(R.string.retrieving_consent_text)
                val consentContent =
                    withContext(Dispatchers.IO) {
                        app.identityVerificationClient.getIdentityDataProcessingConsentContent(
                            IdentityDataProcessingConsentContentInput(preferredContentType = "text/plain", preferredLanguage = "en-AU"),
                        )
                    }
                hideLoading()

                // Display the consent text in the scrollable text view
                val consentText = consentContent.content
                binding.consentTextContent.text = consentText
                binding.consentTextScrollView.visibility = View.VISIBLE

                // Store the retrieved consent content for use when providing/withdrawing consent
                retrievedConsentContent = consentContent

                // Show both consent action buttons
                binding.provideConsentButton.visibility = View.VISIBLE
                binding.withdrawConsentButton.visibility = View.VISIBLE
            } catch (e: SudoIdentityVerificationException) {
                hideLoading()
                showAlertDialog(
                    titleResId = R.string.consent_text_error,
                    message = e.localizedMessage ?: getString(R.string.consent_requirement_unknown_error),
                    negativeButtonResId = android.R.string.ok,
                )
            } catch (e: Exception) {
                hideLoading()
                showAlertDialog(
                    titleResId = R.string.consent_text_error,
                    getString(R.string.consent_text_api_error, e.message),
                    negativeButtonResId = android.R.string.ok,
                )
            }
        }
    }

    /** Provides consent using the previously retrieved consent content */
    private fun provideConsent() {
        val consentContent = retrievedConsentContent
        if (consentContent == null) {
            showAlertDialog(
                titleResId = R.string.consent_provide_error,
                message = getString(R.string.consent_retrieve_first_error),
                negativeButtonResId = android.R.string.ok,
            )
            return
        }

        launch {
            try {
                showLoading(R.string.providing_consent)
                withContext(Dispatchers.IO) {
                    app.identityVerificationClient.provideIdentityDataProcessingConsent(
                        input =
                            IdentityDataProcessingConsentInput(
                                content = consentContent.content,
                                contentType = consentContent.contentType,
                                language = consentContent.language,
                            ),
                    )
                }
                hideLoading()

                // Show success message
                showAlertDialog(
                    titleResId = R.string.consent_provided_success,
                    message = getString(R.string.consent_provided_success_message),
                    negativeButtonResId = android.R.string.ok,
                )

                // Update the UI to reflect the new consent status
                updateConsentStatus()
            } catch (e: SudoIdentityVerificationException) {
                hideLoading()
                showAlertDialog(
                    titleResId = R.string.consent_provide_error,
                    message = e.localizedMessage ?: getString(R.string.consent_provide_unknown_error),
                    negativeButtonResId = android.R.string.ok,
                )
            } catch (e: Exception) {
                hideLoading()
                showAlertDialog(
                    titleResId = R.string.consent_provide_error,
                    message = getString(R.string.consent_provide_api_error, e.message),
                    negativeButtonResId = android.R.string.ok,
                )
            }
        }
    }

    /** Withdraws consent using the previously retrieved consent content */
    private fun withdrawConsent() {
        launch {
            try {
                showLoading(R.string.withdrawing_consent)
                withContext(Dispatchers.IO) {
                    app.identityVerificationClient.withdrawIdentityDataProcessingConsent()
                }
                hideLoading()

                // Show success message
                showAlertDialog(
                    titleResId = R.string.consent_withdrawn_success,
                    message = getString(R.string.consent_withdrawn_success_message),
                    negativeButtonResId = android.R.string.ok,
                )

                // Update the UI to reflect the new consent status
                updateConsentStatus()
            } catch (e: SudoIdentityVerificationException) {
                hideLoading()
                showAlertDialog(
                    titleResId = R.string.consent_withdraw_error,
                    message = e.localizedMessage ?: getString(R.string.consent_withdraw_unknown_error),
                    negativeButtonResId = android.R.string.ok,
                )
            } catch (e: Exception) {
                hideLoading()
                showAlertDialog(
                    titleResId = R.string.consent_withdraw_error,
                    message = getString(R.string.consent_withdraw_api_error, e.message),
                    negativeButtonResId = android.R.string.ok,
                )
            }
        }
    }

    /** Navigates to identity verification consent documentation when the "Learn More" button is pressed. */
    private fun learnMore() {
        val openUrl = Intent(Intent.ACTION_VIEW)
        openUrl.data = Uri.parse(getString(R.string.identity_verification_doc_url))
        startActivity(openUrl)
    }

    /** Updates the UI to show current consent status */
    private fun updateConsentStatus() {
        launch {
            try {
                val consentStatus =
                    withContext(Dispatchers.IO) {
                        app.identityVerificationClient.getIdentityDataProcessingConsentStatus()
                    }

                // Display the consent status
                val statusText =
                    if (consentStatus.consented) {
                        getString(R.string.consent_granted)
                    } else {
                        getString(R.string.consent_not_granted)
                    }

                binding.consentStatusText.text = statusText
            } catch (e: SudoIdentityVerificationException) {
                binding.consentStatusText.text = getString(R.string.consent_status_error)
            } catch (e: Exception) {
                binding.consentStatusText.text = getString(R.string.consent_status_error)
            }
        }
    }

    /**
     * Sets toolbar items and buttons to enabled/disabled.
     *
     * @param isEnabled [Boolean] If true, toolbar items and buttons will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        toolbarMenu.getItem(0)?.isEnabled = isEnabled

        binding.retrieveConsentTextButton.isEnabled = isEnabled
        binding.provideConsentButton.isEnabled = isEnabled
        binding.withdrawConsentButton.isEnabled = isEnabled
    }

    /**
     * Displays the loading [AlertDialog] indicating that an operation is occurring.
     *
     * @param textResId [StringRes] Resource ID of the text to display in the loading dialog.
     */
    private fun showLoading(
        @StringRes textResId: Int,
    ) {
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
