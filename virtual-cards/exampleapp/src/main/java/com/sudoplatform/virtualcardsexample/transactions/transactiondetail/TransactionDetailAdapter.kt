/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.transactions.transactiondetail

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * A [RecyclerView.Adapter] used to feed [TransactionDetailCell] data to the list view and handle
 * creation and replacement of views for the [TransactionDetailCell] data items.
 *
 * @property items List of [TransactionDetailCell] data items to display.
 */
class TransactionDetailAdapter(private val items: List<TransactionDetailCell>) : RecyclerView.Adapter<TransactionDetailViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): TransactionDetailViewHolder {
        return TransactionDetailViewHolder.inflate(
            parent,
        )
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onBindViewHolder(holder: TransactionDetailViewHolder, position: Int) {
        val transactionDetailCell = items[position]
        holder.bind(transactionDetailCell)
    }
}
