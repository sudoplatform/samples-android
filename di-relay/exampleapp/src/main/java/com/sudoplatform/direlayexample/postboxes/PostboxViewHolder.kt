/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.postboxes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.direlayexample.databinding.LayoutItemCellBinding
import com.sudoplatform.sudodirelay.types.Postbox

/**
 * A [RecyclerView.ViewHolder] used to describe the Postbox item view and metadata about its
 * place within the [RecyclerView].
 *
 * The item view contains a name label of the postbox identifier and the service endpoint.
 *
 * @property binding The Postbox item view binding component.
 */
class PostboxViewHolder(private val binding: LayoutItemCellBinding) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun inflate(parent: ViewGroup): PostboxViewHolder {
            val binding = LayoutItemCellBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ),
                parent,
                false
            )
            return PostboxViewHolder(binding)
        }
    }

    fun bind(postbox: Postbox) {
        binding.name.text = postbox.id
    }
}
