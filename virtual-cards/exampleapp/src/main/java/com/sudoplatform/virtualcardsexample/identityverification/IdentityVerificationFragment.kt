/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudoidentityverification.QueryOption
import com.sudoplatform.sudoidentityverification.SudoIdentityVerificationClient
import com.sudoplatform.sudoidentityverification.SudoIdentityVerificationException
import com.sudoplatform.virtualcardsexample.App
import com.sudoplatform.virtualcardsexample.R
import com.sudoplatform.virtualcardsexample.createLoadingAlertDialog
import com.sudoplatform.virtualcardsexample.databinding.FragmentIdentityVerificationBinding
import com.sudoplatform.virtualcardsexample.mainmenu.MainMenuFragment
import com.sudoplatform.virtualcardsexample.shared.InputFormAdapter
import com.sudoplatform.virtualcardsexample.shared.InputFormCell
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
 * This [IdentityVerificationFragment] presents a form so that a user can perform Secure
 * ID Verification.
 *
 * - Links From:
 *  - [MainMenuFragment]: A user chooses the "Secure ID Verification" option from the main menu which
 *   will show this view allowing the user to perform Secure ID Verification.
 */
class IdentityVerificationFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentIdentityVerificationBinding>()
    private val binding by bindingDelegate

    /** Toolbar [Menu] displaying title and verify button. */
    private lateinit var toolbarMenu: Menu

    /** A reference to the input form [RecyclerView.Adapter] handling input form data. */
    private lateinit var adapter: InputFormAdapter

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** A list of [InputFormCell]s that corresponds to the cells of the input form. */
    private val inputFormCells = mutableListOf<InputFormCell>()

    /** An array of labels used for each [InputFormCell]. */
    private var labels = emptyArray<String>()

    /** An array of the default text populated for each [InputFormCell]. */
    private val enteredInput =
        arrayOf("John", "Smith", "222333 Peachtree Place", null, "30318", "USA", "1975-02-28")

    /** Types of verification statuses. */
    enum class VerificationStatus(val status: Int) {
        VERIFIED(R.string.verified),
        UNVERIFIED(R.string.not_verified),
        UNKNOWN(R.string.unknown),
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingDelegate.attach(FragmentIdentityVerificationBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.secure_id_verification)
            inflateMenu(R.menu.nav_menu_with_verify_button)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.verify -> {
                        verifyUser()
                    }
                }
                true
            }
            toolbarMenu = menu
        }
        app = requireActivity().application as App
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView()
        configureFormCells()

        binding.learnMoreButton.setOnClickListener {
            learnMore()
        }

        fetchVerificationStatus()
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    /**
     * Validates and verifies a user's identity from the [SudoIdentityVerificationClient] based on
     * the submitted form inputs.
     */
    private fun verifyUser() {
        if (validateFormData()) {
            showAlertDialog(
                titleResId = R.string.validate_fields,
                positiveButtonResId = android.R.string.ok
            )
            return
        }
        showLoading(R.string.verifying_identity)
        var addressLine1 = enteredInput[2]
        val addressLine2 = enteredInput[3]
        if (!addressLine2.isNullOrEmpty()) {
            addressLine1 = "$addressLine1 $addressLine2"
        }
        launch {
            try {
                val verifiedIdentity = withContext(Dispatchers.IO) {
                    app.identityVerificationClient.verifyIdentity(
                        firstName = enteredInput[0] ?: "",
                        lastName = enteredInput[1] ?: "",
                        address = addressLine1 ?: "",
                        city = "",
                        state = "",
                        postalCode = enteredInput[4] ?: "",
                        country = enteredInput[5] ?: "",
                        dateOfBirth = enteredInput[6] ?: ""
                    )
                }
                if (verifiedIdentity.verified) {
                    showAlertDialog(
                        titleResId = R.string.verification_complete,
                        messageResId = R.string.identity_verified,
                        positiveButtonResId = android.R.string.ok
                    )
                    binding.statusLabel.text = getString(VerificationStatus.VERIFIED.status)
                    toolbarMenu.getItem(0)?.isEnabled = false
                } else {
                    showAlertDialog(
                        titleResId = R.string.verification_complete,
                        messageResId = R.string.identity_not_verified,
                        positiveButtonResId = android.R.string.ok
                    )
                    binding.statusLabel.text = getString(VerificationStatus.UNVERIFIED.status)
                }
            } catch (e: SudoIdentityVerificationException) {
                showAlertDialog(
                    titleResId = R.string.verification_failed,
                    message = e.localizedMessage ?: e.toString(),
                    positiveButtonResId = R.string.try_again,
                    onPositive = { verifyUser() },
                    negativeButtonResId = android.R.string.ok
                )
            }
            hideLoading()
        }
    }

    /** Lookup the verification status from the [SudoIdentityVerificationClient] of the registered user. */
    private fun fetchVerificationStatus() {
        showLoading(R.string.checking_status)
        launch {
            try {
                val verifiedIdentity = withContext(Dispatchers.IO) {
                    app.identityVerificationClient.checkIdentityVerification(QueryOption.REMOTE_ONLY)
                }

                if (verifiedIdentity.verified) {
                    binding.statusLabel.text = getString(VerificationStatus.VERIFIED.status)
                    toolbarMenu.getItem(0)?.isEnabled = false
                } else {
                    binding.statusLabel.text = getString(VerificationStatus.UNVERIFIED.status)
                }
            } catch (e: SudoIdentityVerificationException) {
                binding.statusLabel.text = getString(VerificationStatus.UNKNOWN.status)
            }
            hideLoading()
        }
    }

    /**
     * Configures the [RecyclerView] used to display the [InputFormCell]s and listens to input
     * change events to capture user input.
     */
    private fun configureRecyclerView() {
        adapter = InputFormAdapter(inputFormCells) { position, charSeq ->
            enteredInput[position] = charSeq
        }

        binding.formRecyclerView.setHasFixedSize(true)
        binding.formRecyclerView.adapter = adapter
        binding.formRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    /** Configures [InputFormCell] labels, text field hints and placeholder text. */
    private fun configureFormCells() {
        labels = resources.getStringArray(R.array.identity_verification_labels)
        for (i in labels.indices) {
            val hint = if (labels[i].contains(getString(R.string.address_line_2))) getString(
                R.string.enter_optional_input,
                labels[i]
            ) else getString(R.string.enter_non_optional_input, labels[i])
            inputFormCells.add(InputFormCell(labels[i], enteredInput[i] ?: "", hint))
        }
    }

    /** Navigates to a Sudo Platform web page when the "Learn More" button is pressed. */
    private fun learnMore() {
        val openUrl = Intent(Intent.ACTION_VIEW)
        openUrl.data = Uri.parse(getString(R.string.identity_verification_doc_url))
        startActivity(openUrl)
    }

    /** Validates submitted input form data. */
    private fun validateFormData(): Boolean {
        val requiredInput = enteredInput.toMutableList()
        requiredInput.removeAt(3)
        return requiredInput.contains("")
    }

    /** Displays the loading [AlertDialog] indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int) {
        loading = createLoadingAlertDialog(textResId)
        loading?.show()
    }

    /** Dismisses the loading [AlertDialog] indicating that an operation has finished. */
    private fun hideLoading() {
        loading?.dismiss()
    }
}
