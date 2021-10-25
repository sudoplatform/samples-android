/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.direlayexample.connection

import com.sudoplatform.sudodirelay.types.RelayMessage

/**
 * Data classes for use in the [ConnectionFragment].
 */
interface ConnectionModels {
    /**
     * An item that is displayed in the connection fragment recycler view. Either a [Message]
     * or a [PostboxEvent].
     */
    sealed class DisplayItem {
        /**
         * A message item containing a [RelayMessage] to be displayed.
         *
         * @property data the [RelayMessage] to display.
         */
        data class Message(val data: RelayMessage) : DisplayItem()

        /**
         * A postbox event containing some [event] metadata to display.
         *
         * @property event the metadata to display.
         */
        data class PostboxEvent(val event: String) : DisplayItem()
    }
}
