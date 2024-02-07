/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
import com.sudoplatform.emailexample.databinding.FragmentEmailMessagesBinding
import com.sudoplatform.emailexample.emailaddresses.EmailAddressesFragment
import com.sudoplatform.emailexample.emailfolders.EmailFolderAdapter
import com.sudoplatform.emailexample.emailfolders.FolderTypes
import com.sudoplatform.emailexample.showAlertDialog
import com.sudoplatform.emailexample.swipe.SwipeLeftActionHelper
import com.sudoplatform.emailexample.util.ObjectDelegate
import com.sudoplatform.emailexample.util.Rfc822MessageParser
import com.sudoplatform.emailexample.util.SimplifiedEmailMessage
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudoemail.subscription.EmailMessageSubscriber
import com.sudoplatform.sudoemail.subscription.Subscriber
import com.sudoplatform.sudoemail.types.CachePolicy
import com.sudoplatform.sudoemail.types.Direction
import com.sudoplatform.sudoemail.types.DraftEmailMessageMetadata
import com.sudoplatform.sudoemail.types.DraftEmailMessageWithContent
import com.sudoplatform.sudoemail.types.EmailAddress
import com.sudoplatform.sudoemail.types.EmailFolder
import com.sudoplatform.sudoemail.types.EmailMessage
import com.sudoplatform.sudoemail.types.ListAPIResult
import com.sudoplatform.sudoemail.types.Owner
import com.sudoplatform.sudoemail.types.State
import com.sudoplatform.sudoemail.types.inputs.DeleteDraftEmailMessagesInput
import com.sudoplatform.sudoemail.types.inputs.GetDraftEmailMessageInput
import com.sudoplatform.sudoemail.types.inputs.ListEmailFoldersForEmailAddressIdInput
import com.sudoplatform.sudoemail.types.inputs.ListEmailMessagesForEmailFolderIdInput
import com.sudoplatform.sudoemail.types.inputs.UpdateEmailMessagesInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID
import kotlin.coroutines.CoroutineContext

/**
 * This [EmailMessagesFragment] presents a list of [EmailMessage]s associated with an [EmailAddress].
 *
 * - Links From:
 *  - [EmailAddressesFragment]: A user chooses an [EmailAddress] from the list which will show this
 *   view with the list of [EmailMessage]s for this [EmailAddress].
 *
 * - Links To:
 *  - [ReadEmailMessageFragment]: If a user taps on an [EmailMessage], the [ReadEmailMessageFragment]
 *   will be presented so that the user can read the email message.
 *  - [SendEmailMessageFragment]: If a user taps the "Compose" button on the top right of the toolbar,
 *   the [SendEmailMessageFragment] will be presented so that the user can compose a new email message.
 */
class EmailMessagesFragment : Fragment(), CoroutineScope, AdapterView.OnItemSelectedListener {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentEmailMessagesBinding>()
    private val binding by bindingDelegate

    /** Toolbar [Menu] displaying title and compose button. */
    private lateinit var toolbarMenu: Menu

    /** A list of folders to organize email messages. */
    private var emailFoldersList = mutableListOf<EmailFolder>()

    /** A reference to the [ArrayAdapter] holding the [EmailFolder] data. */
    private lateinit var foldersAdapter: EmailFolderAdapter

    /** A reference to the [RecyclerView.Adapter] handling [EmailMessage] data. */
    private lateinit var adapter: EmailMessageAdapter

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** A mutable list of [EmailMessage]s. */
    private var emailMessageList = mutableListOf<EmailMessage>()

    /** A mutable list of [SimplifiedEmailMessage]s. */
    private var draftEmailMessageList = mutableListOf<SimplifiedEmailMessage>()

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: EmailMessagesFragmentArgs by navArgs()

    /** The selected Email address used to filter email messages. */
    private lateinit var emailAddress: String

    /** The selected Email address Identifier used to filter email messages. */
    private lateinit var emailAddressId: String

    /** The selected [EmailFolder] used to filter email messages. */
    private lateinit var selectedEmailFolder: EmailFolder

