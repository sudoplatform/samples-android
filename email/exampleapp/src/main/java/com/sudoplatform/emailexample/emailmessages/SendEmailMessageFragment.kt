/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import android.app.Activity.RESULT_OK
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
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
import com.sudoplatform.emailexample.databinding.FragmentSendEmailMessageBinding
import com.sudoplatform.emailexample.emailmessages.emailAttachments.EmailAttachmentAdapter
import com.sudoplatform.emailexample.showAlertDialog
import com.sudoplatform.emailexample.swipe.SwipeLeftActionHelper
import com.sudoplatform.emailexample.util.ObjectDelegate
import com.sudoplatform.emailexample.util.Rfc822MessageFactory
import com.sudoplatform.emailexample.util.SimplifiedEmailMessage
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudoemail.types.EmailAttachment
import com.sudoplatform.sudoemail.types.EmailMessage
import com.sudoplatform.sudoemail.types.InternetMessageFormatHeader
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
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
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

    /** A reference to the [RecyclerView.Adapter] handling [EmailAttachment] data. */
    private lateinit var attachmentsAdapter: EmailAttachmentAdapter

    /** Email Address used to compose a reply email message. */
    private lateinit var emailAddress: String

    /** Email display name associated with the Email Address. */
    private lateinit var emailDisplayName: String

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

    /** A mutable list of [EmailAttachment]s. */
    private val emailAttachmentList: MutableList<EmailAttachment> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingDelegate.attach(FragmentSendEmailMessageBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.compose_email_message)
            inflateMenu(R.menu.nav_menu_with_send_save_attachment_buttons)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.send -> {
                        sendEmailMessage()
                    }
                    R.id.save -> {
                        saveDraftEmailMessage()
                    }
                    R.id.attachment -> {
                        launchFilePicker()
                    }
                }
                true
            }
            toolbarMenu = menu
        }
        app = requireActivity().application as App
        emailAddress = args.emailAddress
        emailDisplayName = args.emailDisplayName.toString()
        emailAddressId = args.emailAddressId
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView()
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
                    val emailMessageHeader = InternetMessageFormatHeader(
                        from = EmailMessage.EmailAddress(emailAddress, emailDisplayName),
                        to = if (binding.toTextView.text.isNotEmpty()) addressesToArray(binding.toTextView.text.toString()) else emptyList(),
                        cc = if (binding.ccTextView.text.isNotEmpty()) addressesToArray(binding.ccTextView.text.toString()) else emptyList(),
                        bcc = if (binding.bccTextView.text.isNotEmpty()) addressesToArray(binding.bccTextView.text.toString()) else emptyList(),
                        replyTo = emptyList(),
                        subject = binding.subjectTextView.text.toString(),
                    )
                    val input = SendEmailMessageInput(
                        senderEmailAddressId = emailAddressId,
                        emailMessageHeader = emailMessageHeader,
                        body = binding.contentBody.text.toString(),
                        attachments = emailAttachmentList,
                    )
                    app.sudoEmailClient.sendEmailMessage(input)
                }
                Toast.makeText(context, getString(R.string.sent), Toast.LENGTH_SHORT).show()
                navController.navigate(
                    SendEmailMessageFragmentDirections
                        .actionSendEmailMessageFragmentToEmailMessagesFragment(
                            emailAddress,
                            emailAddressId,
                        ),
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
                        to = addressesToArray(binding.toTextView.text.toString())
                            .map { it.toString() },
                        cc = addressesToArray(binding.ccTextView.text.toString())
                            .map { it.toString() },
                        bcc = addressesToArray(binding.bccTextView.text.toString())
                            .map { it.toString() },
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
                Toast.makeText(context, getString(R.string.saved), Toast.LENGTH_SHORT).show()
                navController.navigate(
                    SendEmailMessageFragmentDirections
                        .actionSendEmailMessageFragmentToEmailMessagesFragment(
                            emailAddress,
                            emailAddressId,
                        ),
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

    /** Validates submitted input form data. */
    private fun validateFormData(): Boolean {
        return binding.toTextView.text.isBlank()
    }

    /**
     * Validates that an email address string is correctly formatted.
     *
     * @param address [String] The email address to validate.
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
     * Transforms a string of comma-separated email addresses to a mutable list of
     * [EmailMessage.EmailAddress].
     *
     * @param addresses [String] The comma-separated email addresses to transform.
     */
    private fun addressesToArray(addresses: String): MutableList<EmailMessage.EmailAddress> {
        val result: MutableList<EmailMessage.EmailAddress> = mutableListOf()
        val split = addresses.split(",").map { EmailMessage.EmailAddress(it.trim()) }
        result.addAll(split)
        return result
    }

    /**
     * Validates that a list of email addresses (in comma-separated string format) are
     * correctly formatted.
     *
     * @param addresses [String] The comma-separated email addresses to validate.
     */
    private fun validateEmailAddressList(addresses: String): Boolean {
        val addressesArray = addressesToArray(addresses)
        for (address in addressesArray) {
            if (!validateEmailAddress(address.emailAddress)) {
                return false
            }
        }
        return true
    }

    /**
     * Validates whether the input email address exist within the platform.
     *
     * @param addresses [String] The comma-separated email addresses to validate.
     */
    private suspend fun validateEncryptedEmailAddresses(addresses: String): Boolean {
        if (!validateEmailAddressList(addresses)) {
            return false
        }

        val emailAddresses = addressesToArray(addresses).map { it.emailAddress }
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
     * Configures a listener on the recipient input fields.
     */
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

    /** Launches the Android Storage access to select a file for an attachment. */
    private fun launchFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        filePicker.launch(intent)
    }

    private val filePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            data?.data?.let { uri ->
                val contentId = UUID.randomUUID().toString()
                val fileName = getAttachmentFileName(uri)
                val mimeType = getAttachmentMimeType(uri)
                val attachmentData = getAttachmentData(uri)

                val attachment = EmailAttachment(
                    fileName = fileName,
                    contentId = contentId,
                    mimeType = mimeType,
                    inlineAttachment = false,
                    data = attachmentData,
                )
                emailAttachmentList.add(attachment)
                attachmentsAdapter.notifyDataSetChanged()
            }
        }
    }

    /**
     * Returns the name of the file selected from the file picker.
     *
     * @param uri [Uri] The URI of the selected file.
     */
    private fun getAttachmentFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            context?.contentResolver?.query(
                uri,
                null,
                null,
                null,
                null,
            )?.use {
                if (it.moveToFirst()) {
                    result = it.getString(it.getColumnIndexOrThrow("_display_name"))
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result ?: "unknown"
    }

    /**
     * Returns the MIME type of the file selected from the file picker.
     *
     * @param uri [Uri] The URI of the selected file.
     */
    private fun getAttachmentMimeType(uri: Uri): String {
        return if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            context?.contentResolver?.getType(uri) ?: ""
        } else {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                fileExtension.lowercase(Locale.ROOT),
            ) ?: ""
        }
    }

    /**
     * Returns the data of the file selected from the file picker in bytes.
     *
     * @param uri [Uri] The URI of the selected file.
     */
    private fun getAttachmentData(uri: Uri): ByteArray {
        var bytes: ByteArray = byteArrayOf()
        val inputStream = context?.contentResolver?.openInputStream(uri)
        inputStream?.use {
            bytes = it.readBytes()
        }
        return bytes
    }

    /**
     * Configures the [RecyclerView] used to display the listed [EmailAttachment] items and listens
     * to item select events to launch the Android file viewer.
     */
    private fun configureRecyclerView() {
        attachmentsAdapter = EmailAttachmentAdapter(emailAttachmentList) { emailAttachment ->
            val fileName = emailAttachment.fileName
            val mimeType = emailAttachment.mimeType
            val data = emailAttachment.data

            val file = File.createTempFile(fileName, null)
            FileOutputStream(file).use { it.write(data) }

            val contentUri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().packageName + ".fileprovider",
                file,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                setDataAndType(contentUri, mimeType)
            }
            startActivity(intent)
        }

        binding.emailAttachmentRecyclerView.adapter = attachmentsAdapter
        binding.emailAttachmentRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        configureSwipeToDelete()
    }

    /**
     * Configures the swipe to delete action by listening to [RecyclerView.ViewHolder] swipe events
     * and drawing the swipe view and delete icon.
     *
     * Swiping in from the left will perform a delete operation and remove the item from the view.
     */
    private fun configureSwipeToDelete() {
        val itemTouchCallback = SwipeLeftActionHelper(requireContext(), onSwipedAction = ::onSwiped)
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.emailAttachmentRecyclerView)
    }

    private fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        emailAttachmentList.removeAt(viewHolder.adapterPosition)
        attachmentsAdapter.notifyItemRemoved(viewHolder.adapterPosition)
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
        binding.toTextView.setText(if (emailMessage.to.isNotEmpty()) emailMessage.to.joinToString("\n") { it.toString() } else "")
        binding.ccTextView.setText(if (emailMessage.cc.isNotEmpty()) emailMessage.cc.joinToString("\n") { it.toString() } else "")
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
        binding.toTextView.setText(if (emailMessage.from.isNotEmpty()) emailMessage.from.joinToString { it.emailAddress } else "")
        binding.ccTextView.setText(if (emailMessage.cc.isNotEmpty()) emailMessage.cc.joinToString { it.emailAddress } else "")
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
