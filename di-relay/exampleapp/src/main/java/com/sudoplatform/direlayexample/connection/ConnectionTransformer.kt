/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.connection

import com.google.gson.Gson
import com.sudoplatform.direlayexample.establishconnection.PeerConnectionExchangeInformation
import com.sudoplatform.sudodirelay.SudoDIRelayClient
import com.sudoplatform.sudodirelay.types.RelayMessage
import java.lang.Exception

/**
 * Transformer methods for transforming objects related to the [ConnectionFragment] and [ConnectionModels]
 */
object ConnectionTransformer {

    /** Event metadata message to display when a peer sent a connection request */
    private const val PEER_SENT_REQUEST = "Peer connection (%s) sent connection request"
    /** Event metadata message to display when a peers connection request is accepted */
    private const val ACCEPTED_CONNECTION = "Accepted connection request from peer"

    /** Event metadata message to display when a peers invitation is scanned */
    private const val SCANNED_PEERS_CODE = "Scanned QR code invitation of peer connection (%s)"
    /** Event metadata message to display when a peers connection request is accepted */
    private const val SENT_REQUEST = "Sent connection request to peer"
    /** Event metadata message to display when a connection is opened with a peer */
    private const val OPENED_CONNECTION = "Opened connection with peer"

    /**
     * Converts a list of [RelayMessage] from the [SudoDIRelayClient] into
     *  [ConnectionModels.DisplayItem]s, such that the [ConnectionViewHolder] can interpret them
     *  and display them in the [ConnectionFragment].
     *
     * @param peerConnectionId The unique string connectionId of the peer these messages are to/from.
     * @return list of [ConnectionModels.DisplayItem] created from the list of [RelayMessage]s
     */
    fun List<RelayMessage>.toListDisplayItems(peerConnectionId: String): List<ConnectionModels.DisplayItem> {
        val displayItems = mutableListOf<ConnectionModels.DisplayItem>()

        val firstItem = firstOrNull()
        var firstItemIsExchangeRequest = false
        try {
            // case where peer sent first request message
            val initialExchange = Gson().fromJson(
                firstItem!!.cipherText,
                PeerConnectionExchangeInformation::class.java
            )
            displayItems.add(
                ConnectionModels.DisplayItem.PostboxEvent(
                    PEER_SENT_REQUEST.format(initialExchange.connectionId)
                )
            )
            displayItems.add(
                ConnectionModels.DisplayItem.PostboxEvent(
                    ACCEPTED_CONNECTION
                )
            )
            firstItemIsExchangeRequest = true
        } catch (e: Exception) {
            // case where request was sent to peer
            displayItems.add(
                ConnectionModels.DisplayItem.PostboxEvent(
                    SCANNED_PEERS_CODE.format(peerConnectionId)
                )
            )
            displayItems.add(
                ConnectionModels.DisplayItem.PostboxEvent(
                    SENT_REQUEST
                )
            )
            displayItems.add(
                ConnectionModels.DisplayItem.PostboxEvent(
                    OPENED_CONNECTION
                )
            )
        }
        forEach { message ->
            if (!firstItemIsExchangeRequest || message != firstItem) {
                displayItems.add(ConnectionModels.DisplayItem.Message(message))
            }
        }
        return displayItems
    }
}
