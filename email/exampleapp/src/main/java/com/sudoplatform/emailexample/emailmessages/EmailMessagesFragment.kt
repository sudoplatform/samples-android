/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
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
import com.sudoplatform.emailexample.databinding.FragmentEmailMessagesBinding
import com.sudoplatform.emailexample.emailaddresses.EmailAddressesFragment
import com.sudoplatform.emailexample.emailfolders.EmailFolderAdapter
import com.sudoplatform.emailexample.emailfolders.SpecialFolderTabLabels
import com.sudoplatform.emailexample.showAlertDialog
import com.sudoplatform.emailexample.swipe.SwipeLeftActionHelper
import com.sudoplatform.emailexample.util.ObjectDelegate
import com.sudoplatform.emailexample.util.Rfc822MessageParser
import com.sudoplatform.emailexample.util.SimplifiedEmailMessage
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudoemail.subscription.EmailMessageSubscriber
import com.sudoplatform.sudoemail.subscription.Subscriber
import com.sudoplatform.sudoemail.types.Direction
import com.sudoplatform.sudoemail.types.DraftEmailMessageMetadata
import com.sudoplatform.sudoemail.types.DraftEmailMessageWithContent
import com.sudoplatform.sudoemail.types.EmailAddress
import com.sudoplatform.sudoemail.types.EmailFolder
import com.sudoplatform.sudoemail.types.EmailMessage
import com.sudoplatform.sudoemail.types.EncryptionStatus
import com.sudoplatform.sudoemail.types.ListAPIResult
import com.sudoplatform.sudoemail.types.Owner
import com.sudoplatform.sudoemail.types.State
import com.sudoplatform.sudoemail.types.inputs.DeleteCustomEmailFolderInput
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

    /** A list of names for folder tabs */
    private var folderTabLabels = mutableListOf<String>()

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

    /** The display name associated with the selected Email address. */
    private lateinit var emailDisplayName: String

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
                                    emailAddress = emailAddress,
                                    emailDisplayName = emailDisplayName,
                                    emailAddressId = emailAddressId,
                                ),
                        )
                    }
                    R.id.settings -> {
                        navController.navigate(
                            EmailMessagesFragmentDirections
                                .actionEmailMessagesFragmentToEmailAddressSettingsFragment(
                                    emailAddressId,
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
        emailDisplayName = args.emailDisplayName.toString()
        emailAddressId = args.emailAddressId
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView()
        navController = Navigation.findNavController(view)

        binding.foldersSpinner.onItemSelectedListener = this
        listEmailFolders()

        foldersAdapter = EmailFolderAdapter(
            requireContext(),
            folderTabLabels,
            onDeleteMessages = { deleteAllEmailMessages() },
            onDeleteCustomFolder = { name ->
                showAlertDialog(
                    titleResId = R.string.confirm_delete_custom_folder_title,
                    messageResId = R.string.confirm_delete_custom_folder,
                    positiveButtonResId = android.R.string.ok,
                    onPositive = { deleteCustomEmailFolder(name) },
                    negativeButtonResId = android.R.string.cancel,
                )
            },
            onEditCustomFolder = { name ->
                val customFolder = emailFoldersList.find { folder -> folder.customFolderName == name }!!
                navController.navigate(
                    EmailMessagesFragmentDirections
                        .actionEmailMessagesFragmentToEditCustomFolderFragment(
                            emailAddressId,
                            emailAddress,
                            customFolderId = customFolder.id,
                            customFolderName = name,
                        ),
                )
            },
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
        binding.foldersSpinner.setSelection(0)
        subscribeToEmailMessages()
    }

    override fun onPause() {
        super.onPause()
        unsubscribeFromEmailMessages()
    }

    /**
     * List [EmailFolder]s from the [SudoEmailClient].
     *
     */
    private fun listEmailFolders() {
        launch {
            showLoading()
            try {
                val emailFolders = withContext(Dispatchers.IO) {
                    val input = ListEmailFoldersForEmailAddressIdInput(
                        emailAddressId = emailAddressId,
                    )
                    app.sudoEmailClient.listEmailFoldersForEmailAddressId(input)
                }
                emailFoldersList.clear()
                emailFoldersList.addAll(emailFolders.items)
                folderTabLabels.clear()
                folderTabLabels.addAll(
                    emailFolders.items.map { folder ->
                        if (folder.customFolderName?.isNotEmpty() == true) {
                            folder.customFolderName.toString()
                        } else {
                            folder.folderName
                        }
                    },
                )
                folderTabLabels.addAll(SpecialFolderTabLabels.entries.map { it.displayName })
                foldersAdapter.notifyDataSetChanged()
            } catch (e: SudoEmailClient.EmailFolderException) {
                showAlertDialog(
                    titleResId = R.string.list_email_folders_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { listEmailFolders() },
                    negativeButtonResId = android.R.string.cancel,
                )
            }
            if (bindingDelegate.isAttached()) {
                binding.filter.visibility = View.VISIBLE
            }
            hideLoading()
        }
    }

    private fun deleteCustomEmailFolder(folderName: String) {
        launch {
            showLoading()
            try {
                val folderToDelete = emailFoldersList.find { folder -> folder.customFolderName == folderName }
                if (folderToDelete != null) {
                    app.sudoEmailClient.deleteCustomEmailFolder(
                        DeleteCustomEmailFolderInput(
                            emailAddressId = emailAddressId,
                            emailFolderId = folderToDelete.id,
                        ),
                    )
                    listEmailFolders()
                    foldersAdapter.notifyDataSetChanged()
                }
            } catch (e: SudoEmailClient.EmailFolderException) {
                showAlertDialog(
                    titleResId = R.string.something_wrong,
                    message = e.localizedMessage ?: e.toString(),
                    positiveButtonResId = R.string.try_again,
                    onPositive = { deleteEmailMessage(folderName) },
                    negativeButtonResId = android.R.string.cancel,
                )
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * List [EmailMessage]s from the [SudoEmailClient].
     *
     * @param emailFolderId [String] The identifier of the email folder assigned to the email
     *  messages to retrieve.
     */
    private fun listEmailMessages(emailFolderId: String) {
        launch {
            try {
                showLoading()
                if (emailFolderId == SpecialFolderTabLabels.DRAFTS.displayName) {
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
                            null,
                        )
                        app.sudoEmailClient.listEmailMessagesForEmailFolderId(input)
                    }

                    when (emailMessages) {
                        is ListAPIResult.Success -> {
                            emailMessageList.clear()
                            val address = EmailMessage.EmailAddress(emailAddress, emailDisplayName)
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
                Toast.makeText(context, getString(R.string.deleted), Toast.LENGTH_SHORT).show()
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
     * Delete all [EmailMessage]s from the [SudoEmailClient].
     */
    private fun deleteAllEmailMessages() {
        launch {
            try {
                showDeleteAlert(R.string.deleting_all_email_messages)
                withContext(Dispatchers.IO) {
                    app.sudoEmailClient.deleteEmailMessages(emailMessageList.map { it.id })
                }
                Toast.makeText(context, getString(R.string.deleted), Toast.LENGTH_SHORT).show()
            } catch (e: SudoEmailClient.EmailMessageException) {
                showAlertDialog(
                    titleResId = R.string.deleting_all_email_messages_failure,
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
                Toast.makeText(context, getString(R.string.deleted), Toast.LENGTH_SHORT).show()
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

    /** Subscribe to receive live updates as [EmailMessage]s are created, updated and deleted. */
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

        override fun emailMessageChanged(emailMessage: EmailMessage, type: EmailMessageSubscriber.ChangeType) {
            launch(Dispatchers.Main) {
                when (type) {
                    EmailMessageSubscriber.ChangeType.CREATED -> {
                        if (
                            (emailMessage.direction == Direction.INBOUND && selectedEmailFolder.id.contains("INBOX")) ||
                            (emailMessage.direction == Direction.OUTBOUND && selectedEmailFolder.id.contains("SENT"))
                        ) {
                            emailMessageList.add(emailMessage)
                        }
                    }
                    EmailMessageSubscriber.ChangeType.UPDATED -> {
                        val index = emailMessageList.indexOfFirst { it.id == emailMessage.id }
                        emailMessageList[index] = emailMessage
                    }
                    EmailMessageSubscriber.ChangeType.DELETED -> {
                        emailMessageList.remove(emailMessage)
                    }
                }
                adapter.notifyDataSetChanged()
            }
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
            if (selectedEmailFolder.folderName == SpecialFolderTabLabels.DRAFTS.displayName) {
                navController.navigate(
                    EmailMessagesFragmentDirections.actionEmailMessagesFragmentToSendEmailMessageFragment(
                        emailAddress = emailAddress,
                        emailDisplayName = emailDisplayName,
                        emailAddressId = emailAddressId,
                        emailMessage = emailMessage,
                        emailMessageWithBody = draftEmailMessageList.find { it.id === emailMessage.id },
                    ),
                )
            } else {
                navController.navigate(
                    EmailMessagesFragmentDirections.actionEmailMessagesFragmentToReadEmailMessageFragment(
                        emailAddress = emailAddress,
                        emailDisplayName = emailDisplayName,
                        emailAddressId = emailAddressId,
                        emailMessage = emailMessage,
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
        when (val item = parent.getItemAtPosition(pos)) {
            SpecialFolderTabLabels.BLOCKLIST.displayName -> {
                navController.navigate(
                    EmailMessagesFragmentDirections
                        .actionEmailMessagesFragmentToAddressBlocklistFragment(
                            emailAddressId,
                            emailAddress,
                        ),
                )
            }
            SpecialFolderTabLabels.DRAFTS.displayName -> {
                selectedEmailFolder = EmailFolder(
                    id = "DRAFT_FOLDER",
                    owner = "draftEmailOwnerId",
                    owners = listOf(Owner("draftOwnerEmailId", "DRAFT")),
                    emailAddressId = emailAddressId,
                    folderName = SpecialFolderTabLabels.DRAFTS.displayName,
                    size = 1.0,
                    unseenCount = 1,
                    version = 1,
                    createdAt = Date(),
                    updatedAt = Date(),
                )
                listEmailMessages(item.toString())
            }
            SpecialFolderTabLabels.CREATE.displayName -> {
                navController.navigate(
                    EmailMessagesFragmentDirections
                        .actionEmailMessagesFragmentToCreateCustomFolderFragment(
                            emailAddressId,
                            emailAddress,
                        ),
                )
            }
            else -> {
                selectedEmailFolder = emailFoldersList.find {
                    if (it.customFolderName?.isNotEmpty() == true) {
                        it.customFolderName == item.toString()
                    } else {
                        it.folderName == item.toString()
                    }
                }!!
                listEmailMessages(selectedEmailFolder.id)
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        /* no-op */
    }

    /** Retrieves the list of [DraftEmailMessageMetadata] for the email address */
    private suspend fun retrieveDraftEmailMessages(): List<DraftEmailMessageWithContent> {
        val draftEmailMessagesMetadata =
            app.sudoEmailClient.listDraftEmailMessageMetadataForEmailAddressId(emailAddressId)

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
            folderId = SpecialFolderTabLabels.DRAFTS.displayName,
            previousFolderId = null,
            seen = true,
            repliedTo = false,
            forwarded = false,
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
            encryptionStatus = EncryptionStatus.UNENCRYPTED,
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
