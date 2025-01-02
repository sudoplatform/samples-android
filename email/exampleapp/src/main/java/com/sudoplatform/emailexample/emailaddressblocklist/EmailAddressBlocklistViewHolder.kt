/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaddressblocklist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.emailexample.databinding.LayoutBlockedAddressCellBinding

/**
 * A [RecyclerView.ViewHolder] used to describe the blocked email address item view and metadata
 * about its place within the [RecyclerView].
 *
 * The item view contains a name label of the address and a checkbox.
 *
 * @property binding [LayoutBlockedAddressCellBinding] The blocked email address item view binding
 *  component.
 */
class EmailAddressBlocklistViewHolder(private val binding: LayoutBlockedAddressCellBinding) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun inflate(parent: ViewGroup): EmailAddressBlocklistViewHolder {
            val binding = LayoutBlockedAddressCellBinding.inflate(
                LayoutInflater.from(
                    parent.context,
                ),
                parent,
                false,
            )
            return EmailAddressBlocklistViewHolder(binding)
        }
    }

    fun bind(address: String, setItemSelected: (Boolean) -> Unit) {
        binding.name.text = address
        binding.checkbox.isChecked = false
        binding.checkbox.setOnClickListener {
            setItemSelected(binding.checkbox.isChecked)
        }
    }
}
