/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaddresses

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.emailexample.databinding.LayoutItemCellBinding
import com.sudoplatform.sudoemail.types.EmailAddress

/**
 * A [RecyclerView.ViewHolder] used to describe the [EmailAddress] item view and metadata about its
 * place within the [RecyclerView].
 *
 * The item view contains a name label of the address.
 *
 * @property binding [LayoutItemCellBinding] The [EmailAddress] item view binding component.
 */
class EmailAddressViewHolder(private val binding: LayoutItemCellBinding) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun inflate(parent: ViewGroup): EmailAddressViewHolder {
            val binding = LayoutItemCellBinding.inflate(
                LayoutInflater.from(
                    parent.context,
                ),
                parent,
                false,
            )
            return EmailAddressViewHolder(binding)
        }
    }

    fun bind(emailAddress: EmailAddress) {
        binding.name.text = emailAddress.emailAddress
    }
}
