/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaddresses

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.emailexample.App
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.createLoadingAlertDialog
import com.sudoplatform.emailexample.databinding.FragmentEmailAddressesBinding
import com.sudoplatform.emailexample.emailmasks.EmailMaskAdapter
import com.sudoplatform.emailexample.emailmessages.EmailMessagesFragment
import com.sudoplatform.emailexample.showAlertDialog
import com.sudoplatform.emailexample.sudos.CreateSudoFragment
import com.sudoplatform.emailexample.sudos.SudosFragment
import com.sudoplatform.emailexample.swipe.SwipeLeftActionHelper
import com.sudoplatform.emailexample.util.ObjectDelegate
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudoemail.types.ConfigurationData
import com.sudoplatform.sudoemail.types.EmailAddress
import com.sudoplatform.sudoemail.types.EmailMask
import com.sudoplatform.sudoemail.types.EmailMaskRealAddressType
import com.sudoplatform.sudoemail.types.EmailMaskStatus
import com.sudoplatform.sudoemail.types.ListAPIResult
import com.sudoplatform.sudoemail.types.VerifyExternalEmailAddressResult
import com.sudoplatform.sudoemail.types.inputs.DeprovisionEmailMaskInput
import com.sudoplatform.sudoemail.types.inputs.DisableEmailMaskInput
import com.sudoplatform.sudoemail.types.inputs.EnableEmailMaskInput
import com.sudoplatform.sudoemail.types.inputs.ListEmailAddressesForSudoIdInput
import com.sudoplatform.sudoemail.types.inputs.ListEmailMasksForOwnerInput
import com.sudoplatform.sudoemail.types.inputs.UpdateEmailMaskInput
import com.sudoplatform.sudoemail.types.inputs.VerifyExternalEmailAddressInput
import com.sudoplatform.sudoprofiles.Sudo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.CoroutineContext

/**
 * This [EmailAddressesFragment] presents a list of [EmailAddress]es and [EmailMask]s.
 *
 * - Links From:
 *  - [CreateSudoFragment]: A user chooses the "Create" option from the top right corner of the toolbar.
 *  - [SudosFragment]: A user selects a [Sudo] from the list which will show this view with the list
 *   of [EmailAddress]es created against this [Sudo].
 *
 * - Links To:
 *  - [ProvisionEmailAddressFragment]: If a user taps the "Create Email Address" button, the
 *   [ProvisionEmailAddressFragment] will be presented so that the user can add a new [EmailAddress]
 *   to their [Sudo].
 *  - [ProvisionEmailMaskFragment]: If a user taps the "Create Email Mask" button.
 *  - [EmailMessagesFragment]: If a user taps an [EmailAddress] from the list.
 */
