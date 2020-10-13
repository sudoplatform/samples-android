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
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.sudoplatform.emailexample.App
import com.sudoplatform.emailexample.MissingFragmentArgumentException
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.createLoadingAlertDialog
import com.sudoplatform.emailexample.showAlertDialog
import com.sudoplatform.emailexample.util.Rfc822MessageFactory
import com.sudoplatform.emailexample.util.SimplifiedEmailMessage
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudoemail.types.EmailMessage
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_provision_email_address.view.*
import kotlinx.android.synthetic.main.fragment_send_email_message.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    /** Toolbar [Menu] displaying title and send button. */
    private lateinit var toolbarMenu: Menu

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private lateinit var loading: AlertDialog

    /** Email Address used to compose a reply email message. */
    private lateinit var emailAddress: String

    /** Email Address Identifier used to compose a reply email message. */
    private lateinit var emailAddressId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_send_email_message, container, false)
        val toolbar = (view.toolbar as Toolbar)
        toolbar.title = getString(R.string.compose_email_message)

        toolbar.inflateMenu(R.menu.nav_menu_with_send_button)
        toolbar.setOnMenuItemClickListener {
            when (it?.itemId) {
                R.id.send -> {
                    sendEmailMessage()
                }
            }
            true
        }
        toolbarMenu = toolbar.menu
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        emailAddress = requireArguments().getString(getString(R.string.email_address))
            ?: throw MissingFragmentArgumentException("Email address missing")
        emailAddressId = requireArguments().getString(getString(R.string.email_address_id))
            ?: throw MissingFragmentArgumentException("Email address id missing")

        val emailMessageWithBody = requireArguments().getParcelable<SimplifiedEmailMessage>(getString(R.string.simplified_email_message))
        val emailMessage = requireArguments().getParcelable<EmailMessage>(getString(R.string.email_message))
        if (emailMessageWithBody != null && emailMessage != null) {
            configureReplyContents(emailMessage, emailMessageWithBody)
        }
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
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
        val app = requireActivity().application as App
        launch {
            try {
                showLoading(R.string.sending)
                withContext(Dispatchers.IO) {
                    val rfc822Data = Rfc822MessageFactory.makeRfc822Data(
                        from = emailAddress,
                        to = toTextView.text.split(",").toList(),
                        cc = ccTextView.text.split(",").toList(),
                        bcc = bccTextView.text.split(",").toList(),
                        subject = subjectTextView.text.toString(),
                        body = contentBody.text.toString()
                    )
                    app.sudoEmailClient.sendEmailMessage(rfc822Data, emailAddressId)
                }
                showAlertDialog(
                    titleResId = R.string.sent,
                    positiveButtonResId = android.R.string.ok,
                    onPositive = {
                        val bundle = bundleOf(
                            getString(R.string.email_address) to emailAddress,
                            getString(R.string.email_address_id) to emailAddressId
                        )
                        navController.navigate(R.id.action_sendEmailMessageFragment_to_emailMessagesFragment, bundle)
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
        return toTextView.text.isBlank()
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
        toTextView.setText(if (emailMessage.from.isNotEmpty()) emailMessage.from.joinToString() else "")
        ccTextView.setText(if (emailMessage.cc.isNotEmpty()) emailMessage.cc.joinToString() else "")
        if (emailMessageWithBody.subject.startsWith("Re:")) {
            subjectTextView.setText(emailMessage.subject)
        } else {
            subjectTextView.setText(getString(R.string.reply_message, emailMessage.subject))
        }
        contentBody.setText(getString(R.string.reply_body, emailMessageWithBody.body))
    }

    /**
     * Sets toolbar items and edit text fields to enabled/disabled.
     *
     * @param isEnabled If true, toolbar items and edit text fields will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        toolbarMenu.getItem(0)?.isEnabled = isEnabled
        toTextView.isEnabled = isEnabled
        ccTextView.isEnabled = isEnabled
        bccTextView.isEnabled = isEnabled
        subjectTextView.isEnabled = isEnabled
        contentBody.isEnabled = isEnabled
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
