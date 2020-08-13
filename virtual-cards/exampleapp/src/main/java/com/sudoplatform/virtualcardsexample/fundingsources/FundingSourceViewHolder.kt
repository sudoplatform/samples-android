/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.fundingsources

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudovirtualcards.types.FundingSource
import com.sudoplatform.virtualcardsexample.R
import kotlinx.android.synthetic.main.layout_funding_source_cell.view.*

/**
 * A [RecyclerView.ViewHolder] used to describe the [FundingSource] item view and metadata about its
 * place within the [RecyclerView].
 *
 * The item view contains a label [nameTextView] of the credit card network and last four digits of
 * the funding source's card number.
 *
 * @property view The [FundingSource] item view component.
 */
class FundingSourceViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

    private val nameTextView: TextView = view.name

    companion object {
        fun inflate(parent: ViewGroup): FundingSourceViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return FundingSourceViewHolder(
                inflater.inflate(
                    R.layout.layout_funding_source_cell,
                    parent,
                    false
                )
            )
        }
    }

    fun bind(fundingSource: FundingSource) {
        if (fundingSource.state == FundingSource.State.INACTIVE)
            nameTextView.text = view.context.getString(R.string.funding_source_cancelled_label, fundingSource.network, fundingSource.last4)
        else
            nameTextView.text = view.context.getString(R.string.funding_source_label, fundingSource.network, fundingSource.last4)
    }
}