    /** Subscription ID for email messages */
    private val subscriptionId = UUID.randomUUID().toString()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingDelegate.attach(FragmentEmailMessagesBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.email_messages)
            inflateMenu(R.menu.nav_menu_with_compose_button)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.compose -> {
                        navController.navigate(
                            EmailMessagesFragmentDirections
                                .actionEmailMessagesFragmentToSendEmailMessageFragment(
                                    emailAddress,
                                    emailAddressId,
                                    args.sudo,
                                ),
                        )
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
        configureRecyclerView()
        navController = Navigation.findNavController(view)

        binding.foldersSpinner.onItemSelectedListener = this
        listEmailFolders(CachePolicy.REMOTE_ONLY)
        foldersAdapter = EmailFolderAdapter(
            requireContext(),
        )
        foldersAdapter.notifyDataSetChanged()
        binding.foldersSpinner.adapter = foldersAdapter
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        subscribeToEmailMessages()
    }

    override fun onPause() {
        super.onPause()
        unsubscribeFromEmailMessages()
    }

    /**
     * List [EmailFolder]s from the [SudoEmailClient].
     *
     * @param cachePolicy [CachePolicy] Option of either retrieving [EmailFolder] data from the
     *  cache or network.
     */
    private fun listEmailFolders(cachePolicy: CachePolicy) {
        launch {
            showLoading()
            try {
                val emailFolders = withContext(Dispatchers.IO) {
                    val input = ListEmailFoldersForEmailAddressIdInput(
                        emailAddressId = emailAddressId,
                        cachePolicy = cachePolicy,
                    )
                    app.sudoEmailClient.listEmailFoldersForEmailAddressId(input)
                }
                emailFoldersList.clear()
                emailFoldersList.addAll(emailFolders.items)
                foldersAdapter.notifyDataSetChanged()
            } catch (e: SudoEmailClient.EmailFolderException) {
                showAlertDialog(
                    titleResId = R.string.list_email_folders_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { listEmailFolders(CachePolicy.REMOTE_ONLY) },
                    negativeButtonResId = android.R.string.cancel,
                )
            }
            binding.filter.visibility = View.VISIBLE
        }
    }

    /**
     * List [EmailMessage]s from the [SudoEmailClient].
     *
     * @param emailFolderId [String] The identifier of the email folder assigned to the email
     *  messages to retrieve.
     * @param cachePolicy [CachePolicy] Option of either retrieving [EmailMessage] data from the
     *  cache or network.
     */
    private fun listEmailMessages(emailFolderId: String, cachePolicy: CachePolicy) {
        launch {
            try {
                showLoading()
                if (emailFolderId == FolderTypes.DRAFTS.toString()) {
                    val draftMessages = withContext(Dispatchers.IO) {
                        retrieveDraftEmailMessages()
                    }
                    emailMessageList.clear()
                    draftMessages.forEach { message ->
                        emailMessageList.add(transformDraftToEmailMessage(message))
                        draftEmailMessageList.add(
                            transformDraftToSimplifiedEmailMessage(
                                message,
                            ),
                        )
                    }
                    emailMessageList.sortWith { lhs, rhs ->
                        when {
                            lhs.createdAt.before(rhs.createdAt) -> 1
                            lhs.createdAt.after(rhs.createdAt) -> -1
                            else -> 0
                        }
                    }
                    adapter.notifyDataSetChanged()
                } else {
                    val emailMessages = withContext(Dispatchers.IO) {
                        val input = ListEmailMessagesForEmailFolderIdInput(
                            emailFolderId,
                            cachePolicy,
                        )
                        app.sudoEmailClient.listEmailMessagesForEmailFolderId(input)
                    }

                    when (emailMessages) {
                        is ListAPIResult.Success -> {
                            emailMessageList.clear()
                            val address = EmailMessage.EmailAddress(emailAddress)
                            for (emailMessage in emailMessages.result.items) {
                                if (emailMessage.emailAddressId == emailAddressId ||
                                    emailMessage.to.contains(address) ||
                                    emailMessage.cc.contains(address) ||
                                    emailMessage.bcc.contains(address)
                                ) {
                                    emailMessageList.add(emailMessage)
                                }
                            }
                            emailMessageList.sortWith { lhs, rhs ->
                                when {
                                    lhs.createdAt.before(rhs.createdAt) -> 1
                                    lhs.createdAt.after(rhs.createdAt) -> -1
                                    else -> 0
                                }
                            }
                            adapter.notifyDataSetChanged()
                        }
                        is ListAPIResult.Partial -> {
                            val cause = emailMessages.result.failed.first().cause
                            showAlertDialog(
                                titleResId = R.string.list_email_addresses_failure,
                                message = cause.localizedMessage ?: "$cause",
                                positiveButtonResId = R.string.try_again,
                                onPositive = {
                                    listEmailMessages(
                                        selectedEmailFolder.id,
                                        CachePolicy.REMOTE_ONLY,
                                    )
                                },
                                negativeButtonResId = android.R.string.cancel,
                            )
                        }
                    }
                }
            } catch (e: SudoEmailClient.EmailMessageException) {
                showAlertDialog(
                    titleResId = R.string.list_email_messages_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = {
                        listEmailMessages(
                            selectedEmailFolder.id,
                            CachePolicy.REMOTE_ONLY,
                        )
                    },
                    negativeButtonResId = android.R.string.cancel,
                )
            }
            hideLoading()
        }
    }

