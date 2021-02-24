/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.transactions

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudovirtualcards.types.CurrencyAmount
import com.sudoplatform.sudovirtualcards.types.Transaction
import com.sudoplatform.virtualcardsexample.R
import com.sudoplatform.virtualcardsexample.databinding.LayoutTransactionCellBinding
import java.util.Date

/**
 * A [RecyclerView.ViewHolder] used to describe the [Transaction] item view and metadata about its
 * place within the [RecyclerView].
 *
 * The item view contains labels:
 *  [TextView] of the [Transaction] description.
 *  [TextView] of the [Transaction] transacted at date.
 *  [TextView] of the [Transaction] billed amount.
 *  [TextView] of the [Transaction] fee amount.
 *
 * @property binding The [Transaction] item view binding component.
 */
class TransactionViewHolder(private val binding: LayoutTransactionCellBinding) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun inflate(parent: ViewGroup): TransactionViewHolder {
            val binding = LayoutTransactionCellBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ),
                parent,
                false
            )
            return TransactionViewHolder(binding)
        }
    }

    fun bind(transaction: Transaction) {
        val description = when (transaction.type) {
            Transaction.Type.DECLINE -> binding.root.context.getString(R.string.declined_transaction_desc, transaction.description)
            Transaction.Type.REFUND -> binding.root.context.getString(R.string.refunded_transaction_desc, transaction.description)
            else -> transaction.description
        }
        binding.descriptionLabel.text = description
        binding.dateLabel.text = formatDate(transaction.transactedAt)
        binding.amountLabel.text = formatCurrencyAmount(transaction.billedAmount)
        binding.feeLabel.text = formatCurrencyAmount(transaction.details.first().markupAmount)
    }

    /**
     * Formats a [CurrencyAmount] value to a presentable [String].
     *
     * @param value The [CurrencyAmount] to be formatted.
     * @return A presentable [String] containing the currency amount value.
     */
    private fun formatCurrencyAmount(value: CurrencyAmount): String {
        val doubleVal = value.amount.toDouble() / 100.0
        return "$%.2f".format(doubleVal)
    }

    /**
     * Formats a [Date] to a presentable String.
     *
     * @param date The [Date] to be formatted.
     * @return A presentable [String] containing the date.
     */
    private fun formatDate(date: Date): String {
        return DateFormat.format("MM/dd/yyyy", date).toString()
    }
}
