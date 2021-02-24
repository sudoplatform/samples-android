/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.cards

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudovirtualcards.types.Card
import com.sudoplatform.virtualcardsexample.R
import com.sudoplatform.virtualcardsexample.databinding.LayoutItemCellBinding

/**
 * A [RecyclerView.ViewHolder] used to describe the [Card] item view and metadata about its place
 * within the [RecyclerView].
 *
 * The item view contains a label [TextView] of the [Card] alias.
 *
 * @property binding The [Card] item view binding component.
 */
class CardViewHolder(private val binding: LayoutItemCellBinding) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun inflate(parent: ViewGroup): CardViewHolder {
            val binding = LayoutItemCellBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ),
                parent,
                false
            )
            return CardViewHolder(binding)
        }
    }

    fun bind(card: Card) {
        if (card.state == Card.State.CLOSED)
            binding.name.text = binding.root.context.getString(R.string.card_cancelled_label, card.alias)
        else
            binding.name.text = binding.root.context.getString(R.string.card_label, card.alias)
    }
}
