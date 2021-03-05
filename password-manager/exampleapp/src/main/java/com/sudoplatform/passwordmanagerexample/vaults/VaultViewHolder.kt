/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.vaults

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.passwordmanagerexample.databinding.LayoutItemCellBinding

/**
 * A [RecyclerView.ViewHolder] used to describe the [Vault] item view and metadata about its place
 * within the [RecyclerView].
 *
 * The item view contains a label [TextView] of the [Vault] name.
 *
 * @param binding The [Vault] item view binding.
 */
class VaultViewHolder(
    private val binding: LayoutItemCellBinding
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun inflate(parent: ViewGroup): VaultViewHolder {
            val binding = LayoutItemCellBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return VaultViewHolder(binding)
        }
    }

    fun bind(vaultName: String) {
        binding.name.text = vaultName
    }
}
