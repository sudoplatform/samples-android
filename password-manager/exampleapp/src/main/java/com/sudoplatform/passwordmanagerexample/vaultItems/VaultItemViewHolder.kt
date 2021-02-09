/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.vaultItems

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.sudopasswordmanager.models.VaultBankAccount
import com.sudoplatform.sudopasswordmanager.models.VaultCreditCard
import com.sudoplatform.sudopasswordmanager.models.VaultItem
import com.sudoplatform.sudopasswordmanager.models.VaultLogin

/**
 * A [RecyclerView.ViewHolder] used to describe the [VaultItem] item view and metadata about its place
 * within the [RecyclerView].
 *
 * The item view contains two lines [TextView] of the [VaultItem] name and second field.
 *
 * @param view The [VaultItem] item view component.
 */
class VaultItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val line1: TextView = view.findViewById(R.id.line1)
    private val line2: TextView = view.findViewById(R.id.line2)
    private val imageView: ImageView = view.findViewById(R.id.imageView_itemType)

    companion object {
        fun inflate(parent: ViewGroup): VaultItemViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return VaultItemViewHolder(inflater.inflate(R.layout.layout_item_two_row, parent, false))
        }
    }

    fun bind(item: VaultItem) {
        (item as? VaultLogin)?.let {
            line1.text = it.name
            line2.text = it.url
            imageView.setImageResource(R.drawable.ic_lock_24px)
        }

        (item as? VaultCreditCard)?.let {
            line1.text = it.name
            line2.text = it.cardNumber?.getValue()
            imageView.setImageResource(R.drawable.ic_credit_card_24px)
        }

        (item as? VaultBankAccount)?.let {
            line1.text = it.name
            line2.text = it.bankName
            imageView.setImageResource(R.drawable.ic_account_balance_24px)
        }
    }
}
