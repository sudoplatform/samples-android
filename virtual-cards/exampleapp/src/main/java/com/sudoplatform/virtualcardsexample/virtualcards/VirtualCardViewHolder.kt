/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.virtualcards

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudovirtualcards.types.CardState
import com.sudoplatform.sudovirtualcards.types.VirtualCard
import com.sudoplatform.virtualcardsexample.R
import com.sudoplatform.virtualcardsexample.databinding.LayoutItemCellBinding

/**
 * A [RecyclerView.ViewHolder] used to describe the [VirtualCard] item view and metadata about its
 * place within the [RecyclerView].
 *
 * The item view contains a label [TextView] of the [VirtualCard] alias.
 *
 * @property binding The [VirtualCard] item view binding component.
 */
class VirtualCardViewHolder(private val binding: LayoutItemCellBinding) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun inflate(parent: ViewGroup): VirtualCardViewHolder {
            val binding = LayoutItemCellBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ),
                parent,
                false
            )
            return VirtualCardViewHolder(binding)
        }
    }

    fun bind(virtualCard: VirtualCard) {
        if (virtualCard.state == CardState.CLOSED) {
            binding.name.text = binding.root.context.getString(R.string.virtual_card_cancelled_label, virtualCard.metadata?.unwrap())
        } else {
            binding.name.text = binding.root.context.getString(R.string.virtual_card_label, virtualCard.metadata?.unwrap())
        }
    }
}