    /**
     * Move the selected [EmailMessage] to the trash folder.
     *
     * @param id [String] The identifier of the [EmailMessage] to move to the trash folder.
     */
    private fun moveEmailMessageToTrash(id: String) {
        launch {
            try {
                showDeleteAlert(R.string.moving_email_message_to_trash)
                val trashFolder = (emailFoldersList.filter { it.folderName == "TRASH" }).first()
                val updateInput = UpdateEmailMessagesInput(
                    ids = listOf(id),
                    values = UpdateEmailMessagesInput.UpdatableValues(folderId = trashFolder.id),
                )
                withContext(Dispatchers.IO) {
                    app.sudoEmailClient.updateEmailMessages(updateInput)
                }
                showAlertDialog(
                    titleResId = R.string.success,
                    positiveButtonResId = android.R.string.ok,
                )
            } catch (e: SudoEmailClient.EmailMessageException) {
                showAlertDialog(
                    titleResId = R.string.moving_email_message_failure,
                    message = e.localizedMessage ?: e.toString(),
                    negativeButtonResId = android.R.string.cancel,
                )
            }
            hideDeleteAlert()
        }
    }

    /**
     * Delete a selected [EmailMessage] from the [SudoEmailClient].
     *
     * @param id [String] The identifier of the [EmailMessage] to delete.
     */
    private fun deleteEmailMessage(id: String) {
        launch {
            try {
                showDeleteAlert(R.string.deleting_email_message)
                withContext(Dispatchers.IO) {
                    app.sudoEmailClient.deleteEmailMessage(id)
                }
                showAlertDialog(
                    titleResId = R.string.success,
                    positiveButtonResId = android.R.string.ok,
                )
            } catch (e: SudoEmailClient.EmailMessageException) {
                showAlertDialog(
                    titleResId = R.string.deleting_email_message_failure,
                    message = e.localizedMessage ?: e.toString(),
                    negativeButtonResId = android.R.string.cancel,
                )
            }
            hideDeleteAlert()
        }
    }

    /**
     * Delete a selected draft [EmailMessage] from the [SudoEmailClient].
     *
     * @param id [String] The identifier of the draft [EmailMessage] to delete.
     */
    private fun deleteDraftEmailMessage(id: String) {
        launch {
            try {
                showDeleteAlert(R.string.deleting_draft_email_message)
                val input = DeleteDraftEmailMessagesInput(
                    ids = listOf(id),
                    emailAddressId,
                )
                withContext(Dispatchers.IO) {
                    app.sudoEmailClient.deleteDraftEmailMessages(input)
                }
                showAlertDialog(
                    titleResId = R.string.success,
                    positiveButtonResId = android.R.string.ok,
                )
            } catch (e: SudoEmailClient.EmailMessageException) {
                showAlertDialog(
                    titleResId = R.string.deleting_draft_email_message_failure,
                    message = e.localizedMessage ?: e.toString(),
                    negativeButtonResId = android.R.string.cancel,
                )
            }
            hideDeleteAlert()
        }
    }

    /** Subscribe to receive live updates as [EmailMessage]s are created and deleted. */
    private fun subscribeToEmailMessages() {
        launch {
            try {
                withContext(Dispatchers.IO) {
                    app.sudoEmailClient.subscribeToEmailMessages(
                        id = subscriptionId,
                        subscriber = emailMessageSubscriber,
                    )
                }
            } catch (e: SudoEmailClient.EmailMessageException) {
                showAlertDialog(
                    titleResId = R.string.subscribe_email_messages_failure,
                    message = e.localizedMessage ?: "$e",
                    negativeButtonResId = android.R.string.ok,
                    onNegative = {},
                )
            }
        }
    }