class EmailAddressesFragment :
    Fragment(),
    CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentEmailAddressesBinding>()
    private val binding by bindingDelegate

    /** A reference to the [RecyclerView.Adapter] handling [EmailAddress] data. */
    private lateinit var adapter: EmailAddressAdapter

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** A mutable list of [EmailAddress]es. */
    private var emailAddressList = mutableListOf<EmailAddress>()

    /** Cached configuration data from the SDK. */
    private var configurationData: ConfigurationData? = null

    /** The full unfiltered list of [EmailMask]s from the SDK. */
    private var allEmailMaskList = mutableListOf<EmailMask>()

    /** The currently displayed (filtered) list of [EmailMask]s. */
    private var filteredEmailMaskList = mutableListOf<EmailMask>()

    /** A reference to the [RecyclerView.Adapter] handling [EmailMask] data. */
    private lateinit var maskAdapter: EmailMaskAdapter

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: EmailAddressesFragmentArgs by navArgs()

    /** A [Sudo] used to filter [EmailAddress]s. */
    private lateinit var sudo: Sudo

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingDelegate.attach(FragmentEmailAddressesBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.email_addresses)
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
        configureRecyclerView()
        configureMasksRecyclerView()
        navController = Navigation.findNavController(view)

        binding.createEmailAddressButton.setOnClickListener {
            navController.navigate(
                EmailAddressesFragmentDirections
                    .actionEmailAddressesFragmentToProvisionEmailAddressFragment(
                        sudo,
                    ),
            )
        }

        listEmailAddresses()
        loadConfigurationData()
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    /**
     * Load configuration data from the [SudoEmailClient] to determine feature availability.
     * Gates the email masks UI based on [ConfigurationData.emailMasksEnabled] and
     * [ConfigurationData.externalEmailMasksEnabled].
     */
    private fun loadConfigurationData() {
        launch {
            try {
                showLoading(R.string.loading_configuration)
                val config =
                    withContext(Dispatchers.IO) {
                        app.sudoEmailClient.getConfigurationData()
                    }
                configurationData = config
                if (bindingDelegate.isAttached()) {
                    if (config.emailMasksEnabled) {
                        // Show masks section
                        binding.masksRecyclerView.visibility = View.VISIBLE
                        binding.createEmailMaskButton.visibility = View.VISIBLE
                        binding.masksDisabledText.visibility = View.GONE

                        // Show/hide switch based on externalEmailMasksEnabled
                        if (config.externalEmailMasksEnabled) {
                            binding.internalExternalSwitch.visibility = View.VISIBLE
                            configureSwitch()
                        } else {
                            binding.internalExternalSwitch.visibility = View.GONE
                        }

                        // Now load the masks
                        listEmailMasks()
                    } else {
                        // Hide mask list, create button, and toggle
                        binding.masksRecyclerView.visibility = View.GONE
                        binding.createEmailMaskButton.visibility = View.GONE
                        binding.internalExternalSwitch.visibility = View.GONE
                        binding.emptyMasksStateText.visibility = View.GONE

                        // Show disabled message
                        binding.masksDisabledText.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                if (bindingDelegate.isAttached()) {
                    showAlertDialog(
                        titleResId = R.string.configuration_load_failure,
                        message = e.localizedMessage ?: "$e",
                        positiveButtonResId = R.string.try_again,
                        onPositive = { loadConfigurationData() },
                        negativeButtonResId = android.R.string.cancel,
                    )
                }
            }
            if (bindingDelegate.isAttached()) {
                hideLoading()
            }
        }
    }

    /**
     * List [EmailAddress]es from the [SudoEmailClient].
     */
    private fun listEmailAddresses() {
        launch {
            try {
                showLoading()
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
                        adapter.notifyDataSetChanged()
                    }
                    is ListAPIResult.Partial -> {
                        val cause =
                            emailAddresses.result.failed
                                .first()
                                .cause
                        showAlertDialog(
                            titleResId = R.string.list_email_addresses_failure,
                            message = cause.localizedMessage ?: "$cause",
                            positiveButtonResId = R.string.try_again,
                            onPositive = { listEmailAddresses() },
                            negativeButtonResId = android.R.string.cancel,
                        )
                    }
                }
            } catch (e: SudoEmailClient.EmailAddressException) {
                showAlertDialog(
                    titleResId = R.string.list_email_addresses_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { listEmailAddresses() },
                    negativeButtonResId = android.R.string.cancel,
                )
            }
            hideLoading()
        }
    }

    /**
     * List [EmailMask]s from the [SudoEmailClient].
     */
    private fun listEmailMasks() {
        launch {
            try {
                val emailMasks =
                    withContext(Dispatchers.IO) {
                        app.sudoEmailClient.listEmailMasksForOwner(ListEmailMasksForOwnerInput())
                    }
                if (bindingDelegate.isAttached()) {
                    when (emailMasks) {
                        is ListAPIResult.Success -> {
                            allEmailMaskList.clear()
                            allEmailMaskList.addAll(emailMasks.result.items)
                            filterMasksByType(binding.internalExternalSwitch.isChecked)
                        }
                        is ListAPIResult.Partial -> {
                            val cause =
                                emailMasks.result.failed
                                    .first()
                                    .cause
                            showAlertDialog(
                                titleResId = R.string.list_email_masks_failure,
                                message = cause.localizedMessage ?: "$cause",
                                positiveButtonResId = R.string.try_again,
                                onPositive = { listEmailMasks() },
                                negativeButtonResId = android.R.string.cancel,
                            )
                        }
                    }
                }
            } catch (e: SudoEmailClient.EmailAddressException) {
                if (bindingDelegate.isAttached()) {
                    showAlertDialog(
                        titleResId = R.string.list_email_masks_failure,
                        message = e.localizedMessage ?: "$e",
                        positiveButtonResId = R.string.try_again,
                        onPositive = { listEmailMasks() },
                        negativeButtonResId = android.R.string.cancel,
                    )
                }
            }
        }
    }

    /**
     * Deprovision a selected [EmailAddress] from the [SudoEmailClient].
     *
     * @param emailAddressId [String] The identifier of the [EmailAddress] to de-provision.
     */
    private fun deprovisionEmailAddress(emailAddressId: String) {
        launch {
            try {
                showDeleteAlert(R.string.deleting_email_address)
                withContext(Dispatchers.IO) {
                    app.sudoEmailClient.deprovisionEmailAddress(emailAddressId)
                }
            } catch (e: SudoEmailClient.EmailAddressException) {
                showAlertDialog(
                    titleResId = R.string.deleting_email_address_failure,
                    message = e.localizedMessage ?: e.toString(),
                    negativeButtonResId = android.R.string.cancel,
                )
            }
            hideDeleteAlert()
        }
    }

    /**
     * Configures the [RecyclerView] used to display the listed [EmailAddress] items and listens to
     * item select events to navigate to the [EmailMessagesFragment].
     */
    private fun configureRecyclerView() {
        adapter =
            EmailAddressAdapter(emailAddressList) { emailAddress ->
                navController.navigate(
                    EmailAddressesFragmentDirections
                        .actionEmailAddressesFragmentToEmailMessagesFragment(
                            emailAddress = emailAddress.emailAddress,
                            emailDisplayName = emailAddress.alias ?: "",
                            emailAddressId = emailAddress.id,
                        ),
                )
            }

        binding.emailAddressRecyclerView.adapter = adapter
        binding.emailAddressRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        configureSwipeToDelete()
    }

    /**
     * Configures the [RecyclerView] used to display the listed [EmailMask] items with full
     * detail (status, real address, metadata, expiration) and tap-to-action support.
     */
    private fun configureMasksRecyclerView() {
        maskAdapter =
            EmailMaskAdapter(
                filteredEmailMaskList,
                ::showActionDialog,
                onVerifyClick = ::initiateVerification,
                showVerifyButton = ::showVerifyButton,
            )
        binding.masksRecyclerView.adapter = maskAdapter
        binding.masksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        configureMaskSwipeToDelete()
    }

    /**
     * Configures the [Internal_External_Switch] listener to filter masks by type
     * and update the create button accordingly.
     */
    private fun configureSwitch() {
        binding.internalExternalSwitch.setOnCheckedChangeListener { _, isChecked ->
            filterMasksByType(isChecked)
            updateCreateButtonForSwitchState(isChecked)
        }
        // Set initial state (switch defaults to OFF = internal)
        updateCreateButtonForSwitchState(binding.internalExternalSwitch.isChecked)
    }

    /**
     * Filters [allEmailMaskList] into [filteredEmailMaskList] based on the switch state.
     *
     * @param showExternal [Boolean] If true, show only EXTERNAL masks. If false, show INTERNAL and UNKNOWN masks.
     */
    private fun filterMasksByType(showExternal: Boolean) {
        filteredEmailMaskList.clear()
        if (showExternal) {
            filteredEmailMaskList.addAll(
                allEmailMaskList.filter { it.realAddressType == EmailMaskRealAddressType.EXTERNAL },
            )
        } else {
            filteredEmailMaskList.addAll(
                allEmailMaskList.filter {
                    it.realAddressType == EmailMaskRealAddressType.INTERNAL ||
                        it.realAddressType == EmailMaskRealAddressType.UNKNOWN
                },
            )
        }
        maskAdapter.notifyDataSetChanged()
        updateEmptyStateVisibility(showExternal)
    }

    /**
     * Updates the empty state message visibility and text based on the current filter.
     *
     * @param showExternal [Boolean] If true, show external empty message. If false, show internal empty message.
     */
    private fun updateEmptyStateVisibility(showExternal: Boolean) {
        if (filteredEmailMaskList.isEmpty()) {
            binding.emptyMasksStateText.text =
                if (showExternal) {
                    getString(R.string.no_external_masks)
                } else {
                    getString(R.string.no_internal_masks)
                }
            binding.emptyMasksStateText.visibility = View.VISIBLE
        } else {
            binding.emptyMasksStateText.visibility = View.GONE
        }
    }

    /**
     * Updates the create button text and click target based on the switch state.
     *
     * @param isExternal [Boolean] If true, button creates external mask. If false, button creates internal mask.
     */
    private fun updateCreateButtonForSwitchState(isExternal: Boolean) {
        if (isExternal) {
            binding.createEmailMaskButton.text = getString(R.string.create_external_email_mask_button)
            binding.createEmailMaskButton.setOnClickListener {
                navController.navigate(
                    EmailAddressesFragmentDirections
                        .actionEmailAddressesFragmentToProvisionExternalEmailMaskFragment(
                            sudo,
                        ),
                )
            }
        } else {
            binding.createEmailMaskButton.text = getString(R.string.create_email_mask_button)
            binding.createEmailMaskButton.setOnClickListener {
                navController.navigate(
                    EmailAddressesFragmentDirections
                        .actionEmailAddressesFragmentToProvisionEmailMaskFragment(
                            sudo,
                        ),
                )
            }
        }
    }

    /**
     * Determines whether the verify button should be shown for the given [EmailMask].
     *
     * The verify button is visible only when:
     * - The internal/external switch is in the external (enabled) position
     * - The mask's [EmailMaskRealAddressType] is EXTERNAL
     * - The mask's status is not ENABLED (i.e., verification has not been completed)
     *
     * @param emailMask [EmailMask] The email mask to check.
     * @return [Boolean] True if the verify button should be shown, false otherwise.
     */
    internal fun showVerifyButton(emailMask: EmailMask): Boolean {
        val switchIsExternal = binding.internalExternalSwitch.isChecked
        return switchIsExternal &&
            emailMask.realAddressType == EmailMaskRealAddressType.EXTERNAL &&
            emailMask.status == EmailMaskStatus.PENDING
    }

    /**
     * Re-enables the verify button for the given [EmailMask] in the RecyclerView.
     *
     * Called when the verification flow is cancelled or fails, so the user can
     * tap "Verify" again to retry.
     *
     * @param emailMask [EmailMask] The email mask whose verify button should be re-enabled.
     */
    private fun reEnableVerifyButton(emailMask: EmailMask) {
        val position = filteredEmailMaskList.indexOf(emailMask)
        if (position >= 0) {
            val viewHolder = binding.masksRecyclerView.findViewHolderForAdapterPosition(position)
            if (viewHolder is com.sudoplatform.emailexample.emailmasks.EmailMaskViewHolder) {
                viewHolder.itemView.findViewById<android.widget.Button>(R.id.verifyButton)?.isEnabled = true
            }
        }
    }

    /**
     * Initiates the verification process for an external [EmailMask] by calling
     * [SudoEmailClient.verifyExternalEmailAddress] without a verification code.
     *
     * Shows a loading indicator on the tapped item and disables the verify button during
     * the initiation call. On success, shows the verification code dialog. On failure,
     * shows an error dialog with "Try Again" and "Cancel" options.
     *
     * @param emailMask [EmailMask] The external email mask to verify.
     */
    private fun initiateVerification(emailMask: EmailMask) {
        // Find the position of the mask in the filtered list to update the ViewHolder
        val position = filteredEmailMaskList.indexOf(emailMask)
        if (position >= 0) {
            val viewHolder = binding.masksRecyclerView.findViewHolderForAdapterPosition(position)
            if (viewHolder is com.sudoplatform.emailexample.emailmasks.EmailMaskViewHolder) {
                viewHolder.itemView.findViewById<android.widget.Button>(R.id.verifyButton)?.isEnabled = false
            }
        }

        launch {
            try {
                showDeleteAlert(R.string.verifying_email)
                withContext(Dispatchers.IO) {
                    app.sudoEmailClient.verifyExternalEmailAddress(
                        VerifyExternalEmailAddressInput(
                            emailAddress = emailMask.realAddress,
                            emailMaskId = emailMask.id,
                        ),
                    )
                }
                hideDeleteAlert()
                if (bindingDelegate.isAttached()) {
                    showVerificationCodeDialog(emailMask)
                }
            } catch (e: Exception) {
                hideDeleteAlert()
                // Re-enable the verify button on failure
                reEnableVerifyButton(emailMask)
                if (bindingDelegate.isAttached()) {
                    showAlertDialog(
                        titleResId = R.string.verification_initiation_failure,
                        message = e.localizedMessage ?: e.toString(),
                        positiveButtonResId = R.string.try_again,
                        onPositive = { initiateVerification(emailMask) },
                        negativeButtonResId = android.R.string.cancel,
                    )
                }
            }
        }
    }

    /**
     * Shows the verification code input dialog for the given [EmailMask].
     *
     * Displays an [AlertDialog] with a text input field (max 10 characters) prompting the user
     * to enter the verification code received at their external email address. If the user
     * confirms with an empty input, the dialog remains open with a validation message. If the
     * user enters a non-empty code and confirms, [completeVerification] is called.
     *
     * @param emailMask [EmailMask] The email mask being verified.
     */
    private fun showVerificationCodeDialog(emailMask: EmailMask) {
        val context = requireContext()
        val padding = (16 * resources.displayMetrics.density).toInt()

        val layout =
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(padding, padding, padding, padding)
            }

        val codeEditText =
            EditText(context).apply {
                hint = getString(R.string.verification_code_prompt)
                inputType = InputType.TYPE_CLASS_TEXT
                filters = arrayOf(android.text.InputFilter.LengthFilter(10))
            }
        layout.addView(codeEditText)

        val validationMessage =
            TextView(context).apply {
                text = getString(R.string.verification_code_required)
                setTextColor(android.graphics.Color.RED)
                visibility = View.GONE
            }
        layout.addView(validationMessage)

        val dialog =
            AlertDialog
                .Builder(context)
                .setTitle(R.string.verification_code_prompt)
                .setView(layout)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel) { d, _ ->
                    d.dismiss()
                    reEnableVerifyButton(emailMask)
                }.create()

        dialog.setOnShowListener {
            val confirmButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            confirmButton.setOnClickListener {
                val code = codeEditText.text.toString().trim()
                if (code.isEmpty()) {
                    validationMessage.visibility = View.VISIBLE
                } else {
                    dialog.dismiss()
                    completeVerification(emailMask, code)
                }
            }
        }

        dialog.show()
    }

    /**
     * Completes the verification process for an external [EmailMask] by calling
     * [SudoEmailClient.verifyExternalEmailAddress] with the verification code.
     *
     * On success: dismisses any loading dialog, shows a Toast success message, and refreshes
     * the mask list. On failure: shows an error dialog with "Try Again" (re-opens the code
     * dialog) and "Cancel" options.
     *
     * @param emailMask [EmailMask] The external email mask being verified.
     * @param code [String] The verification code entered by the user.
     */
    private fun completeVerification(
        emailMask: EmailMask,
        code: String,
    ) {
        launch {
            try {
                showDeleteAlert(R.string.verifying_email)
                val result =
                    withContext(Dispatchers.IO) {
                        app.sudoEmailClient.verifyExternalEmailAddress(
                            VerifyExternalEmailAddressInput(
                                emailAddress = emailMask.realAddress,
                                emailMaskId = emailMask.id,
                                verificationCode = code,
                            ),
                        )
                    }
                hideDeleteAlert()
                if (bindingDelegate.isAttached()) {
                    if (result.isVerified) {
                        Toast
                            .makeText(
                                requireContext(),
                                R.string.verification_success,
                                Toast.LENGTH_SHORT,
                            ).show()
                        listEmailMasks()
                    } else {
                        reEnableVerifyButton(emailMask)
                        val reason =
                            result.reason
                                ?: getString(R.string.verification_completion_failure)
                        showAlertDialog(
                            titleResId = R.string.verification_completion_failure,
                            message = reason,
                            positiveButtonResId = R.string.try_again,
                            onPositive = { showVerificationCodeDialog(emailMask) },
                            negativeButtonResId = android.R.string.cancel,
                        )
                    }
                }
            } catch (e: Exception) {
                hideDeleteAlert()
                reEnableVerifyButton(emailMask)
                if (bindingDelegate.isAttached()) {
                    showAlertDialog(
                        titleResId = R.string.verification_completion_failure,
                        message = e.localizedMessage ?: e.toString(),
                        positiveButtonResId = R.string.try_again,
                        onPositive = { showVerificationCodeDialog(emailMask) },
                        negativeButtonResId = android.R.string.cancel,
                    )
                }
            }
        }
    }

    /**
     * Shows an action dialog for the given [EmailMask] with status-based options.
     *
     * @param emailMask [EmailMask] The email mask to show actions for.
     */
    private fun showActionDialog(emailMask: EmailMask) {
        val builder =
            AlertDialog
                .Builder(requireContext())
                .setTitle(emailMask.maskAddress)
                .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }

        when (emailMask.status) {
            EmailMaskStatus.ENABLED -> {
                builder.setPositiveButton(R.string.disable) { dialog, _ ->
                    dialog.dismiss()
                    disableEmailMask(emailMask.id)
                }
            }
            EmailMaskStatus.DISABLED -> {
                builder.setPositiveButton(R.string.enable) { dialog, _ ->
                    dialog.dismiss()
                    enableEmailMask(emailMask.id)
                }
            }
            EmailMaskStatus.LOCKED,
            EmailMaskStatus.PENDING,
            EmailMaskStatus.UNKNOWN,
            -> {
                // No additional positive button for these statuses
            }
        }

        builder.setNeutralButton(R.string.update) { dialog, _ ->
            dialog.dismiss()
            showUpdateDialog(emailMask)
        }

        builder.show()
    }

    /**
     * Shows the update dialog for the given [EmailMask].
     * Presents a form with a "Metadata:" label followed by a pre-filled EditText,
     * an "Expires:" label followed by a button showing the current date or "Pick Date",
     * and Save/Cancel buttons.
     *
     * @param emailMask [EmailMask] The email mask to update.
     */
    private fun showUpdateDialog(emailMask: EmailMask) {
        val context = requireContext()
        val padding = (16 * resources.displayMetrics.density).toInt()
        val smallPadding = (8 * resources.displayMetrics.density).toInt()
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())

        // Build the layout programmatically
        val layout =
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(padding, padding, padding, padding)
            }

        // "Metadata:" label
        val metadataLabel =
            TextView(context).apply {
                text = getString(R.string.metadata_json_label)
                textSize = 14f
            }
        layout.addView(metadataLabel)

        // Metadata JSON EditText - pre-filled with existing value or empty
        val metadataEditText =
            EditText(context).apply {
                hint = getString(R.string.metadata_json_hint)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                minLines = 3
                if (!emailMask.metadata.isNullOrEmpty()) {
                    setText(JSONObject(emailMask.metadata!!).toString())
                }
            }
        layout.addView(metadataEditText)

        // "Expires:" label
        val expiresLabel =
            TextView(context).apply {
                text = getString(R.string.set_expiration)
                textSize = 14f
                setPadding(0, padding, 0, smallPadding)
            }
        layout.addView(expiresLabel)

        // Horizontal layout for date button and clear button
        val dateRow =
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
            }

        // Date button - shows current date or "Pick Date"
        var selectedDate: Date? = emailMask.expiresAt?.takeIf { it.time != 0L }
        val dateButton =
            Button(context).apply {
                text =
                    if (selectedDate != null) {
                        dateFormat.format(selectedDate!!)
                    } else {
                        getString(R.string.pick_date)
                    }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
        dateButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            if (selectedDate != null) {
                calendar.time = selectedDate!!
            }
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val selectedCalendar =
                        Calendar.getInstance().apply {
                            set(year, month, dayOfMonth)
                        }
                    selectedDate = selectedCalendar.time
                    dateButton.text = dateFormat.format(selectedCalendar.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
            ).show()
        }
        dateRow.addView(dateButton)

        // Clear button - resets the expiration date
        val clearButton =
            Button(context).apply {
                text = getString(R.string.clear_date)
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
            }
        clearButton.setOnClickListener {
            selectedDate = null
            dateButton.text = getString(R.string.pick_date)
        }
        dateRow.addView(clearButton)

        layout.addView(dateRow)

        // Build and show the AlertDialog
        val dialog =
            AlertDialog
                .Builder(context)
                .setTitle(emailMask.maskAddress)
                .setView(layout)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(android.R.string.cancel) { d, _ ->
                    d.dismiss()
                }.create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val metadataText = metadataEditText.text.toString().trim()
                val metadata: Map<String, String>? =
                    if (metadataText.isNotEmpty()) {
                        val parsed = parseMetadataJson(metadataText)
                        if (parsed == null) {
                            Toast.makeText(requireContext(), R.string.invalid_json_error, Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        parsed
                    } else {
                        emptyMap()
                    }

                val expiresAt: Date? = selectedDate ?: Date(0)

                dialog.dismiss()
                updateEmailMask(emailMask.id, metadata, expiresAt)
            }
        }
        dialog.show()
    }

    /**
     * Enable a disabled [EmailMask] via the [SudoEmailClient].
     *
     * @param maskId [String] The identifier of the [EmailMask] to enable.
     */
    private fun enableEmailMask(maskId: String) {
        launch {
            try {
                showDeleteAlert(R.string.enabling_email_mask)
                withContext(Dispatchers.IO) {
                    app.sudoEmailClient.enableEmailMask(
                        EnableEmailMaskInput(emailMaskId = maskId),
                    )
                }
                hideDeleteAlert()
                listEmailMasks()
            } catch (e: Exception) {
                hideDeleteAlert()
                showAlertDialog(
                    titleResId = R.string.enable_email_mask_failure,
                    message = e.localizedMessage ?: e.toString(),
                    positiveButtonResId = R.string.try_again,
                    onPositive = { enableEmailMask(maskId) },
                    negativeButtonResId = android.R.string.cancel,
                )
            }
        }
    }

    /**
     * Disable an enabled [EmailMask] via the [SudoEmailClient].
     *
     * @param maskId [String] The identifier of the [EmailMask] to disable.
     */
    private fun disableEmailMask(maskId: String) {
        launch {
            try {
                showDeleteAlert(R.string.disabling_email_mask)
                withContext(Dispatchers.IO) {
                    app.sudoEmailClient.disableEmailMask(
                        DisableEmailMaskInput(emailMaskId = maskId),
                    )
                }
                hideDeleteAlert()
                listEmailMasks()
            } catch (e: Exception) {
                hideDeleteAlert()
                showAlertDialog(
                    titleResId = R.string.disable_email_mask_failure,
                    message = e.localizedMessage ?: e.toString(),
                    positiveButtonResId = R.string.try_again,
                    onPositive = { disableEmailMask(maskId) },
                    negativeButtonResId = android.R.string.cancel,
                )
            }
        }
    }

    /**
     * Update an [EmailMask] via the [SudoEmailClient].
     *
     * @param maskId [String] The identifier of the [EmailMask] to update.
     * @param metadata [Map]? The metadata map to set, or null to leave unchanged.
     * @param expiresAt [Date]? The expiration date to set, or null to leave unchanged.
     */
    private fun updateEmailMask(
        maskId: String,
        metadata: Map<String, String>?,
        expiresAt: Date?,
    ) {
        launch {
            try {
                showDeleteAlert(R.string.updating_email_mask)
                withContext(Dispatchers.IO) {
                    app.sudoEmailClient.updateEmailMask(
                        UpdateEmailMaskInput(
                            emailMaskId = maskId,
                            metadata = metadata,
                            expiresAt = expiresAt,
                        ),
                    )
                }
                hideDeleteAlert()
                listEmailMasks()
            } catch (e: Exception) {
                hideDeleteAlert()
                showAlertDialog(
                    titleResId = R.string.update_email_mask_failure,
                    message = e.localizedMessage ?: e.toString(),
                    positiveButtonResId = R.string.try_again,
                    onPositive = { updateEmailMask(maskId, metadata, expiresAt) },
                    negativeButtonResId = android.R.string.cancel,
                )
            }
        }
    }

    /**
     * Deprovision a selected [EmailMask] from the [SudoEmailClient].
     *
     * @param maskId [String] The identifier of the [EmailMask] to deprovision.
     * @param position [Int] The adapter position of the item being deprovisioned.
     */
    private fun deprovisionEmailMask(
        maskId: String,
        position: Int,
    ) {
        launch {
            try {
                showDeleteAlert(R.string.deleting_email_mask)
                withContext(Dispatchers.IO) {
                    app.sudoEmailClient.deprovisionEmailMask(
                        DeprovisionEmailMaskInput(emailMaskId = maskId),
                    )
                }
                hideDeleteAlert()
                // Remove from both lists
                allEmailMaskList.removeAll { it.id == maskId }
                filteredEmailMaskList.removeAt(position)
                maskAdapter.notifyItemRemoved(position)
                if (filteredEmailMaskList.isEmpty()) {
                    updateEmptyStateVisibility(binding.internalExternalSwitch.isChecked)
                }
            } catch (e: Exception) {
                hideDeleteAlert()
                maskAdapter.notifyItemChanged(position)
                showAlertDialog(
                    titleResId = R.string.delete_email_mask_failure,
                    message = e.localizedMessage ?: e.toString(),
                    positiveButtonResId = R.string.try_again,
                    onPositive = { deprovisionEmailMask(maskId, position) },
                    negativeButtonResId = android.R.string.cancel,
                )
            }
        }
    }

    /**
     * Parses a JSON string into a [Map] of [String] to [String].
     *
     * Uses [JSONObject] to parse the string, iterates keys and calls `getString(key)` for each.
     * Returns the resulting map, or `null` if any exception occurs (invalid JSON or non-string values).
     *
     * @param jsonString [String] The JSON string to parse.
     * @return A [Map] of key-value pairs, or `null` if parsing fails.
     */
    internal fun parseMetadataJson(jsonString: String): Map<String, String>? =
        try {
            val jsonObject = JSONObject(jsonString)
            val map = mutableMapOf<String, String>()
            jsonObject.keys().forEach { key ->
                map[key] = jsonObject.getString(key)
            }
            map
        } catch (e: Exception) {
            null
        }

    /**
     * Sets buttons and recycler view to enabled/disabled.
     *
     * @param isEnabled [Boolean] If true, buttons and recycler view will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        binding.createEmailAddressButton.isEnabled = isEnabled
        binding.createEmailMaskButton.isEnabled = isEnabled
        binding.emailAddressRecyclerView.isEnabled = isEnabled
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading(
        @StringRes textResId: Int = 0,
    ) {
        if (textResId != 0) {
            binding.progressText.text = getString(textResId)
        }
        binding.progressBar.visibility = View.VISIBLE
        binding.progressText.visibility = View.VISIBLE
        binding.emailAddressRecyclerView.visibility = View.GONE
        binding.masksRecyclerView.visibility = View.GONE
        setItemsEnabled(false)
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        if (bindingDelegate.isAttached()) {
            binding.progressBar.visibility = View.GONE
            binding.progressText.visibility = View.GONE
            binding.emailAddressRecyclerView.visibility = View.VISIBLE
            binding.masksRecyclerView.visibility = View.VISIBLE
            setItemsEnabled(true)
        }
    }

    /** Displays the loading [AlertDialog] indicating that a deletion operation is occurring. */
    private fun showDeleteAlert(
        @StringRes textResId: Int,
    ) {
        loading = createLoadingAlertDialog(textResId)
        loading?.show()
    }

    /** Dismisses the loading [AlertDialog] indicating that a deletion operation has finished. */
    private fun hideDeleteAlert() {
        loading?.dismiss()
    }

    /**
     * Configures the swipe to delete action by listening to [RecyclerView.ViewHolder] swipe events
     * and drawing the swipe view and delete icon.
     *
     * Swiping in from the left will perform a delete operation and remove the item from the view.
     */
    private fun configureSwipeToDelete() {
        val itemTouchCallback = SwipeLeftActionHelper(requireContext(), onSwipedAction = ::onSwiped)
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.emailAddressRecyclerView)
    }

    private fun onSwiped(
        viewHolder: RecyclerView.ViewHolder,
        direction: Int,
    ) {
        val emailAddress = emailAddressList[viewHolder.adapterPosition]
        deprovisionEmailAddress(emailAddress.id)
        emailAddressList.removeAt(viewHolder.adapterPosition)
        adapter.notifyItemRemoved(viewHolder.adapterPosition)
    }

    /**
     * Configures the swipe to delete action for email masks.
     */
    private fun configureMaskSwipeToDelete() {
        val itemTouchCallback = SwipeLeftActionHelper(requireContext(), onSwipedAction = ::onMaskSwiped)
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.masksRecyclerView)
    }

    private fun onMaskSwiped(
        viewHolder: RecyclerView.ViewHolder,
        direction: Int,
    ) {
        val emailMask = filteredEmailMaskList[viewHolder.adapterPosition]
        deprovisionEmailMask(emailMask.id, viewHolder.adapterPosition)
    }
}
