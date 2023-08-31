/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.postbox

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.direlayexample.App
import com.sudoplatform.direlayexample.R
import com.sudoplatform.direlayexample.databinding.FragmentPostboxBinding
import com.sudoplatform.direlayexample.register.RegisterFragment
import com.sudoplatform.direlayexample.showAlertDialog
import com.sudoplatform.direlayexample.swipe.SwipeLeftActionHelper
import com.sudoplatform.direlayexample.util.ObjectDelegate
import com.sudoplatform.sudodirelay.SudoDIRelayClient
import com.sudoplatform.sudodirelay.subscription.MessageSubscriber
import com.sudoplatform.sudodirelay.subscription.Subscriber
import com.sudoplatform.sudodirelay.types.Message
import com.sudoplatform.sudodirelay.types.Postbox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.coroutines.CoroutineContext

/**
 * Wrapper class to allow us to pass the message through to the MessageFragment rather than
 * passing it by id and requiring it to be fetched again from the server.
 */
@Parcelize
data class MessageWrapper(
    val message: @RawValue Message
) : Parcelable

/**
 * This [PostboxFragment] presents a view showing the [Postbox.id]s of the users active postboxes,
 * which they can navigate to, and an option to create a new relay postbox.
 *
 * Links From:
 *  - [RegisterFragment]: A user clicked start.
 *
 * Links To:
 *  - [MessageFragment]: If a user clicks on a postbox which has already established a peer
 *   connection. The [MessageFragment] will be presented to allow the user to interact with
 *   the chosen postbox's connection.
 */
class PostboxFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentPostboxBinding>()
    private val binding by bindingDelegate

    /** Toolbar [Menu] displaying title. */
    private lateinit var toolbarMenu: Menu

    /** A reference to the [RecyclerView.Adapter] handling Message data. */
    private lateinit var adapter: MessageAdapter

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** A mutable list of Messages. */
    private var messageList = mutableListOf<Message>()

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: PostboxFragmentArgs by navArgs()

    /** The postbox associated with these messages. */
    private lateinit var postbox: Postbox

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingDelegate.attach(FragmentPostboxBinding.inflate(inflater, container, false))
        app = requireActivity().application as App
        postbox = args.postbox.postbox
        subscribeToRelayMessages()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView()
        navController = Navigation.findNavController(view)
        configureToolbar(binding.toolbar.root)

        binding.serviceEndpoint.text = postbox.serviceEndpoint
        binding.postboxEnabledSwitch.isChecked = postbox.isEnabled

        binding.postboxEnabledSwitch.setOnCheckedChangeListener { _, _ ->
            // do whatever you need to do when the switch is toggled here
            savePostboxEnabled()
        }
        binding.sendMessageButton.setOnClickListener {
            sendMessage()
            reloadMessages()
        }
    }

    override fun onStart() {
        super.onStart()
        subscribeToRelayMessages()
    }

    override fun onStop() {
        unsubscribeFromRelayMessages()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        reloadMessages()
    }

    private fun reloadMessages() {
        launch {
            setItemsEnabled(false)
            showLoading(R.string.loading_messages)

            updateMessagesForPostbox()

            setItemsEnabled(true)
            hideLoading()
        }
    }

    private fun savePostboxEnabled() {
        launch {
            setItemsEnabled(false)
            showLoading(R.string.updating_postbox)

            updatePostbox()

            setItemsEnabled(true)
            hideLoading()
        }
    }

    /**
     * Configures the [RecyclerView] used to display the listed Message items and listens to
     * item select events to navigate to the [MessageFragment] for the selected message
     */
    private fun configureRecyclerView() {
        adapter =
            MessageAdapter(messageList) { message ->
                launch {
                    navigateToMessageDisplay(message)
                }
            }

        binding.messageRecyclerView.adapter = adapter
        binding.messageRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        configureSwipeToDelete()
    }

    /**
     * Navigate to the [MessageFragment] with the
     * [message] as navigation argument.
     *
     * @param message the message to be displayed.
     */
    private fun navigateToMessageDisplay(message: Message) {
        navController.navigate(
            PostboxFragmentDirections.actionPostboxFragmentToMessageFragment(
                message = MessageWrapper(message)
            )
        )
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    /** Configures the toolbar menu. */
    private fun configureToolbar(toolbar: Toolbar) {
        toolbar.inflateMenu(R.menu.general_nav_menu)

        with(toolbar) {
            title = getString(R.string.postbox_messages)
            toolbarMenu = menu
        }
    }

    /**
     * Use an external http utility to send a message to the current postbox. Normally
     * this would be done by an external component to the relay application but it
     * is included here for ease of use.
     */
    private fun sendMessage() {
        launch {
            showLoading(R.string.sending_message)
            try {
                val messageVal = binding.messageText.getText()?.toString() ?: ""
                val postRequest = Request.Builder()
                    .url(postbox.serviceEndpoint)
                    .post(messageVal.toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()
                val client = OkHttpClient()
                val response = withContext(Dispatchers.IO) {
                    client.newCall(postRequest).execute()
                }
                showAlertDialog(
                    titleResId = R.string.success,
                    positiveButtonResId = android.R.string.ok
                )
                binding.messageText.setText("")
            } catch (e: SudoDIRelayClient.DIRelayException) {
                showAlertDialog(
                    titleResId = R.string.message_sending_failed,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { sendMessage() },
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideLoading()
        }
    }

    /**
     * Gets all stored messages from the service for the current postbox
     */
    private suspend fun updateMessagesForPostbox() {
        messageList.clear()
        try {
            messageList.addAll(
                app.diRelayClient.listMessages(100).items
                    .filter { it.postboxId == postbox.id }
            )
            adapter.notifyDataSetChanged()
        } catch (e: Exception) {
            showAlertDialog(
                titleResId = R.string.fetch_messages_failure,
                message = e.localizedMessage ?: "$e",
                negativeButtonResId = android.R.string.ok
            )
        }
    }

/**
     * Updates the postbox with the current state as represented by the application
     */
    private suspend fun updatePostbox() {
        try {
            app.diRelayClient.updatePostbox(postbox.id, binding.postboxEnabledSwitch.isChecked)
        } catch (e: Exception) {
            showAlertDialog(
                titleResId = R.string.update_postbox_failure,
                message = e.localizedMessage ?: "$e",
                negativeButtonResId = android.R.string.ok
            )
        }
    }

    /**
     * Configures the swipe to delete action by listening to [RecyclerView.ViewHolder] swipe events
     * and drawing the swipe view and delete icon.
     *
     * Swiping in from the left will perform a delete operation and remove the item from the view.
     */
    private fun configureSwipeToDelete() {
        val itemTouchCallback = SwipeLeftActionHelper(requireContext(), onSwipedAction = ::onSwiped)
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.messageRecyclerView)
    }

    private fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        launch {
            val message = messageList[viewHolder.adapterPosition]
            deleteMessage(message.id)
            messageList.removeAt(viewHolder.adapterPosition)
            adapter.notifyItemRemoved(viewHolder.adapterPosition)
        }
    }

    /**
     * Calls the relay client to delete the message with id of [messageId].
     *
     * @param messageId The identifier of the message to be deleted
     */
    private suspend fun deleteMessage(messageId: String) {
        try {
            showLoading(R.string.delete_message_pending)
            app.diRelayClient.deleteMessage(messageId)
        } catch (e: SudoDIRelayClient.DIRelayException) {
            showAlertDialog(
                titleResId = R.string.message_delete_failed,
                message = e.localizedMessage ?: "$e",
                negativeButtonResId = android.R.string.ok
            )
        }
        hideLoading()
    }

    /**
     * Sets create postbox button to enabled/disabled
     *
     * @param isEnabled If true, create postbox button will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        binding.sendMessageButton.isEnabled = isEnabled
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int = 0) {
        binding.sendMessageProgressText.setText(textResId)
        binding.progressBar3.visibility = View.VISIBLE
        binding.sendMessageProgressText.visibility = View.VISIBLE
        setItemsEnabled(false)
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        if (bindingDelegate.isAttached()) {
            binding.progressBar3.visibility = View.GONE
            binding.sendMessageProgressText.visibility = View.GONE
            setItemsEnabled(true)
        }
    }

    /**
     * Use the relay client to subscribe to relay events. Subscription events
     * are handled by the [relaySubscriber] subscriber.
     */
    private fun subscribeToRelayMessages() {
        launch {
            try {
                withContext(Dispatchers.IO) {
                    app.diRelayClient.subscribeToRelayEvents(
                        subscriberId = postbox.id,
                        subscriber = relaySubscriber
                    )
                }
            } catch (e: SudoDIRelayClient.DIRelayException) {
                showAlertDialog(
                    titleResId = R.string.subscription_failed,
                    message = e.localizedMessage ?: "$e",
                    negativeButtonResId = android.R.string.ok,
                    onNegative = {}
                )
            }
        }
    }

    /**
     * A [Subscriber] object which notifies the view that a new message is available.
     */
    private val relaySubscriber = object : MessageSubscriber {
        override fun messageCreated(message: Message) {
            launch {
                // This could be managed by explicitly adding the new message to the postbox display
                // but for now we are 'simply' asking for a full refresh if the message is for us.
                if (message.postboxId == postbox.id) {
                    updateMessagesForPostbox()
                }
            }
        }

        override fun connectionStatusChanged(state: Subscriber.ConnectionState) {}
    }

    /**
     * Use the relay client to unsubscribe from live RelayService events.
     */
    private fun unsubscribeFromRelayMessages() {
        launch {
            try {
                withContext(Dispatchers.IO) {
                    app.diRelayClient.unsubscribeToRelayEvents(postbox.id)
                }
            } catch (e: SudoDIRelayClient.DIRelayException) {
                app.logger.error("Failed to unsubscribe: $e")
            }
        }
    }
}
