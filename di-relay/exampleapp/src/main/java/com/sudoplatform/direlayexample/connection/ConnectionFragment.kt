/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.connection

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.direlayexample.App
import com.sudoplatform.direlayexample.R
import com.sudoplatform.direlayexample.connection.ConnectionTransformer.toListDisplayItems
import com.sudoplatform.direlayexample.connection.ConnectionViewHolder.Companion.TEMPORARY_SENDING_MESSAGE_PREFIX
import com.sudoplatform.direlayexample.connection.connectiondetails.ConnectionDetailsFragment
import com.sudoplatform.direlayexample.databinding.FragmentConnectionBinding
import com.sudoplatform.direlayexample.establishconnection.invite.InviteFragment
import com.sudoplatform.direlayexample.establishconnection.scaninivitation.ScanInvitationFragment
import com.sudoplatform.direlayexample.postboxes.PostboxesFragment
import com.sudoplatform.direlayexample.showAlertDialog
import com.sudoplatform.direlayexample.util.ObjectDelegate
import com.sudoplatform.sudodirelay.SudoDIRelayClient
import com.sudoplatform.sudodirelay.subscription.DIRelayEventSubscriber
import com.sudoplatform.sudodirelay.types.PostboxDeletionResult
import com.sudoplatform.sudodirelay.types.RelayMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.lang.Exception
import java.util.Date
import java.util.UUID
import kotlin.coroutines.CoroutineContext

/**
 * This [ConnectionFragment] presents a list of [RelayMessage]s associated with the selected postbox
 *  and an option to send an encrypted message to the peer the selected postbox is connected with.
 *
 * Links From:
 *  - [PostboxesFragment]: A user chooses a postbox from the list which will show this view with the list
 *   of [RelayMessage]s that have been sent to and from this postbox.
 *  - [ScanInvitationFragment]: A user scanned an invitation from a peer and established a connection.
 *  - [InviteFragment]: A user created an invitation and received a response from the connecting peer,
 *   and therefore established a connection.
 *
 * Links To:
 *  - [ConnectionDetailsFragment]: The user taps the "Details" button and the [ConnectionDetailsFragment]
 *   is presented such that they can see more details about the connection of this selected postbox.
 */
class ConnectionFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentConnectionBinding>()
    private val binding by bindingDelegate

    /** Toolbar [Menu] displaying title and compose button. */
    private lateinit var toolbarMenu: Menu

    /** A reference to the [RecyclerView.Adapter] handling [RelayMessage] data. */
    private lateinit var adapter: ConnectionAdapter

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** A mutable list of [RelayMessage]s. */
    private var relayMessageList = mutableListOf<RelayMessage>()

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: ConnectionFragmentArgs by navArgs()

    /** The selected postbox/connection Identifier containing the messages. */
    private lateinit var connectionId: String

    /** The identifier of the peer that a connection was established with */
    private lateinit var peerConnectionId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        connectionId = args.connectionId
        bindingDelegate.attach(FragmentConnectionBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = connectionId
            inflateMenu(R.menu.nav_menu_with_details_button)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.details -> {
                        navController.navigate(
                            ConnectionFragmentDirections.actionConnectionFragmentToConnectionDetailsFragment(
                                connectionId
                            )
                        )
                    }
                }
                true
            }
            toolbarMenu = menu
        }
        app = requireActivity().application as App
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView()
        navController = Navigation.findNavController(view)

        binding.sendButton.setOnClickListener {
            binding.composeMessageEditText.text?.let {
                if (!binding.composeMessageEditText.text.isNullOrBlank()) {
                    sendEncryptedMessageToPeer(it.toString())
                }
            }
        }
        checkPeerConnection()
        listRelayMessages()
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
        subscribeToRelayMessages()
    }

    override fun onPause() {
        super.onPause()
        unsubscribeFromRelayEvents()
    }

    /**
     * Attempts to get peer's connectionId from internal storage. Else displays error.
     */
    private fun checkPeerConnection() {
        launch {
            if (app.connectionsStorage.isPeerConnected(connectionId)) {
                peerConnectionId =
                    app.connectionsStorage.getPeerConnectionIdForConnection(connectionId)!!
            } else {
                showAlertDialog(
                    titleResId = R.string.generic_failed_title,
                    message = "No peer connection could be found for this postbox",
                    negativeButtonResId = android.R.string.ok
                )
            }
        }
    }

    /**
     * Use the relay client to get messages for this [connectionId]. On getting messages,
     * attempt to decrypt them and add to the recycler view to be displayed.
     */
    private fun listRelayMessages() {
        launch {
            showLoading()
            try {
                val relayMessages = withContext(Dispatchers.IO) {
                    app.diRelayClient.listMessages(connectionId)
                }
                relayMessageList.clear()
                relayMessageList.addAll(attemptDecryptRelayMessages(relayMessages))

                adapter.submitList(relayMessageList.toListDisplayItems(peerConnectionId))
            } catch (e: SudoDIRelayClient.DIRelayException) {
                showAlertDialog(
                    titleResId = R.string.list_relay_message_failed,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { listRelayMessages() },
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideLoading()
        }
    }

    /**
     * Attempt to create a new [RelayMessage] from [msg], with the cipher text unpacked/decrypted.
     * If it cannot be decrypted, return the original [msg].
     *
     * @param msg the [RelayMessage] to attempt to unpack.
     * @return copy of [msg] with decrypted cipher text, or just the original [msg] if failed to decrypt.
     */
    private fun attemptDecryptRelayMessage(msg: RelayMessage): RelayMessage {
        return try {
            msg.copy(
                cipherText = app.keyManagement.unpackEncryptedMessageForConnection(
                    connectionId,
                    msg.cipherText
                )
            )
        } catch (e: Exception) {
            app.logger.error("failed to decrypt message: \n\t$msg\n\t${e.localizedMessage}")
            msg
        }
    }

    /**
     * Attempts to decrypt a list of relay messages [messages].
     *
     * @param messages the list of [RelayMessage]s to attempt to decrypt.
     * @return a copy of the list with their cipher text decrypted, or with original cipher text if
     *  it could not be decrypted.
     */
    private fun attemptDecryptRelayMessages(messages: List<RelayMessage>): List<RelayMessage> {
        return messages.map { msg -> attemptDecryptRelayMessage(msg) }
    }

    /**
     * Attempts to post a packed/encrypted message with the contents of [msg] to the peer. On
     * success, the message is added to the recycler view as an outbound message.
     *
     * @param msg the message to be packed and sent to the peer.
     */
    private fun sendEncryptedMessageToPeer(msg: String) {
        val peerPostboxURL = app.diRelayClient.getPostboxEndpoint(peerConnectionId)

        launch {
            binding.composeMessageEditText.text?.clear()
            val temporaryMessage = displayTemporarySendingMessage(msg)
            val encryptedMessage = app.keyManagement.packEncryptedMessageForPeer(
                peerConnectionId,
                msg
            )
            if (postMessageToEndpoint(encryptedMessage, peerPostboxURL)) {
                // storeMessage in relay postbox encrypted for self (with own public key)
                val storedMsg = storeSelfEncryptedMessageInPostbox(msg)

                relayMessageList.add(attemptDecryptRelayMessage(storedMsg))
                adapter.submitList(relayMessageList.toListDisplayItems(peerConnectionId))
            } else {
                showAlertDialog(
                    titleResId = R.string.post_message_failed,
                    message = "failed to send message",
                    negativeButtonResId = android.R.string.ok

                )
            }
            removeRelayMessage(temporaryMessage)
        }
    }

    /**
     * Does a HTTP POST to the supplied [endpoint], containing the string body of [msg].
     *
     * @return whether the POST succeeded or not
     */
    private suspend fun postMessageToEndpoint(msg: String, endpoint: String): Boolean {
        val postRequest = Request.Builder()
            .url(endpoint)
            .post(msg.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        val client = OkHttpClient()
        val response = withContext(Dispatchers.IO) {
            client.newCall(postRequest).execute()
        }

        return response.code == 200
    }

    /**
     * Creates a mocked [RelayMessage] with the [msg] as cipherText, adds it to the recyclerView
     * and returns it. The functions purpose is strictly for UI/UX reasons as the created RelayMessage
     * is not bound to the relay service in anyway.
     *
     * @param msg the string cipherText to be held by the relayMessage
     * @return the [RelayMessage] added to the list.
     */
    private fun displayTemporarySendingMessage(msg: String): RelayMessage {
        val temporaryMessage = RelayMessage(
            TEMPORARY_SENDING_MESSAGE_PREFIX + UUID.randomUUID().toString(),
            connectionId,
            msg,
            RelayMessage.Direction.OUTBOUND,
            Date()
        )

        relayMessageList.add(temporaryMessage)
        adapter.submitList(relayMessageList.toListDisplayItems(peerConnectionId))

        return temporaryMessage
    }

    /**
     * Removes the specified [relayMessage] from the relayMessageList and updates the recyclerView.
     *
     * @param relayMessage the [RelayMessage] to remove
     */
    private fun removeRelayMessage(relayMessage: RelayMessage) {
        relayMessageList.remove(relayMessage)
        adapter.submitList(relayMessageList.toListDisplayItems(peerConnectionId))
    }

    /**
     * Use the relay client to store a self-encrypted message in the postbox for this [connectionId].
     *
     * @param msg the unencrypted contents of the msg to be stored.
     * @return the [RelayMessage] object that was stored in the postbox.
     */
    private suspend fun storeSelfEncryptedMessageInPostbox(msg: String): RelayMessage {
        val selfEncryptedMessage = app.keyManagement.packEncryptedMessageForPeer(
            connectionId,
            msg
        )
        return app.diRelayClient.storeMessage(connectionId, selfEncryptedMessage)
    }

    /**
     * Use the relay client to subscribe to the [connectionId] postbox. Subscription events
     * are handled by the [relaySubscriber] subscriber.
     */
    private fun subscribeToRelayMessages() {
        launch {
            try {
                withContext(Dispatchers.IO) {
                    app.diRelayClient.subscribeToRelayEvents(
                        connectionId = connectionId,
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
     * A [DIRelayEventSubscriber] object which attempts to decrypt incoming
     * messages and adds them to the recycler view.
     */
    private val relaySubscriber = object : DIRelayEventSubscriber {
        override fun messageIncoming(message: RelayMessage) {
            launch {
                val decryptedMessage = withContext(Dispatchers.IO) {
                    attemptDecryptRelayMessage(message)
                }
                relayMessageList.add(decryptedMessage)
                adapter.submitList(relayMessageList.toListDisplayItems(peerConnectionId))
            }
        }

        override fun connectionStatusChanged(state: DIRelayEventSubscriber.ConnectionState) {}
        override fun postBoxDeleted(update: PostboxDeletionResult) {}
    }

    /**
     * Use the relay client to unsubscribe from live RelayEvent updates.
     */
    private fun unsubscribeFromRelayEvents() {
        launch {
            try {
                withContext(Dispatchers.IO) {
                    app.diRelayClient.unsubscribeToRelayEvents(connectionId)
                }
            } catch (e: SudoDIRelayClient.DIRelayException) {
                app.logger.error("Failed to unsubscribe: $e")
            }
        }
    }

    /**
     * Configures the [RecyclerView] used to display the listed [ConnectionModels.DisplayItem] items.
     */
    private fun configureRecyclerView() {
        adapter = ConnectionAdapter()
        adapter.registerAdapterDataObserver(
            object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    if (bindingDelegate.isAttached()) {
                        binding.relayMessageRecyclerView.layoutManager?.scrollToPosition(adapter.itemCount - 1)
                    }
                }
            }
        )
        binding.relayMessageRecyclerView.adapter = adapter
        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.stackFromEnd = true
        binding.relayMessageRecyclerView.layoutManager = layoutManager

        binding.relayMessageRecyclerView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (bindingDelegate.isAttached()) {
                binding.relayMessageRecyclerView.layoutManager?.scrollToPosition(adapter.itemCount - 1)
            }
        }
    }

    /**
     * Sets toolbar items and recycler view to enabled/disabled.
     *
     * @param isEnabled If true, toolbar items and recycler view will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        binding.relayMessageRecyclerView.isEnabled = isEnabled
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int = 0) {
        if (textResId != 0) {
            binding.progressText2.text = getString(textResId)
        }
        binding.progressBar3.visibility = View.VISIBLE
        binding.progressText2.visibility = View.VISIBLE
        binding.relayMessageRecyclerView.visibility = View.GONE
        setItemsEnabled(false)
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        if (bindingDelegate.isAttached()) {
            binding.progressBar3.visibility = View.GONE
            binding.progressText2.visibility = View.GONE
            binding.relayMessageRecyclerView.visibility = View.VISIBLE
            setItemsEnabled(true)
        }
    }
}
