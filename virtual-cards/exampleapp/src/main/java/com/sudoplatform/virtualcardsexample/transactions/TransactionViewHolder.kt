/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.transactions

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudovirtualcards.types.CurrencyAmount
import com.sudoplatform.sudovirtualcards.types.Transaction
import com.sudoplatform.virtualcardsexample.R
import java.util.Date
import kotlinx.android.synthetic.main.layout_transaction_cell.view.*

/**
 * A [RecyclerView.ViewHolder] used to describe the [Transaction] item view and metadata about its
 * place within the [RecyclerView].
 *
 * The item view contains labels:
 *  [descriptionTextView] of the [Transaction] description.
 *  [dateTextView] of the [Transaction] transacted at date.
 *  [amountTextView] of the [Transaction] billed amount.
 *  [feeTextView] of the [Transaction] fee amount.
 *
 * @property view The [Transaction] item view component.
 */
class TransactionViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

    private val descriptionTextView: TextView = view.descriptionLabel
    private val dateTextView: TextView = view.dateLabel
    private val amountTextView: TextView = view.amountLabel
    private val feeTextView: TextView = view.feeLabel

    companion object {
        fun inflate(parent: ViewGroup): TransactionViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return TransactionViewHolder(
                inflater.inflate(
                    R.layout.layout_transaction_cell,
                    parent,
                    false
                )
            )
        }
    }

    fun bind(transaction: Transaction) {
        val description = when (transaction.type) {
            Transaction.Type.DECLINE -> view.context.getString(R.string.declined_transaction_desc, transaction.description)
            Transaction.Type.REFUND -> view.context.getString(R.string.refunded_transaction_desc, transaction.description)
            else -> transaction.description
        }
        descriptionTextView.text = description
        dateTextView.text = formatDate(transaction.transactedAt)
        amountTextView.text = formatCurrencyAmount(transaction.billedAmount)
        feeTextView.text = formatCurrencyAmount(transaction.details.first().markupAmount)
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
