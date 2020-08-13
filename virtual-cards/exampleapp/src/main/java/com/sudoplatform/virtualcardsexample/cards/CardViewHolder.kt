/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.cards

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudovirtualcards.types.Card
import com.sudoplatform.virtualcardsexample.R
import kotlinx.android.synthetic.main.layout_item_cell.view.*

/**
 * A [RecyclerView.ViewHolder] used to describe the [Card] item view and metadata about its place
 * within the [RecyclerView].
 *
 * The item view contains a label [nameTextView] of the [Card] alias.
 *
 * @property view The [Card] item view component.
 */
class CardViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

    private val nameTextView: TextView = view.name

    companion object {
        fun inflate(parent: ViewGroup): CardViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return CardViewHolder(
                inflater.inflate(
                    R.layout.layout_item_cell,
                    parent,
                    false
                )
            )
        }
    }

    fun bind(card: Card) {
        if (card.state == Card.State.CLOSED)
            nameTextView.text = view.context.getString(R.string.card_cancelled_label, card.alias)
        else
            nameTextView.text = view.context.getString(R.string.card_label, card.alias)
    }
}
