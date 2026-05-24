/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmasks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.databinding.LayoutEmailMaskItemCellBinding
import com.sudoplatform.sudoemail.types.EmailMask
import com.sudoplatform.sudoemail.types.EmailMaskStatus
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A [RecyclerView.ViewHolder] used to describe the [EmailMask] item view and metadata about its
 * place within the [RecyclerView].
 *
 * The item view contains the mask address as primary text and the status as color-coded
 * secondary text.
 *
 * @property binding [LayoutEmailMaskItemCellBinding] The [EmailMask] item view binding component.
 */
class EmailMaskViewHolder(
    private val binding: LayoutEmailMaskItemCellBinding,
) : RecyclerView.ViewHolder(binding.root) {
    companion object {
        fun inflate(parent: ViewGroup): EmailMaskViewHolder {
            val binding =
                LayoutEmailMaskItemCellBinding.inflate(
                    LayoutInflater.from(
                        parent.context,
                    ),
                    parent,
                    false,
                )
            return EmailMaskViewHolder(binding)
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
        emailMask: EmailMask,
        onVerifyClick: ((EmailMask) -> Unit)? = null,
        showVerify: Boolean = false,
    ) {
        binding.maskAddressText.text = emailMask.maskAddress
        binding.statusText.text = emailMask.status.name

        val statusColor =
            when (emailMask.status) {
                EmailMaskStatus.ENABLED -> ContextCompat.getColor(binding.root.context, R.color.colorPositiveMsg)
                EmailMaskStatus.DISABLED -> ContextCompat.getColor(binding.root.context, R.color.colorNegativeMsg)
                EmailMaskStatus.LOCKED -> ContextCompat.getColor(binding.root.context, R.color.colorNegativeMsg)
                EmailMaskStatus.PENDING -> ContextCompat.getColor(binding.root.context, R.color.colorWarningMsg)
                EmailMaskStatus.UNKNOWN -> ContextCompat.getColor(binding.root.context, android.R.color.darker_gray)
            }
        binding.statusText.setTextColor(statusColor)
        binding.realAddressText.text = emailMask.realAddress

        // Display metadata with title prefix or "-" if absent
        val metadata = emailMask.metadata
        binding.metadataText.text =
            if (!metadata.isNullOrEmpty()) {
                "Metadata (JSON): ${JSONObject(metadata)}"
            } else {
                "Metadata (JSON): -"
            }

        // Display expiration date with title prefix or "-" if absent
        binding.expiresAtText.text = formatExpiresAtForDisplay(emailMask.expiresAt)

        // Show/hide verify button based on visibility logic
        if (showVerify && onVerifyClick != null) {
            binding.verifyButton.visibility = View.VISIBLE
            binding.verifyButton.isEnabled = true
            binding.verifyButton.setOnClickListener {
                onVerifyClick(emailMask)
            }
        } else {
            binding.verifyButton.visibility = View.GONE
            binding.verifyButton.setOnClickListener(null)
        }
    }
}
