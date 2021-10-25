/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.direlayexample.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "peer_connection")
data class PeerConnection(
    @PrimaryKey @ColumnInfo(name = "my_connection_id") val myConnectionID: String,
    @ColumnInfo(name = "peer_connection_id") val peerConnectionID: String? = null
)

@Dao
interface PeerConnectionDao {
    @Query("SELECT * FROM peer_connection")
    fun getAll(): List<PeerConnection>

    @Query("SELECT * FROM peer_connection WHERE my_connection_id LIKE :connectionID LIMIT 1")
    fun findByConnectionID(connectionID: String): PeerConnection

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(vararg connections: PeerConnection)

    @Delete
    fun delete(connection: PeerConnection)
}
