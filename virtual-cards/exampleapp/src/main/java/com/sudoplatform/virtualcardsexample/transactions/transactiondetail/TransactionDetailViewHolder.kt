/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.transactions.transactiondetail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.virtualcardsexample.R
import kotlinx.android.synthetic.main.layout_transaction_detail_cell.view.*

/**
 * A [RecyclerView.ViewHolder] used to describe the transaction detail item view and metadata about
 * its place within the [RecyclerView].
 *
 * The item view contains a [titleLabel], [subtitleLabel] and a [valueLabel].
 *
 * @param view The transaction detail item view component.
 */
class TransactionDetailViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val title: TextView = view.titleLabel
    private val subtitle: TextView = view.subtitleLabel
    private val value: TextView = view.valueLabel

    companion object {
        fun inflate(parent: ViewGroup): TransactionDetailViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return TransactionDetailViewHolder(
                inflater.inflate(
                    R.layout.layout_transaction_detail_cell,
                    parent,
                    false
                )
            )
        }
    }

    fun bind(transactionDetailCell: TransactionDetailCell) {
        title.text = transactionDetailCell.titleLabel
        subtitle.text = transactionDetailCell.subtitleLabel
        value.text = transactionDetailCell.valueLabel
    }
}
