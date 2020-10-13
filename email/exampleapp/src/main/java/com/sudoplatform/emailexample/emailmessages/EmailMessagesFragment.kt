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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.emailexample.App
import com.sudoplatform.emailexample.MissingFragmentArgumentException
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.createLoadingAlertDialog
import com.sudoplatform.emailexample.emailaddresses.EmailAddressesFragment
import com.sudoplatform.emailexample.showAlertDialog
import com.sudoplatform.emailexample.swipe.SwipeLeftActionHelper
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudoemail.subscription.EmailMessageSubscriber
import com.sudoplatform.sudoemail.types.CachePolicy
import com.sudoplatform.sudoemail.types.EmailAddress
import com.sudoplatform.sudoemail.types.EmailMessage
import java.util.UUID
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_email_messages.*
import kotlinx.android.synthetic.main.fragment_email_messages.progressBar
import kotlinx.android.synthetic.main.fragment_email_messages.progressText
import kotlinx.android.synthetic.main.fragment_email_messages.view.*
import kotlinx.android.synthetic.main.fragment_sudos.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
class EmailMessagesFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** Toolbar [Menu] displaying title and compose button. */
    private lateinit var toolbarMenu: Menu

    /** A reference to the [RecyclerView.Adapter] handling [EmailMessage] data. */
    private lateinit var adapter: EmailMessageAdapter

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private lateinit var loading: AlertDialog

    /** A mutable list of [EmailMessage]s. */
    private var emailMessageList = mutableListOf<EmailMessage>()

    /** The selected Email address used to filter email messages. */
    private lateinit var emailAddress: String

    /** The selected Email address Identifier used to filter email messages. */
    private lateinit var emailAddressId: String

    /** Subscription ID for email messages */
    private val subscriptionId = UUID.randomUUID().toString()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_email_messages, container, false)
        val toolbar = (view.toolbar as Toolbar)
        emailAddress = requireArguments().getString(getString(R.string.email_address))
            ?: throw MissingFragmentArgumentException("Email address missing")
        emailAddressId = requireArguments().getString(getString(R.string.email_address_id))
            ?: throw MissingFragmentArgumentException("Email address id missing")
        toolbar.title = getString(R.string.email_messages)

        toolbar.inflateMenu(R.menu.nav_menu_with_compose_button)
        toolbar.setOnMenuItemClickListener {
            when (it?.itemId) {
                R.id.compose -> {
                    val bundle = bundleOf(
                        getString(R.string.email_address) to emailAddress,
                        getString(R.string.email_address_id) to emailAddressId
                    )
                    navController.navigate(R.id.action_emailMessagesFragment_to_sendEmailMessageFragment, bundle)
                }
            }
            true
        }
        toolbarMenu = toolbar.menu
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView(view)
        navController = Navigation.findNavController(view)

        listEmailMessages(CachePolicy.REMOTE_ONLY)
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
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
     * List [EmailMessage]s from the [SudoEmailClient].
     *
     * @param cachePolicy Option of either retrieving [EmailMessage] data from the cache or network.
     */
    private fun listEmailMessages(cachePolicy: CachePolicy) {
        val app = requireActivity().application as App
        launch {
            try {
                showLoading()
                val emailMessages = withContext(Dispatchers.IO) {
                    app.sudoEmailClient.listEmailMessages(cachePolicy = cachePolicy)
                }
                emailMessageList.clear()
                val address = EmailMessage.EmailAddress(emailAddress)
                for (emailMessage in emailMessages.items) {
                    if (emailMessage.emailAddressId == emailAddressId ||
                        emailMessage.to.contains(address) ||
                        emailMessage.cc.contains(address) ||
                        emailMessage.bcc.contains(address)) {
                        emailMessageList.add(emailMessage)
                    }
                }
                emailMessageList.sortWith(
                    Comparator { lhs, rhs ->
                        when {
                            lhs.createdAt.before(rhs.createdAt) -> 1
                            lhs.createdAt.after(rhs.createdAt) -> -1
                            else -> 0
                        }
                    }
                )
                adapter.notifyDataSetChanged()
            } catch (e: SudoEmailClient.EmailMessageException) {
                showAlertDialog(
                    titleResId = R.string.list_email_messages_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { listEmailMessages(CachePolicy.REMOTE_ONLY) },
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideLoading()
        }
    }

    /**
     * Delete a selected [EmailMessage] from the [SudoEmailClient].
     *
     * @param id The identifier of the [EmailMessage] to delete.
     */
    private fun deleteEmailMessage(id: String) {
        val app = requireActivity().application as App
        launch {
            try {
                showDeleteAlert(R.string.deleting_email_message)
                withContext(Dispatchers.IO) {
                    app.sudoEmailClient.deleteEmailMessage(id)
                }
                showAlertDialog(
                    titleResId = R.string.success,
                    positiveButtonResId = android.R.string.ok
                )
            } catch (e: SudoEmailClient.EmailMessageException) {
                showAlertDialog(
                    titleResId = R.string.deleting_email_message_failure,
                    message = e.localizedMessage ?: e.toString(),
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideDeleteAlert()
        }
    }

    /** Subscribe to receive live updates as [EmailMessage]s are created and deleted. */
    private fun subscribeToEmailMessages() {
        val app = requireActivity().application as App
        launch {
            try {
                withContext(Dispatchers.IO) {
                    app.sudoEmailClient.subscribeToEmailMessages(
                        id = subscriptionId,
                        subscriber = emailMessageSubscriber
                    )
                }
            } catch (e: SudoEmailClient.EmailMessageException) {
                showAlertDialog(
                    titleResId = R.string.subscribe_email_messages_failure,
                    message = e.localizedMessage ?: "$e",
                    negativeButtonResId = android.R.string.ok,
                    onNegative = {}
                )
            }
        }
    }

    private val emailMessageSubscriber = object : EmailMessageSubscriber {
        override fun emailMessageCreated(emailMessage: EmailMessage) {
            launch(Dispatchers.Main) {
                emailMessageList.add(emailMessage)
                adapter.notifyDataSetChanged()
            }
        }
        override fun emailMessageDeleted(emailMessage: EmailMessage) {
            launch(Dispatchers.Main) {
                emailMessageList.remove(emailMessage)
                adapter.notifyDataSetChanged()
            }
        }
        override fun connectionStatusChanged(state: EmailMessageSubscriber.ConnectionState) {
            if (state == EmailMessageSubscriber.ConnectionState.DISCONNECTED) {
                launch(Dispatchers.Main) {
                    showAlertDialog(
                        titleResId = R.string.subscribe_email_messages_failure,
                        messageResId = R.string.subscribe_lost_connection,
                        positiveButtonResId = android.R.string.ok,
                        onPositive = {}
                    )
                }
            }
        }
    }

    /** Unsubscribe from live [EmailMessage] updates. */
    private fun unsubscribeFromEmailMessages() {
        val app = requireActivity().application as App
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
    private fun configureRecyclerView(view: View) {
        adapter = EmailMessageAdapter(emailMessageList) { emailMessage ->
            val bundle = bundleOf(
                getString(R.string.email_address) to emailAddress,
                getString(R.string.email_address_id) to emailAddressId,
                getString(R.string.email_message) to emailMessage
            )
            navController.navigate(R.id.action_emailMessagesFragment_to_readEmailMessageFragment, bundle)
        }
        view.emailMessageRecyclerView.adapter = adapter
        view.emailMessageRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        configureSwipeToDelete()
    }

    /**
     * Sets toolbar items and recycler view to enabled/disabled.
     *
     * @param isEnabled If true, toolbar items and recycler view will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        toolbarMenu.getItem(0)?.isEnabled = isEnabled
        emailMessageRecyclerView?.isEnabled = isEnabled
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int = 0) {
        if (textResId != 0) {
            progressText.text = getString(textResId)
        }
        progressBar.visibility = View.VISIBLE
        progressText.visibility = View.VISIBLE
        emailMessageRecyclerView?.visibility = View.GONE
        setItemsEnabled(false)
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        progressBar?.visibility = View.GONE
        progressText?.visibility = View.GONE
        emailMessageRecyclerView?.visibility = View.VISIBLE
        setItemsEnabled(true)
    }

    /** Displays the loading [AlertDialog] indicating that a deletion operation is occurring. */
    private fun showDeleteAlert(@StringRes textResId: Int) {
        loading = createLoadingAlertDialog(textResId)
        loading.show()
    }

    /** Dismisses the loading [AlertDialog] indicating that a deletion operation has finished. */
    private fun hideDeleteAlert() {
        loading.dismiss()
    }

    /**
     * Configures the swipe to delete action by listening to [RecyclerView.ViewHolder] swipe events
     * and drawing the swipe view and delete icon.
     *
     * Swiping in from the left will perform a delete operation and remove the item from the view.
     */
    private fun configureSwipeToDelete() {
        val itemTouchCallback = SwipeLeftActionHelper(requireContext(), onSwipedAction = ::onSwiped)
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(emailMessageRecyclerView)
    }

    private fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val emailMessage = emailMessageList[viewHolder.adapterPosition]
        deleteEmailMessage(emailMessage.messageId)
        emailMessageList.removeAt(viewHolder.adapterPosition)
        adapter.notifyItemRemoved(viewHolder.adapterPosition)
    }
}