    private val emailMessageSubscriber = object : EmailMessageSubscriber {
        override fun connectionStatusChanged(state: Subscriber.ConnectionState) {
            if (state == Subscriber.ConnectionState.DISCONNECTED) {
                launch(Dispatchers.Main) {
                    showAlertDialog(
                        titleResId = R.string.subscribe_email_messages_failure,
                        messageResId = R.string.subscribe_lost_connection,
                        positiveButtonResId = android.R.string.ok,
                        onPositive = {},
                    )
                }
            }
        }

        override fun emailMessageChanged(emailMessage: EmailMessage) {
            launch(Dispatchers.Main) {
                addOrDeleteEmailMessage(emailMessage)
                adapter.notifyDataSetChanged()
            }
        }
    }

    /** Add to the list of email messages or remove an existing [EmailMessage]. */
    private fun addOrDeleteEmailMessage(newEmailMessage: EmailMessage) {
        val removeAtIndex = emailMessageList.indexOfFirst { it.id == newEmailMessage.id }
        if (removeAtIndex == -1) {
            emailMessageList.add(newEmailMessage)
        } else {
            emailMessageList.removeAt(removeAtIndex)
        }
    }

    /** Unsubscribe from live [EmailMessage] updates. */
    private fun unsubscribeFromEmailMessages() {
        launch {
            try {
                withContext(Dispatchers.IO) {
                    app.sudoEmailClient.unsubscribeFromEmailMessages(subscriptionId)
                }
            } catch (e: SudoEmailClient.EmailMessageException) {
                app.logger.error("Failed to unsubscribe: $e")
            }
        }
    }

    /**
     * Configures the [RecyclerView] used to display the listed [EmailMessage] items and listens to
     * item select events to navigate to the [ReadEmailMessageFragment].
     */
    private fun configureRecyclerView() {
        adapter = EmailMessageAdapter(emailMessageList) { emailMessage ->
            if (selectedEmailFolder.folderName == FolderTypes.DRAFTS.toString()) {
                navController.navigate(
                    EmailMessagesFragmentDirections.actionEmailMessagesFragmentToSendEmailMessageFragment(
                        emailAddress,
                        emailAddressId,
                        args.sudo,
                        emailMessage,
                        emailMessageWithBody = draftEmailMessageList.find { it.id === emailMessage.id },
                    ),
                )
            } else {
                navController.navigate(
                    EmailMessagesFragmentDirections.actionEmailMessagesFragmentToReadEmailMessageFragment(
                        emailAddress,
                        emailAddressId,
                        emailMessage,
                        args.sudo,
                    ),
                )
            }
        }
        binding.emailMessageRecyclerView.adapter = adapter
        binding.emailMessageRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        configureSwipeToDelete()
    }

