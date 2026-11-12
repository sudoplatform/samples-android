/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaliases

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.databinding.LayoutEmailAliasItemCellBinding
import com.sudoplatform.sudoemail.types.EmailAlias
import com.sudoplatform.sudoemail.types.EmailAliasStatus
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A [RecyclerView.ViewHolder] used to describe the [EmailAlias] item view and metadata about its
 * place within the [RecyclerView].
 *
 * The item view contains the alias address as primary text and the status as color-coded
 * secondary text.
 *
 * @property binding [LayoutEmailAliasItemCellBinding] The [EmailAlias] item view binding component.
 */
class EmailAliasViewHolder(
    private val binding: LayoutEmailAliasItemCellBinding,
) : RecyclerView.ViewHolder(binding.root) {
    companion object {
        fun inflate(parent: ViewGroup): EmailAliasViewHolder {
            val binding =
                LayoutEmailAliasItemCellBinding.inflate(
                    LayoutInflater.from(
                        parent.context,
                    ),
                    parent,
                    false,
                )
            return EmailAliasViewHolder(binding)
        }

        /**
         * Formats an expiration date for display in the list item.
         * Returns "Expires: MM/dd/yyyy" for valid dates with time > 0,
         * or "Expires: -" for null or epoch-zero dates.
         */
        fun formatExpiresAtForDisplay(expiresAt: Date?): String {
            if (expiresAt == null || expiresAt.time == 0L) return "Expires: -"
            val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            return "Expires: ${formatter.format(expiresAt)}"
        }
    }

    fun bind(
        emailAlias: EmailAlias,
        onVerifyClick: ((EmailAlias) -> Unit)? = null,
        showVerify: Boolean = false,
    ) {
        binding.aliasAddressText.text = emailAlias.aliasAddress
        binding.statusText.text = emailAlias.status.name

        val statusColor =
            when (emailAlias.status) {
                EmailAliasStatus.ENABLED -> ContextCompat.getColor(binding.root.context, R.color.colorPositiveMsg)
                EmailAliasStatus.DISABLED -> ContextCompat.getColor(binding.root.context, R.color.colorNegativeMsg)
                EmailAliasStatus.LOCKED -> ContextCompat.getColor(binding.root.context, R.color.colorNegativeMsg)
                EmailAliasStatus.PENDING -> ContextCompat.getColor(binding.root.context, R.color.colorWarningMsg)
                EmailAliasStatus.UNKNOWN -> ContextCompat.getColor(binding.root.context, android.R.color.darker_gray)
            }
        binding.statusText.setTextColor(statusColor)
        binding.realAddressText.text = emailAlias.realAddress

        // Display metadata with title prefix or "-" if absent
        val metadata = emailAlias.metadata
        binding.metadataText.text =
            if (!metadata.isNullOrEmpty()) {
                "Metadata (JSON): ${JSONObject(metadata)}"
            } else {
                "Metadata (JSON): -"
            }

        // Display expiration date with title prefix or "-" if absent
        binding.expiresAtText.text = formatExpiresAtForDisplay(emailAlias.expiresAt)

        // Show/hide verify button based on visibility logic
        if (showVerify && onVerifyClick != null) {
            binding.verifyButton.visibility = View.VISIBLE
            binding.verifyButton.isEnabled = true
            binding.verifyButton.setOnClickListener {
                onVerifyClick(emailAlias)
            }
        } else {
            binding.verifyButton.visibility = View.GONE
            binding.verifyButton.setOnClickListener(null)
        }
    }
}
