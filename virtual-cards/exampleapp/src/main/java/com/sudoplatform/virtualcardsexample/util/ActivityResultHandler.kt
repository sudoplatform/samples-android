/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.util

import android.content.Intent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handler for notifying the library that an activity result was received.
 */
interface ActivityResultHandler {

    /**
     * Listener that can be attached to listen for results
     */
    interface ActivityResultListener {

        /**
         * Called when the handler is notified of an activity result.
         */
        fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    }

    /**
     * Activities can notify this handler whenever onActivityResult is called triggering listeners.
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)

    /**
     * Add a listener to this handler with a given [key]
     */
    fun addListener(key: String, listener: ActivityResultListener)

    /**
     * Remove a listener from this handler by [key]
     */
    fun removeListener(key: String)
}

@Singleton
class DefaultActivityResultHandler @Inject constructor() : ActivityResultHandler {

    private var listeners = mutableMapOf<String, ActivityResultHandler.ActivityResultListener>()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            listeners.forEach { (_, listener) -> listener.onActivityResult(requestCode, resultCode, data) }
        } catch (e: Exception) {
        }
    }

    override fun addListener(key: String, listener: ActivityResultHandler.ActivityResultListener) {
        listeners += key to listener
    }

    override fun removeListener(key: String) {
        listeners -= key
    }
}
