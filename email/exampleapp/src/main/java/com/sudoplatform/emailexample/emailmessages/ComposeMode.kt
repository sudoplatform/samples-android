/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

/**
 * What the compose screen starts from. Every mode but [NEW] also takes the ID of the message
 * being replied to or forwarded, or of the draft being edited.
 *
 * @property routeValue the value of the compose route's `mode` argument.
 */
enum class ComposeMode(
    val routeValue: String,
) {
    NEW("new"),
    REPLY("reply"),
    FORWARD("forward"),
    DRAFT("draft"),
    ;

    companion object {
        /** The mode for a `mode` route argument, or [NEW] if it is missing or unrecognised. */
        fun fromRouteValue(value: String?): ComposeMode = entries.firstOrNull { it.routeValue == value } ?: NEW
    }
}
