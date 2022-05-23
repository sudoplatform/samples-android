/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.fundingsources

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudovirtualcards.types.FundingSource
import com.sudoplatform.virtualcardsexample.R
import com.sudoplatform.virtualcardsexample.databinding.LayoutFundingSourceCellBinding

/**
 * A [RecyclerView.ViewHolder] used to describe the [FundingSource] item view and metadata about its
 * place within the [RecyclerView].
 *
 * The item view contains a label [TextView] of the credit card network and last four digits of
 * the funding source's card number.
 *
 * @property binding The [FundingSource] item view binding component.
 */
class FundingSourceViewHolder(private val binding: LayoutFundingSourceCellBinding) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun inflate(parent: ViewGroup): FundingSourceViewHolder {
            val binding = LayoutFundingSourceCellBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ),
                parent,
                false
            )
            return FundingSourceViewHolder(binding)
        }
    }

    fun bind(fundingSource: FundingSource) {
        if (fundingSource.state == FundingSource.State.INACTIVE)
            binding.name.text = binding.root.context.getString(R.string.funding_source_cancelled_label, fundingSource.network, fundingSource.last4)
        else
            binding.name.text = binding.root.context.getString(R.string.funding_source_label, fundingSource.network, fundingSource.last4)
    }
}
