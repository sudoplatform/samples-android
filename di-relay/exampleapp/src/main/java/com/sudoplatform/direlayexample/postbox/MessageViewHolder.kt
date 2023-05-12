/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.postbox

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.direlayexample.databinding.LayoutItemCellBinding
import com.sudoplatform.sudodirelay.types.Message

/**
 * A [RecyclerView.ViewHolder] used to describe the PostMessage item view and metadata about its
 * place within the [RecyclerView].
 *
 * The item view contains a name label of the postbox identifier and the service endpoint.
 *
 * @property binding The Message item view binding component.
 */
class MessageViewHolder(private val binding: LayoutItemCellBinding) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun inflate(parent: ViewGroup): MessageViewHolder {
            val binding = LayoutItemCellBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ),
                parent,
                false
            )
            return MessageViewHolder(binding)
        }
    }

    fun bind(message: Message) {
        binding.name.text = message.id
    }
}