    /**
     * Sets toolbar items and recycler view to enabled/disabled.
     *
     * @param isEnabled [Boolean] If true, toolbar items and recycler view will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        toolbarMenu.getItem(0)?.isEnabled = isEnabled
        binding.emailMessageRecyclerView.isEnabled = isEnabled
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int = 0) {
        if (textResId != 0) {
            binding.progressText.text = getString(textResId)
        }
        binding.progressBar.visibility = View.VISIBLE
        binding.progressText.visibility = View.VISIBLE
        binding.emailMessageRecyclerView.visibility = View.GONE
        setItemsEnabled(false)
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        if (bindingDelegate.isAttached()) {
            binding.progressBar.visibility = View.GONE
            binding.progressText.visibility = View.GONE
            binding.emailMessageRecyclerView.visibility = View.VISIBLE
            setItemsEnabled(true)
        }
    }

    /** Displays the loading [AlertDialog] indicating that a deletion operation is occurring. */
    private fun showDeleteAlert(@StringRes textResId: Int) {
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
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.emailMessageRecyclerView)
    }

    private fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val emailMessage = emailMessageList[viewHolder.adapterPosition]
        when (selectedEmailFolder.folderName) {
            "TRASH" -> deleteEmailMessage(emailMessage.id)
            "DRAFTS" -> {
                deleteDraftEmailMessage(emailMessage.id)
                draftEmailMessageList.removeAt(viewHolder.adapterPosition)
            }
            else -> moveEmailMessageToTrash(emailMessage.id)
        }
        emailMessageList.removeAt(viewHolder.adapterPosition)
        adapter.notifyItemRemoved(viewHolder.adapterPosition)
    }

    /** Sets the selected folder type. */
    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        val item = parent.getItemAtPosition(pos)
        if (item == FolderTypes.BLOCKLIST.toString()) {
            navController.navigate(
                EmailMessagesFragmentDirections
                    .actionEmailMessagesFragmentToAddressBlocklistFragment(
                        args.sudo,
                        emailAddressId,
                        emailAddress,
                    ),
            )
        } else if (item == FolderTypes.DRAFTS.toString()) {
            selectedEmailFolder = EmailFolder(
                id = "DRAFT_FOLDER",
                owner = "draftEmailOwnerId",
                owners = listOf(Owner("draftOwnerEmailId", "DRAFT")),
                emailAddressId = emailAddressId,
                folderName = FolderTypes.DRAFTS.toString(),
                size = 1.0,
                unseenCount = 1,
                version = 1,
                createdAt = Date(),
                updatedAt = Date(),
            )
            listEmailMessages(item.toString(), CachePolicy.CACHE_ONLY)
        } else {
            selectedEmailFolder = emailFoldersList.find { it.folderName == item.toString() }!!
            listEmailMessages(selectedEmailFolder.id, CachePolicy.REMOTE_ONLY)
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        /* no-op */
    }

    /** Retrieves the list of [DraftEmailMessageMetadata] for the email address */
    private suspend fun retrieveDraftEmailMessages(): List<DraftEmailMessageWithContent> {
        val draftEmailMessagesMetadata =
            app.sudoEmailClient.listDraftEmailMessageMetadata(emailAddressId)

        return draftEmailMessagesMetadata.map { draftMetadata ->
            app.sudoEmailClient.getDraftEmailMessage(
                GetDraftEmailMessageInput(
                    draftMetadata.id,
                    emailAddressId,
                ),
            )
        }
    }

    /**
     * Create a dummy [EmailMessage] object based on a [DraftEmailMessageWithContent].
     *
     * @param draft [DraftEmailMessageWithContent] The draft email message content.
     */
    private fun transformDraftToEmailMessage(draft: DraftEmailMessageWithContent): EmailMessage {
        val rfc822Message = Rfc822MessageParser.parseRfc822Data(draft.rfc822Data)
        return EmailMessage(
            id = draft.id,
            clientRefId = "draftClientRefId",
            owner = "draftOwnerId",
            owners = listOf(Owner(id = "draftOwnerId", issuer = "drafts")),
            emailAddressId = emailAddressId,
            folderId = FolderTypes.DRAFTS.toString(),
            previousFolderId = null,
            seen = true,
            direction = Direction.OUTBOUND,
            state = State.UNDELIVERED,
            version = 1,
            sortDate = draft.updatedAt,
            createdAt = draft.updatedAt,
            updatedAt = draft.updatedAt,
            size = draft.rfc822Data.size.toDouble(),
            from = listOf(EmailMessage.EmailAddress(rfc822Message.from[0])),
            to = rfc822Message.to.map { EmailMessage.EmailAddress(it) },
            cc = rfc822Message.cc.map { EmailMessage.EmailAddress(it) },
            bcc = rfc822Message.bcc.map { EmailMessage.EmailAddress(it) },
            replyTo = listOf(EmailMessage.EmailAddress(rfc822Message.from[0])),
            subject = rfc822Message.subject,
            sentAt = null,
            receivedAt = null,
            hasAttachments = false,
        )
    }

    /** Create a dummy [SimplifiedEmailMessage] object based on a [DraftEmailMessageWithContent] */
    private fun transformDraftToSimplifiedEmailMessage(draft: DraftEmailMessageWithContent): SimplifiedEmailMessage {
        val rfc822Message = Rfc822MessageParser.parseRfc822Data(draft.rfc822Data)
        return SimplifiedEmailMessage(
            id = draft.id,
            from = rfc822Message.from,
            to = rfc822Message.to,
            cc = rfc822Message.cc,
            bcc = rfc822Message.bcc,
            subject = rfc822Message.subject,
            body = rfc822Message.body,
            isDraft = true,
        )
    }
}
