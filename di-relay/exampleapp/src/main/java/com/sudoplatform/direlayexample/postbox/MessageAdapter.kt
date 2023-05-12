/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.postbox

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudodirelay.types.Message

/**
 * A [RecyclerView.Adapter] used to feed message ID data to the list view and handle creation
 * and replacement of views for the Messages.
 *
 * @property items List of message IDs to display.
 * @property itemSelectedListener Callback which listens for list item select events.
 */
class MessageAdapter(
    private val items: List<Message>,
    private val itemSelectedListener: (Message) -> Unit
) : RecyclerView.Adapter<MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        return MessageViewHolder.inflate(
            parent
        )
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = items[position]
        holder.bind(message)

        holder.itemView.setOnClickListener {
            itemSelectedListener(message)
        }
    }
}
