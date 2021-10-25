/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.direlayexample.db

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Class for managing internal data storage related to postbox connections.
 * This class handles CRUD operations for postbox connectionIds and the mapped peer connectionIds.
 *
 * @param context the application context
 */
class PostboxConnectionsStorage(context: Context) {
    private val dao: PeerConnectionDao = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "database"
    ).build().connectionDao()

    /**
     * Get all postbox connectionIds that are stored on this device.
     *
     * @return List of string connectionIds that this device owns.
     */
    suspend fun getAllConnectionIds(): List<String> {
        return withContext(Dispatchers.IO) {
            dao.getAll().map { it.myConnectionID }
        }
    }

    /**
     * Returns whether a peerConnectionId is stored against the provided [connectionId].
     *
     * @param connectionId the connectionId to check for a mapped peerConnectionId
     * @return true if there is a peerConnectionId mapped to the [connectionId], else false.
     */
    suspend fun isPeerConnected(connectionId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                dao.findByConnectionID(connectionId).peerConnectionID != null
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Attempts to get the connectionId of the peer that is mapped to the provided [connectionId].
     *
     * @param connectionId the connectionId to find the mapped peerConnectionId.
     * @return the peer's connectionId or null if the [connectionId] or peerConnectionId doesn't exist.
     */
    suspend fun getPeerConnectionIdForConnection(connectionId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                dao.findByConnectionID(connectionId).peerConnectionID
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Stores a postbox [connectionId] without a mapped peer connectionId.
     *
     * @param connectionId the connectionId to store
     */
    suspend fun storeConnectionId(connectionId: String) {
        withContext(Dispatchers.IO) {
            dao.insertOrReplace(PeerConnection(connectionId))
        }
    }

    /**
     * Maps a [peerConnectionId] to [myConnectionId].
     *
     * @param myConnectionId the connectionId owned by this device.
     * @param peerConnectionId the peers connectionId to map to [myConnectionId].
     */
    suspend fun storePeersConnectionId(myConnectionId: String, peerConnectionId: String) {
        withContext(Dispatchers.IO) {
            dao.insertOrReplace(PeerConnection(myConnectionId, peerConnectionId))
        }
    }

    /**
     * Deletes all stored data related to [connectionId].
     *
     * @param connectionId the connectionId to delete.
     */
    suspend fun deleteConnection(connectionId: String) {
        withContext(Dispatchers.IO) {
            dao.delete(PeerConnection(connectionId))
        }
    }
}