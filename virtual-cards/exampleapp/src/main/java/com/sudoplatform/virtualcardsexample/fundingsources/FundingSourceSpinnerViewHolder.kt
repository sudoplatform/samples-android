/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.fundingsources

import android.view.View
import android.widget.TextView
import com.sudoplatform.sudovirtualcards.types.FundingSource
import com.sudoplatform.virtualcardsexample.R

/**
 * A View Holder used to describe the [FundingSource] item view which contains a text view of
 * the funding source last 4 digits and type.
 *
 * @property row [View] The view which contains the [FundingSource] item.
 */
class FundingSourceSpinnerViewHolder(private val row: View?) {
    val label: TextView = row?.findViewById(R.id.textView) as TextView
}
