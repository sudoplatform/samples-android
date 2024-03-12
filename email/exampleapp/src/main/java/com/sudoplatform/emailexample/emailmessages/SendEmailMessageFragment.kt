/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.sudoplatform.emailexample.App
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.createLoadingAlertDialog
import com.sudoplatform.emailexample.databinding.FragmentSendEmailMessageBinding
import com.sudoplatform.emailexample.showAlertDialog
import com.sudoplatform.emailexample.util.ObjectDelegate
import com.sudoplatform.emailexample.util.Rfc822MessageFactory
import com.sudoplatform.emailexample.util.SimplifiedEmailMessage
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudoemail.types.EmailMessage
import com.sudoplatform.sudoemail.types.inputs.CreateDraftEmailMessageInput
import com.sudoplatform.sudoemail.types.inputs.LookupEmailAddressesPublicInfoInput
import com.sudoplatform.sudoemail.types.inputs.SendEmailMessageInput
import com.sudoplatform.sudoemail.types.inputs.UpdateDraftEmailMessageInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Timer
import java.util.TimerTask
import java.util.regex.Pattern
import kotlin.coroutines.CoroutineContext

/**
 * This [SendEmailMessageFragment] presents a view to allow a user to compose and send an email
 * message.
 *
 * - Links From:
 *  - [EmailMessagesFragment]: A user taps the "Compose" button in the top right corner of the
 *   toolbar.
 *
 * - Links To:
 *  - [EmailMessagesFragment]: If a user successfully sends an email message, they will be returned
 *   to this view.
 */
class SendEmailMessageFragment : Fragment(), CoroutineScope {

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

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentSendEmailMessageBinding>()
    private val binding by bindingDelegate

    /** Toolbar [Menu] displaying title and send button. */
    private lateinit var toolbarMenu: Menu

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: SendEmailMessageFragmentArgs by navArgs()

    /** Email Address used to compose a reply email message. */
    private lateinit var emailAddress: String

    /** Email Address Identifier used to compose a reply email message. */
    private lateinit var emailAddressId: String

    /** Timer to prevent stacking SDK calls for looking up encrypted email addresses. */
    private var encryptedEmailAddressLookupTimer: Timer? = null

