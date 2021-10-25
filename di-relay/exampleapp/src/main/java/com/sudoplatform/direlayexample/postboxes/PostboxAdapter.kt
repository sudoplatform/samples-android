/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.postboxes

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * A [RecyclerView.Adapter] used to feed Postbox ID data to the list view and handle creation
 * and replacement of views for the Postboxes.
 *
 * @property items List of Postbox IDs to display.
 * @property itemSelectedListener Callback which listens for list item select events.
 */
class PostboxAdapter(
    private val items: List<String>,
    private val itemSelectedListener: (String) -> Unit
) : RecyclerView.Adapter<PostboxViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostboxViewHolder {
        return PostboxViewHolder.inflate(
            parent
        )
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onBindViewHolder(holder: PostboxViewHolder, position: Int) {
        val postbox = items[position]
        holder.bind(postbox)

        holder.itemView.setOnClickListener {
            itemSelectedListener(postbox)
        }
    }
}
