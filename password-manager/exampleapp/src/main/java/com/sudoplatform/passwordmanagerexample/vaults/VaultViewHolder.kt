/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.vaults

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.passwordmanagerexample.R

/**
 * A [RecyclerView.ViewHolder] used to describe the [Vault] item view and metadata about its place
 * within the [RecyclerView].
 *
 * The item view contains a label [TextView] of the [Vault] name.
 *
 * @param view The [Vault] item view component.
 */
class VaultViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val nameTextView: TextView = view.findViewById(R.id.name)

    companion object {
        fun inflate(parent: ViewGroup): VaultViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return VaultViewHolder(
                inflater.inflate(
                    R.layout.layout_item_cell,
                    parent,
                    false
                )
            )
        }
    }

    fun bind(vaultName: String) {
        nameTextView.text = vaultName
    }
}
