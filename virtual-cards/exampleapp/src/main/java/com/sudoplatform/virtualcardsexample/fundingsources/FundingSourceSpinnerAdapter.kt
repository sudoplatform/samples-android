/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.fundingsources

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.sudoplatform.sudovirtualcards.types.BankAccountFundingSource
import com.sudoplatform.sudovirtualcards.types.CreditCardFundingSource
import com.sudoplatform.sudovirtualcards.types.FundingSource
import com.sudoplatform.virtualcardsexample.R

/**
 * An Adapter used to feed [FundingSource] data to the drop down spinner list.
 *
 * @property items [List<FundingSource>] List of [FundingSource] data items to display.
 */
class FundingSourceSpinnerAdapter(val context: Context, private val items: List<FundingSource>) : BaseAdapter() {

    private val inflator = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val fundingSourceSpinnerViewHolder: FundingSourceSpinnerViewHolder
        if (convertView == null) {
            view = inflator.inflate(R.layout.layout_dropdown_item, parent, false)
            fundingSourceSpinnerViewHolder = FundingSourceSpinnerViewHolder(view)
            view?.tag = fundingSourceSpinnerViewHolder
        } else {
            view = convertView
            fundingSourceSpinnerViewHolder = view.tag as FundingSourceSpinnerViewHolder
        }
        when (val fundingSource = items[position]) {
            is CreditCardFundingSource -> {
                fundingSourceSpinnerViewHolder.label.text =
                    context.getString(
                        R.string.funding_source_credit_card_description,
                        fundingSource.last4,
                        fundingSource.cardType
                    )
            }
            is BankAccountFundingSource -> {
                fundingSourceSpinnerViewHolder.label.text =
                    context.getString(
                        R.string.funding_source_bank_account_description,
                        fundingSource.last4,
                        fundingSource.bankAccountType
                    )
            }
        }
        return view
    }

    override fun getItem(position: Int): Any {
        return items[position]
    }

    override fun getCount(): Int {
        return items.count()
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
}
