/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.sudos

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudoprofiles.Sudo

/**
 * A [RecyclerView.Adapter] used to feed [Sudo] data to the list view and handle creation and
 * replacement of views for the [Sudo] data items.
 *
 * @property items [List<Sudo>] List of [Sudo] data items to display.
 * @property itemSelectedListener Callback which listens for list item select events.
 */
class SudoAdapter(private val items: List<Sudo>, private val itemSelectedListener: (Sudo) -> Unit) : RecyclerView.Adapter<SudoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SudoViewHolder {
        return SudoViewHolder.inflate(
            parent,
        )
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onBindViewHolder(holder: SudoViewHolder, position: Int) {
        val sudo = items[position]
        sudo.label?.let { holder.bind(it) }

        holder.itemView.setOnClickListener {
            itemSelectedListener(sudo)
        }
    }
}
