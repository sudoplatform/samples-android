/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.databinding.LayoutEmailMessageCellBinding
import com.sudoplatform.sudoemail.types.Direction
import com.sudoplatform.sudoemail.types.EmailMessage
import com.sudoplatform.sudoemail.types.EncryptionStatus
import java.util.Date

/**
 * A [RecyclerView.ViewHolder] used to describe the [EmailMessage] item view and metadata about its
 * place within the [RecyclerView].
 *
 * The item view contains a recipient, subject and a date label.
 *
 * @property binding [LayoutEmailMessageCellBinding] The [EmailMessage] item view binding component.
 */
class EmailMessageViewHolder(private val binding: LayoutEmailMessageCellBinding) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun inflate(parent: ViewGroup): EmailMessageViewHolder {
            val binding = LayoutEmailMessageCellBinding.inflate(
                LayoutInflater.from(
                    parent.context,
                ),
                parent,
                false,
            )
            return EmailMessageViewHolder(binding)
        }
    }

    fun bind(emailMessage: EmailMessage) {
        val encryptionIndicator = if (emailMessage.encryptionStatus === EncryptionStatus.ENCRYPTED) "🔒 " else ""
        if (emailMessage.hasAttachments) binding.imageView.visibility = View.VISIBLE else binding.imageView.visibility = View.GONE
        if (emailMessage.direction == Direction.INBOUND) {
            binding.recipientLabel.text = binding.root.context.getString(
                R.string.from_recipient,
                if (emailMessage.from[0].displayName == "null") {
                    emailMessage.from[0].emailAddress
                } else {
                    emailMessage.from[0].toString()
                },
                encryptionIndicator,
            )
        } else {
            binding.recipientLabel.text = binding.root.context.getString(
                R.string.to_recipient,
                emailMessage.to.joinToString { it.toString() }, encryptionIndicator,
            )
        }
        binding.subjectLabel.text = emailMessage.subject ?: binding.root.context.getString(R.string.no_subject)
        binding.dateLabel.text = formatDate(emailMessage.createdAt)
    }

    /**
     * Formats a [Date] to a presentable String.
     *
     * @param date [Date] The [Date] to be formatted.
     * @return A presentable [String] containing the date.
     */
    private fun formatDate(date: Date): String {
        return DateFormat.format("MM/dd/yyyy", date).toString()
    }
}
