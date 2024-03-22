/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.fundingsources

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudovirtualcards.types.FundingSource

/**
 * A [RecyclerView.Adapter] used to feed [FundingSource] data to the list view and handle creation
 * and replacement of views for the [FundingSource] data items.
 *
 * @property items List of [FundingSource] data items to display.
 */
class FundingSourceAdapter(private val items: List<FundingSource>, private val buttonClickedListener: (FundingSource) -> Unit) : RecyclerView.Adapter<FundingSourceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FundingSourceViewHolder {
        return FundingSourceViewHolder.inflate(
            parent,
        )
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onBindViewHolder(holder: FundingSourceViewHolder, position: Int) {
        val fundingSource = items[position]
        holder.bind(fundingSource)

        holder.getRefreshButton().setOnClickListener {
            buttonClickedListener(fundingSource)
        }
    }
}
