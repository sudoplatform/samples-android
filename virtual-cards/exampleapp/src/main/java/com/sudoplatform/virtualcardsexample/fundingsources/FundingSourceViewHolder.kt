/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.fundingsources

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudovirtualcards.extensions.isUnfunded
import com.sudoplatform.sudovirtualcards.extensions.needsRefresh
import com.sudoplatform.sudovirtualcards.types.BankAccountFundingSource
import com.sudoplatform.sudovirtualcards.types.CreditCardFundingSource
import com.sudoplatform.sudovirtualcards.types.CurrencyAmount
import com.sudoplatform.sudovirtualcards.types.FundingSource
import com.sudoplatform.sudovirtualcards.types.FundingSourceState
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
                    parent.context,
                ),
                parent,
                false,
            )
            return FundingSourceViewHolder(binding)
        }
    }

    fun bind(fundingSource: FundingSource) {
        when (fundingSource) {
            is CreditCardFundingSource -> {
                if (fundingSource.state == FundingSourceState.INACTIVE) {
                    binding.name.text =
                        binding.root.context.getString(R.string.funding_source_credit_card_cancelled_label, fundingSource.network)
                } else {
                    binding.name.text = binding.root.context.getString(
                        R.string.funding_source_credit_card_label,
                        fundingSource.network,
                    )
                }
                binding.description.text = binding.root.context.getString(R.string.funding_source_credit_card_description, fundingSource.last4, fundingSource.cardType)
                setCardNetwork(fundingSource.network)
            }
            is BankAccountFundingSource -> {
                if (fundingSource.state == FundingSourceState.INACTIVE) {
                    binding.name.text =
                        binding.root.context.getString(R.string.funding_source_bank_account_cancelled_label, fundingSource.institutionName)
                } else {
                    binding.name.text =
                        binding.root.context.getString(R.string.funding_source_bank_account_label, fundingSource.institutionName)
                }
                binding.refreshButton.visibility = if (fundingSource.needsRefresh()) View.VISIBLE else View.GONE

                val unfundedSuffix = if (fundingSource.isUnfunded()) " ***UNFUNDED*** ${this.formatCurrencyAmount(fundingSource.unfundedAmount)}" else ""
                binding.description.text = binding.root.context.getString(R.string.funding_source_bank_account_description, fundingSource.last4, fundingSource.bankAccountType, unfundedSuffix)
                if (fundingSource.institutionLogo != null) {
                    val decoded = Base64.decode(fundingSource.institutionLogo?.data, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                    binding.imageView.setImageBitmap(bitmap)
                } else {
                    binding.imageView.setImageResource(R.drawable.ic_bank_32px)
                }
            }
        }
    }

    private fun formatCurrencyAmount(amount: CurrencyAmount?): String {
        if (amount == null) {
            return ""
        }
        val centsAmount = String.format("%.2f", amount.amount / 100.0)
        return "$$centsAmount ${amount.currency}"
    }

    fun getRefreshButton(): Button {
        return binding.refreshButton
    }

    private fun setCardNetwork(network: CreditCardFundingSource.CreditCardNetwork) {
        when (network) {
            CreditCardFundingSource.CreditCardNetwork.MASTERCARD ->
                binding.imageView.setImageResource(R.drawable.ic_mastercard_32px)
            CreditCardFundingSource.CreditCardNetwork.VISA ->
                binding.imageView.setImageResource(R.drawable.ic_visa_32px)
            else -> binding.imageView.setImageResource(R.drawable.ic_credit_card_32px)
        }
    }
}
