/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.addressblocklist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.emailexample.databinding.LayoutBlockedAddressBinding

class AddressBlocklistHolder(private val binding: LayoutBlockedAddressBinding) : RecyclerView.ViewHolder(binding.root) {
    companion object {
        fun inflate(parent: ViewGroup): AddressBlocklistHolder {
            val binding = LayoutBlockedAddressBinding.inflate(
                LayoutInflater.from(
                    parent.context,
                ),
                parent,
                false,
            )
            return AddressBlocklistHolder(binding)
        }
    }

    fun bind(address: String, setItemSelected: (Boolean) -> Unit) {
        binding.textView.text = address
        binding.unblock.isChecked = false
        binding.unblock.setOnClickListener {
            setItemSelected(binding.unblock.isChecked)
        }
    }
}
