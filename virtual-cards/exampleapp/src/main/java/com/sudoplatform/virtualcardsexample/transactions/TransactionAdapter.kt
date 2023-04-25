/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.transactions

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudovirtualcards.types.Transaction

/**
 * A [RecyclerView.Adapter] used to feed [Transaction] data to the list view and handle creation and
 * replacement of views for the [Transaction] data items.
 *
 * @property items List of [Transaction] data items to display.
 * @property itemSelectedListener Callback which listens for list item select events.
 */
class TransactionAdapter(private val items: List<Transaction>, private val itemSelectedListener: (Transaction) -> Unit) : RecyclerView.Adapter<TransactionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        return TransactionViewHolder.inflate(
            parent
        )
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = items[position]
        holder.bind(transaction)

        holder.itemView.setOnClickListener {
            itemSelectedListener(transaction)
        }
    }
}
