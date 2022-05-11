/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.vaultItems

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.databinding.LayoutItemTwoRowBinding
import com.sudoplatform.sudopasswordmanager.models.VaultBankAccount
import com.sudoplatform.sudopasswordmanager.models.VaultContact
import com.sudoplatform.sudopasswordmanager.models.VaultCreditCard
import com.sudoplatform.sudopasswordmanager.models.VaultDocument
import com.sudoplatform.sudopasswordmanager.models.VaultDriversLicense
import com.sudoplatform.sudopasswordmanager.models.VaultItem
import com.sudoplatform.sudopasswordmanager.models.VaultLogin
import com.sudoplatform.sudopasswordmanager.models.VaultMembership
import com.sudoplatform.sudopasswordmanager.models.VaultPassport
import com.sudoplatform.sudopasswordmanager.models.VaultSocialSecurityNumber

/**
 * A [RecyclerView.ViewHolder] used to describe the [VaultItem] item view and metadata about its place
 * within the [RecyclerView].
 *
 * The item view contains two lines [TextView] of the [VaultItem] name and second field.
 *
 * @param binding The [VaultItem] view binding
 */
class VaultItemViewHolder(
    private val binding: LayoutItemTwoRowBinding
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun inflate(parent: ViewGroup): VaultItemViewHolder {
            val binding = LayoutItemTwoRowBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return VaultItemViewHolder(binding)
        }
    }

    fun bind(item: VaultItem) {
        (item as? VaultLogin)?.let {
            binding.line1.text = it.name
            binding.line2.text = it.url
            binding.imageViewItemType.setImageResource(R.drawable.ic_lock_24px)
        }

        (item as? VaultCreditCard)?.let {
            binding.line1.text = it.name
            binding.line2.text = it.cardNumber?.getValue()
            binding.imageViewItemType.setImageResource(R.drawable.ic_credit_card_24px)
        }

        (item as? VaultBankAccount)?.let {
            binding.line1.text = it.name
            binding.line2.text = it.bankName
            binding.imageViewItemType.setImageResource(R.drawable.ic_account_balance_24px)
        }

        (item as? VaultContact)?.let {
            binding.line1.text = it.name
            binding.line2.text = it.firstName
            binding.imageViewItemType.setImageResource(R.drawable.ic_baseline_account_box_24)
        }

        (item as? VaultDriversLicense)?.let {
            binding.line1.text = it.name
            binding.line2.text = it.firstName
            binding.imageViewItemType.setImageResource(R.drawable.ic_baseline_drive_eta_24)
        }

        (item as? VaultDocument)?.let {
            binding.line1.text = it.name
            binding.line2.text = it.contentType
            binding.imageViewItemType.setImageResource(R.drawable.ic_baseline_insert_drive_file_24)
        }

        (item as? VaultMembership)?.let {
            binding.line1.text = it.name
            binding.line2.text = it.firstName
            binding.imageViewItemType.setImageResource(R.drawable.ic_baseline_card_membership_24)
        }

        (item as? VaultPassport)?.let {
            binding.line1.text = it.name
            binding.line2.text = it.firstName
            binding.imageViewItemType.setImageResource(R.drawable.ic_baseline_featured_video_24)
        }

        (item as? VaultSocialSecurityNumber)?.let {
            binding.line1.text = it.name
            binding.line2.text = it.firstName
            binding.imageViewItemType.setImageResource(R.drawable.ic_baseline_grid_3x3_24)
        }
    }
}
