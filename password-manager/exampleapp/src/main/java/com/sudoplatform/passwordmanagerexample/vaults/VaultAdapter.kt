/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.vaults

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.sudopasswordmanager.models.Vault
import java.text.SimpleDateFormat

/**
 * A [RecyclerView.Adapter] used to feed [Vault] data to the list view and handle creation and
 * replacement of views for the [Vault] data items.
 *
 * @property items List of [Vault] data items to display.
 * @property itemSelectedListener Callback which listens for list item select events.
 */
class VaultAdapter(
    context: Context,
    private val items: List<Vault>,
    private val itemSelectedListener: (Vault) -> Unit
) : RecyclerView.Adapter<VaultViewHolder>() {

    private val dateFormat = SimpleDateFormat(context.getString(R.string.vault_label_date_format))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VaultViewHolder {
        return VaultViewHolder.inflate(
            parent
        )
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onBindViewHolder(holder: VaultViewHolder, position: Int) {
        val vault = items[position]
        holder.bind(dateFormat.format(vault.createdAt))

        holder.itemView.setOnClickListener {
            itemSelectedListener(vault)
        }
    }
}
