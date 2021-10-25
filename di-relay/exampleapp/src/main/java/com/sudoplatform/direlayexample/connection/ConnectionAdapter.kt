/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.connection

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.direlayexample.databinding.LayoutPostboxMessagesEventCellBinding
import com.sudoplatform.direlayexample.databinding.LayoutRelayMessageCellBinding
import java.lang.IllegalArgumentException

/**
 * A [RecyclerView.Adapter] used to feed [ConnectionModels.DisplayItem] data to the list view and
 * handle creation and replacement of views for the [ConnectionModels.DisplayItem] data items.
 */
class ConnectionAdapter :
    ListAdapter<ConnectionModels.DisplayItem, ConnectionViewHolder>(Differ) {

    private enum class ViewType {
        EVENT,
        MESSAGE
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ConnectionModels.DisplayItem.PostboxEvent -> ViewType.EVENT
            is ConnectionModels.DisplayItem.Message -> ViewType.MESSAGE
        }.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConnectionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ViewType.MESSAGE.ordinal -> ConnectionViewHolder.Message(
                LayoutRelayMessageCellBinding.inflate(inflater, parent, false)
            )
            ViewType.EVENT.ordinal -> ConnectionViewHolder.Event(
                LayoutPostboxMessagesEventCellBinding.inflate(inflater, parent, false)
            )
            else -> throw IllegalArgumentException("unknown viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: ConnectionViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    object Differ : DiffUtil.ItemCallback<ConnectionModels.DisplayItem>() {
        override fun areItemsTheSame(
            oldItem: ConnectionModels.DisplayItem,
            newItem: ConnectionModels.DisplayItem
        ): Boolean =
            when (oldItem) {
                is ConnectionModels.DisplayItem.Message
                -> newItem is ConnectionModels.DisplayItem.Message && oldItem.data.messageId == newItem.data.messageId
                is ConnectionModels.DisplayItem.PostboxEvent
                -> newItem is ConnectionModels.DisplayItem.PostboxEvent && oldItem.event == newItem.event
            }

        override fun areContentsTheSame(
            oldItem: ConnectionModels.DisplayItem,
            newItem: ConnectionModels.DisplayItem
        ): Boolean = oldItem == newItem
    }
}
