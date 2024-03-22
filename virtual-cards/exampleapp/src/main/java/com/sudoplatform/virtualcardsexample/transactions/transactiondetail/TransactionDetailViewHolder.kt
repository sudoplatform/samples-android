/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.transactions.transactiondetail

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.virtualcardsexample.databinding.LayoutTransactionDetailCellBinding

/**
 * A [RecyclerView.ViewHolder] used to describe the transaction detail item view and metadata about
 * its place within the [RecyclerView].
 *
 * The item view contains a label [TextView]s of the title, subtitle and value.
 *
 * @property binding The transaction detail item view binding component.
 */
class TransactionDetailViewHolder(private val binding: LayoutTransactionDetailCellBinding) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun inflate(parent: ViewGroup): TransactionDetailViewHolder {
            val binding = LayoutTransactionDetailCellBinding.inflate(
                LayoutInflater.from(
                    parent.context,
                ),
                parent,
                false,
            )
            return TransactionDetailViewHolder(binding)
        }
    }

    fun bind(transactionDetailCell: TransactionDetailCell) {
        binding.titleLabel.text = transactionDetailCell.titleLabel
        binding.subtitleLabel.text = transactionDetailCell.subtitleLabel
        binding.valueLabel.text = transactionDetailCell.valueLabel
    }
}
