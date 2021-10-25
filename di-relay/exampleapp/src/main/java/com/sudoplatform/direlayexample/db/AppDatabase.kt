/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.direlayexample.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PeerConnection::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun connectionDao(): PeerConnectionDao
}
