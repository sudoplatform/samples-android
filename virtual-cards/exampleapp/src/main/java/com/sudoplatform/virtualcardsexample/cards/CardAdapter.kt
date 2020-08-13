/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.cards

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudovirtualcards.types.Card

/**
 * A [RecyclerView.Adapter] used to feed [Card] data to the list view and handle creation and
 * replacement of views for the [Card] data items.
 *
 * @property items List of [Card] data items to display.
 * @property itemSelectedListener Callback which listens for list item select events.
 */
class CardAdapter(private val items: List<Card>, private val itemSelectedListener: (Card) -> Unit) : RecyclerView.Adapter<CardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        return CardViewHolder.inflate(
            parent
        )
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val card = items[position]
        holder.bind(card)

        holder.itemView.setOnClickListener {
            itemSelectedListener(card)
        }
    }
}
