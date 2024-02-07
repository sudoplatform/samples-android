/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import android.os.Bundle
import android.text.format.DateFormat
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
import com.sudoplatform.emailexample.databinding.FragmentReadEmailMessageBinding
import com.sudoplatform.emailexample.showAlertDialog
import com.sudoplatform.emailexample.util.ObjectDelegate
import com.sudoplatform.emailexample.util.Rfc822MessageParser
import com.sudoplatform.emailexample.util.SimplifiedEmailMessage
import com.sudoplatform.sudoapiclient.sudoApiClientLogger
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudoemail.types.BatchOperationResult
import com.sudoplatform.sudoemail.types.BatchOperationStatus
import com.sudoplatform.sudoemail.types.EmailMessage
import com.sudoplatform.sudoemail.types.inputs.GetEmailMessageRfc822DataInput
import com.sudoplatform.sudoprofiles.Sudo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import kotlin.coroutines.CoroutineContext

/**
 * This [ReadEmailMessageFragment] presents a view to allow a user to view the contents of an an
 * email message.
 *
 * - Links From:
 *  - [EmailMessagesFragment]: A user selects an [EmailMessage] from the list which will present this
 *   view with the contents of the selected [EmailMessage].
 *
 * - Links To:
 *  - [SendEmailMessageFragment]: If a user taps the "Reply" button on the top right of the toolbar,
 *   the [SendEmailMessageFragment] will be presented so that a user can compose a reply email message.
 */
class ReadEmailMessageFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentReadEmailMessageBinding>()
    private val binding by bindingDelegate

    /** Toolbar [Menu] displaying title and block and reply buttons. */
    private lateinit var toolbarMenu: Menu

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: ReadEmailMessageFragmentArgs by navArgs()

    /** Email Address used to compose a reply email message. */
    private lateinit var emailAddress: String

    /** Email Address Identifier used to compose a reply email message. */
    private lateinit var emailAddressId: String

    /** The selected email message. */
    private lateinit var emailMessage: EmailMessage

    /** The simplified email message containing the body. */
    private lateinit var emailMessageWithBody: SimplifiedEmailMessage

    private lateinit var sudo: Sudo

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingDelegate.attach(FragmentReadEmailMessageBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.read_email_message)
            inflateMenu(R.menu.nav_menu_with_block_reply_buttons)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.reply -> {
                        navController.navigate(
                            ReadEmailMessageFragmentDirections
                                .actionReadEmailMessageFragmentToSendEmailMessageFragment(
                                    emailAddress,
                                    emailAddressId,
                                    args.sudo,
                                    emailMessage,
                                    emailMessageWithBody,
                                ),
                        )
                    }
                    R.id.block -> {
                        sudoApiClientLogger.info("Block clicked")
                        blockEmailAddress(emailMessage.from[0].emailAddress)
                    }
                }
                true
            }
            toolbarMenu = menu
        }
        app = requireActivity().application as App
        emailAddress = args.emailAddress
        emailAddressId = args.emailAddressId
        emailMessage = args.emailMessage
        sudo = args.sudo
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        readEmailMessage()
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    /** Reads an email message from the [SudoEmailClient]. */
    private fun readEmailMessage() {
        launch {
            try {
                showLoading(R.string.reading)
                val input = GetEmailMessageRfc822DataInput(
                    id = emailMessage.id,
                    emailAddressId = emailAddressId,
                )
                val rfc822Data = withContext(Dispatchers.IO) {
                    app.sudoEmailClient.getEmailMessageRfc822Data(input)
                }
                if (rfc822Data != null) {
                    emailMessageWithBody = Rfc822MessageParser.parseRfc822Data(rfc822Data.rfc822Data)
                    configureEmailMessageContents(emailMessage)
                }
            } catch (e: SudoEmailClient.EmailMessageException) {
                showAlertDialog(
                    titleResId = R.string.read_email_message_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { readEmailMessage() },
                    negativeButtonResId = android.R.string.cancel,
                )
            }
            hideLoading()
        }
    }

    /**
     *
     */
    private fun blockEmailAddress(address: String) {
        launch {
            try {
                showLoading(R.string.blocking_address)
                when (val response = app.sudoEmailClient.blockEmailAddresses(listOf(address))) {
                    is BatchOperationResult.SuccessOrFailureResult -> {
                        if (response.status === BatchOperationStatus.SUCCESS) {
                            showAlertDialog(
                                titleResId = R.string.block_address_success,
                                positiveButtonResId = R.string.continue_message,
                                onPositive = {
                                    navController.navigate(
                                        ReadEmailMessageFragmentDirections.actionReadEmailMessageFragmentToEmailMessagesFragment(
                                            emailAddress,
                                            emailAddressId,
                                            sudo,
                                        ),
                                    )
                                },
                            )
                        } else {
                            showAlertDialog(
                                titleResId = R.string.block_address_failure,
                                message = "Something went wrong.",
                                positiveButtonResId = R.string.try_again,
                                onPositive = { blockEmailAddress(address) },
                                negativeButtonResId = android.R.string.cancel,
                            )
                        }
                    } is BatchOperationResult.PartialResult -> {
                        // no-op
                    }
                }
            } catch (e: SudoEmailClient.EmailBlocklistException) {
                showAlertDialog(
                    titleResId = R.string.block_address_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { blockEmailAddress(address) },
                    negativeButtonResId = android.R.string.cancel,
                )
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * Configures the view with the appropriate values from the email message contents.
     *
     * @param emailMessage [EmailMessage] Email message to configure the view with.
     */
    private fun configureEmailMessageContents(emailMessage: EmailMessage) {
        binding.dateValue.text = formatDate(emailMessage.createdAt)
        binding.fromValue.text = if (emailMessage.from.isNotEmpty()) emailMessage.from.first().toString() else ""
        binding.toValue.text = if (emailMessage.to.isNotEmpty()) emailMessage.to.joinToString("\n") else ""
        binding.ccValue.text = if (emailMessage.cc.isNotEmpty()) emailMessage.cc.joinToString("\n") else ""
        binding.subject.text = emailMessage.subject
        binding.contentBody.text = emailMessageWithBody.body
    }

    /**
     * Formats a [Date] to a presentable String.
     *
     * @param date [Date] The [Date] to be formatted.
     * @return A presentable [String] containing the date.
     */
    private fun formatDate(date: Date): String {
        return DateFormat.format("MM/dd/yyyy", date).toString()
    }

    /**
     * Sets toolbar items to enabled/disabled.
     *
     * @param isEnabled [Boolean] If true, toolbar items will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        toolbarMenu.getItem(0)?.isEnabled = isEnabled
    }

    /** Displays the loading [AlertDialog] indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int) {
        loading = createLoadingAlertDialog(textResId)
        loading?.show()
        binding.contents.visibility = View.GONE
        setItemsEnabled(false)
    }

    /** Dismisses the loading [AlertDialog] indicating that an operation has finished. */
    private fun hideLoading() {
        loading?.dismiss()
        if (bindingDelegate.isAttached()) {
            binding.contents.visibility = View.VISIBLE
            setItemsEnabled(true)
        }
    }
}