    /**
     * Encryption status for each email address ui input.
     * Used to track cases where multiple values need to be verified without stacking requests.
     */
    private val encryptedInputStatuses: MutableMap<String, Boolean?> = mutableMapOf(
        "to" to null,
        "cc" to null,
        "bcc" to null,
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingDelegate.attach(FragmentSendEmailMessageBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.compose_email_message)
            inflateMenu(R.menu.nav_menu_with_send_button)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.send -> {
                        sendEmailMessage()
                    }
                    R.id.save -> {
                        saveDraftEmailMessage()
                    }
                }
                true
            }
            toolbarMenu = menu
        }
        app = requireActivity().application as App
        emailAddress = args.emailAddress
        emailAddressId = args.emailAddressId
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureFieldListener(binding.toTextView, "to")
        configureFieldListener(binding.bccTextView, "cc")
        configureFieldListener(binding.ccTextView, "bcc")
        navController = Navigation.findNavController(view)

        val emailMessageWithBody = args.emailMessageWithBody
        val emailMessage = args.emailMessage
        if (emailMessageWithBody != null && emailMessage != null) {
            if (emailMessageWithBody.isDraft) {
                configureEmailMessageContents(
                    emailMessage,
                    emailMessageWithBody,
                )
            } else {
                configureReplyContents(emailMessage, emailMessageWithBody)
            }
        }
    }

    override fun onDestroy() {
        loading?.dismiss()
        encryptedEmailAddressLookupTimer?.cancel()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    private fun configureFieldListener(textView: TextView, fieldName: String) {
        textView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) { /* no-op */ }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { /* no-op */ }
            var timer = Timer()
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                hideEncryptedIndicatorView()
                timer.cancel()
                timer = Timer()
                timer.schedule(
                    object : TimerTask() {
                        override fun run() {
                            handleEncryptedIndicatorView(
                                addresses = s?.toString() ?: "",
                                fieldName = fieldName,
                            )
                        }
                    },
                    CHECK_DELAY,
                )
                encryptedEmailAddressLookupTimer = timer
            }
        })
    }

    /** Sends an email message from the [SudoEmailClient]. */
    private fun sendEmailMessage() {
        if (validateFormData()) {
            showAlertDialog(
                titleResId = R.string.validate_to_address,
                positiveButtonResId = android.R.string.ok,
            )
            return
        }
        launch {
            try {
                showLoading(R.string.sending)
                withContext(Dispatchers.IO) {
                    val rfc822Data = Rfc822MessageFactory.makeRfc822Data(
                        from = emailAddress,
                        to = addressesToArray(binding.toTextView.text.toString()),
                        cc = addressesToArray(binding.ccTextView.text.toString()),
                        bcc = addressesToArray(binding.bccTextView.text.toString()),
                        subject = binding.subjectTextView.text.toString(),
                        body = binding.contentBody.text.toString(),
                    )
                    val input = SendEmailMessageInput(
                        rfc822Data = rfc822Data,
                        senderEmailAddressId = emailAddressId,
                    )
                    app.sudoEmailClient.sendEmailMessage(input)
                }
                showAlertDialog(
                    titleResId = R.string.sent,
                    positiveButtonResId = android.R.string.ok,
                    onPositive = {
                        navController.navigate(
                            SendEmailMessageFragmentDirections
                                .actionSendEmailMessageFragmentToEmailMessagesFragment(
                                    emailAddress,
                                    emailAddressId,
                                    args.sudo,
                                ),
                        )
                    },
                )
            } catch (e: SudoEmailClient.EmailMessageException) {
                showAlertDialog(
                    titleResId = R.string.send_email_message_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { sendEmailMessage() },
                    negativeButtonResId = android.R.string.cancel,
                )
            }
            hideLoading()
        }
    }

    /** Saves a draft email message from the [SudoEmailClient]. */
    private fun saveDraftEmailMessage() {
        launch {
            try {
                showLoading(R.string.saving)
                withContext(Dispatchers.IO) {
                    val rfc822Data = Rfc822MessageFactory.makeRfc822Data(
                        from = emailAddress,
                        to = addressesToArray(binding.toTextView.text.toString()),
                        cc = addressesToArray(binding.ccTextView.text.toString()),
                        bcc = addressesToArray(binding.bccTextView.text.toString()),
                        subject = binding.subjectTextView.text.toString(),
                        body = binding.contentBody.text.toString(),
                    )
                    val message = args.emailMessageWithBody
                    if (message?.isDraft == true) {
                        val input = UpdateDraftEmailMessageInput(
                            rfc822Data = rfc822Data,
                            senderEmailAddressId = emailAddressId,
                            id = message.id,
                        )
                        app.sudoEmailClient.updateDraftEmailMessage(input)
                    } else {
                        val input = CreateDraftEmailMessageInput(
                            rfc822Data = rfc822Data,
                            senderEmailAddressId = emailAddressId,
                        )
                        app.sudoEmailClient.createDraftEmailMessage(input)
                    }
                }
                showAlertDialog(
                    titleResId = R.string.saved,
                    positiveButtonResId = android.R.string.ok,
                    onPositive = {
                        navController.navigate(
                            SendEmailMessageFragmentDirections
                                .actionSendEmailMessageFragmentToEmailMessagesFragment(
                                    emailAddress,
                                    emailAddressId,
                                    args.sudo,
                                ),
                        )
                    },
                )
            } catch (e: SudoEmailClient.EmailMessageException) {
                showAlertDialog(
                    titleResId = R.string.save_email_message_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { saveDraftEmailMessage() },
                    negativeButtonResId = android.R.string.cancel,
                )
            }
            hideLoading()
        }
    }

    /**
     * Validates that an email address string is correctly formatted.
     */
    private fun validateEmailAddress(address: String): Boolean {
        val addressSpecPattern = Pattern.compile("^.+@[^.].*\\.[a-z]{2,}$")
        val rfc822AddressPattern = Pattern.compile("^.*<.+@[^.].*\\.[a-z]{2,}>$")

        return if (address.contains(" ")) {
            rfc822AddressPattern.matcher(address).find()
        } else {
            addressSpecPattern.matcher(address).find()
        }
    }

    /**
     * Transforms a string of comma-separated email addresses in a mutable list of email
     * address strings.
     */
    private fun addressesToArray(addresses: String): MutableList<String> {
        val result: MutableList<String> = mutableListOf()
        val split = addresses.split(",").map { it.trim() }
        result.addAll(split)
        return result
    }

    /**
     * Validates that a list of email addresses (in comma-separated string format) are
     * correctly formatted.
     */
    private fun validateEmailAddressList(addresses: String): Boolean {
        val addressesArray = addressesToArray(addresses)
        for (address in addressesArray) {
            if (!validateEmailAddress(address)) {
                return false
            }
        }

        return true
    }

    private suspend fun validateEncryptedEmailAddresses(addressesInput: String): Boolean {
        if (!validateEmailAddressList(addressesInput)) {
            return false
        }

        val emailAddresses = addressesToArray(addressesInput)
        val input = LookupEmailAddressesPublicInfoInput(emailAddresses)

        val emailAddressesPublicInfo = withContext(Dispatchers.IO) {
            app.sudoEmailClient.lookupEmailAddressesPublicInfo(input)
        }

        val resultEmailAddresses = emailAddressesPublicInfo.map { it.emailAddress }
        val result = emailAddresses.all { emailAddress ->
            resultEmailAddresses.contains(emailAddress)
        }

        return result
    }

    /**
     * Callback provided to each of the text input fields that's invoked when the
     * text value is updated.
     */
    private fun handleEncryptedIndicatorView(addresses: String, fieldName: String) {
        launch {
            try {
                encryptedInputStatuses[fieldName] = null
                hideEncryptedIndicatorView()

                if (addresses.isNotBlank()) {
                    val isEncrypted = validateEncryptedEmailAddresses(addresses)
                    encryptedInputStatuses[fieldName] = isEncrypted
                }

                // Check if at least one input value is true (encrypted), and the rest are null (empty)
                val validInput = encryptedInputStatuses.any { it.value == true }
                val invalidInput = encryptedInputStatuses.any { it.value == false }
                val validInputs = validInput && !invalidInput

                if (validInputs) {
                    showEncryptedIndicatorView()
                }
            } catch (e: SudoEmailClient.EmailAddressException) {
                withContext(Dispatchers.Main) {
                    showAlertDialog(
                        titleResId = R.string.encrypted_email_address_lookup_failure,
                        message = e.localizedMessage ?: "$e",
                        negativeButtonResId = android.R.string.cancel,
                    )
                }
            }
        }
    }

    /** Shows the encrypted indicator view. */
    private fun showEncryptedIndicatorView() {
        if (binding.encryptedIndicator.visibility == View.GONE) {
            binding.encryptedIndicator.visibility = View.VISIBLE
        }
    }

    /** Hides the encrypted indicator view. */
    private fun hideEncryptedIndicatorView() {
        if (binding.encryptedIndicator.visibility == View.VISIBLE) {
            binding.encryptedIndicator.visibility = View.GONE
        }
    }

    /** Validates submitted input form data. */
    private fun validateFormData(): Boolean {
        return binding.toTextView.text.isBlank()
    }

    /**
     * Configures the view with the appropriate values from the email message contents.
     *
     * @param emailMessage [EmailMessage] Email message to configure the view with.
     * @param emailMessageWithBody [SimplifiedEmailMessage] Email message containing the message
     *  body to configure the view with.
     */
    private fun configureEmailMessageContents(
        emailMessage: EmailMessage,
        emailMessageWithBody: SimplifiedEmailMessage,
    ) {
        binding.toTextView.setText(if (emailMessage.to.isNotEmpty()) emailMessage.to.joinToString("\n") else "")
        binding.ccTextView.setText(if (emailMessage.cc.isNotEmpty()) emailMessage.cc.joinToString("\n") else "")
        binding.subjectTextView.setText(if (emailMessage.subject.isNullOrBlank()) "" else emailMessage.subject)
        binding.contentBody.setText(emailMessageWithBody.body)
    }

    /**
     * Configures the view with the information required to compose a reply message.
     *
     * @param emailMessage [EmailMessage] Email message to configure the view with.
     * @param emailMessageWithBody [SimplifiedEmailMessage] Email message containing the message
     *  body to configure the view with.
     */
    private fun configureReplyContents(
        emailMessage: EmailMessage,
        emailMessageWithBody: SimplifiedEmailMessage,
    ) {
        binding.toTextView.setText(if (emailMessage.from.isNotEmpty()) emailMessage.from.joinToString() else "")
        binding.ccTextView.setText(if (emailMessage.cc.isNotEmpty()) emailMessage.cc.joinToString() else "")
        if (emailMessageWithBody.subject.startsWith("Re:")) {
            binding.subjectTextView.setText(emailMessage.subject)
        } else {
            binding.subjectTextView.setText(getString(R.string.reply_message, emailMessage.subject))
        }
        binding.contentBody.setText(getString(R.string.reply_body, emailMessageWithBody.body))
    }

    /**
     * Sets toolbar items and edit text fields to enabled/disabled.
     *
     * @param isEnabled [Boolean] If true, toolbar items and edit text fields will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        toolbarMenu.getItem(0)?.isEnabled = isEnabled
        binding.toTextView.isEnabled = isEnabled
        binding.ccTextView.isEnabled = isEnabled
        binding.bccTextView.isEnabled = isEnabled
        binding.subjectTextView.isEnabled = isEnabled
        binding.contentBody.isEnabled = isEnabled
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
