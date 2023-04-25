/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.virtualcards

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudovirtualcards.types.VirtualCard

/**
 * A [RecyclerView.Adapter] used to feed [VirtualCard] data to the list view and handle creation and
 * replacement of views for the [VirtualCard] data items.
 *
 * @property items List of [VirtualCard] data items to display.
 * @property itemSelectedListener Callback which listens for list item select events.
 */
class VirtualCardAdapter(private val items: List<VirtualCard>, private val itemSelectedListener: (VirtualCard) -> Unit) : RecyclerView.Adapter<VirtualCardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VirtualCardViewHolder {
        return VirtualCardViewHolder.inflate(
            parent
        )
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onBindViewHolder(holder: VirtualCardViewHolder, position: Int) {
        val virtualCard = items[position]
        holder.bind(virtualCard)

        holder.itemView.setOnClickListener {
            itemSelectedListener(virtualCard)
        }
    }
}
