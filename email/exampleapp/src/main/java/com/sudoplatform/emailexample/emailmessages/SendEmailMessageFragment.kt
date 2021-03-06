/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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
        navController = Navigation.findNavController(view)

        val emailMessageWithBody = args.emailMessageWithBody
        val emailMessage = args.emailMessage
        if (emailMessageWithBody != null && emailMessage != null) {
            configureReplyContents(emailMessage, emailMessageWithBody)
        }
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    /** Sends an email message from the [SudoEmailClient]. */
    private fun sendEmailMessage() {
        if (validateFormData()) {
            showAlertDialog(
                titleResId = R.string.validate_to_address,
                positiveButtonResId = android.R.string.ok
            )
            return
        }
        launch {
            try {
                showLoading(R.string.sending)
                withContext(Dispatchers.IO) {
                    val rfc822Data = Rfc822MessageFactory.makeRfc822Data(
                        from = emailAddress,
                        to = binding.toTextView.text.split(",").toList(),
                        cc = binding.ccTextView.text.split(",").toList(),
                        bcc = binding.bccTextView.text.split(",").toList(),
                        subject = binding.subjectTextView.text.toString(),
                        body = binding.contentBody.text.toString()
                    )
                    app.sudoEmailClient.sendEmailMessage(rfc822Data, emailAddressId)
                }
                showAlertDialog(
                    titleResId = R.string.sent,
                    positiveButtonResId = android.R.string.ok,
                    onPositive = {
                        navController.navigate(
                            SendEmailMessageFragmentDirections
                                .actionSendEmailMessageFragmentToEmailMessagesFragment(
                                    emailAddress,
                                    emailAddressId
                                )
                        )
                    }
                )
            } catch (e: SudoEmailClient.EmailMessageException) {
                showAlertDialog(
                    titleResId = R.string.send_email_message_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { sendEmailMessage() },
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideLoading()
        }
    }

    /** Validates submitted input form data. */
    private fun validateFormData(): Boolean {
        return binding.toTextView.text.isBlank()
    }

    /**
     * Configures the view with the information required to compose a reply message.
     *
     * @param emailMessage Email message to configure the view with.
     * @param emailMessageWithBody Email message containing the message body to configure the view with.
     */
    private fun configureReplyContents(
        emailMessage: EmailMessage,
        emailMessageWithBody: SimplifiedEmailMessage
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
     * @param isEnabled If true, toolbar items and edit text fields will be enabled.
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
