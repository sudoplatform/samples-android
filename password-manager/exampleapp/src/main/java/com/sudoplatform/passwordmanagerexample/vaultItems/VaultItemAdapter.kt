/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.vaultItems

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudopasswordmanager.models.VaultItem

/**
 * A [RecyclerView.Adapter] used to feed [VaultLogin] data to the list view and handle creation and
 * replacement of views for the [VaultLogin] data items.
 *
 * @property items List of [VaultLogin] data items to display.
 * @property itemSelectedListener Callback which listens for list item select events.
 */
class VaultItemAdapter(
    private val items: List<VaultItem>,
    private val itemSelectedListener: (VaultItem) -> Unit
) : RecyclerView.Adapter<VaultItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VaultItemViewHolder {
        return VaultItemViewHolder.inflate(
            parent
        )
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onBindViewHolder(holder: VaultItemViewHolder, position: Int) {
        val item = items[position]

        holder.bind(item)
        holder.itemView.setOnClickListener {
            itemSelectedListener(item)
        }
    }
}
