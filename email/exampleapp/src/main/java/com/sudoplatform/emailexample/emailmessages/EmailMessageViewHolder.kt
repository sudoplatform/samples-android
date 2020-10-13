/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.emailexample.R
import com.sudoplatform.sudoemail.types.EmailMessage
import java.util.Date
import kotlinx.android.synthetic.main.layout_email_message_cell.view.*

/**
 * A [RecyclerView.ViewHolder] used to describe the [EmailMessage] item view and metadata about its
 * place within the [RecyclerView].
 *
 * The item view contains a [recipientLabel], [subjectLabel] and a [dateLabel].
 *
 * @property view The [EmailMessage] item view component.
 */
class EmailMessageViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

    private val recipientTextView: TextView = view.recipientLabel
    private val subjectTextView: TextView = view.subjectLabel
    private val dateTextView: TextView = view.dateLabel

    companion object {
        fun inflate(parent: ViewGroup): EmailMessageViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return EmailMessageViewHolder(
                inflater.inflate(
                    R.layout.layout_email_message_cell,
                    parent,
                    false
                )
            )
        }
    }

    fun bind(emailMessage: EmailMessage) {
        if (emailMessage.direction == EmailMessage.Direction.INBOUND) {
            recipientTextView.text = view.context.getString(R.string.from_recipient, emailMessage.from[0])
        } else {
            recipientTextView.text = view.context.getString(R.string.to_recipient, emailMessage.to.joinToString())
        }
        subjectTextView.text = emailMessage.subject ?: view.context.getString(R.string.no_subject)
        dateTextView.text = formatDate(emailMessage.createdAt)
    }

    /**
     * Formats a [Date] to a presentable String.
     *
     * @param date The [Date] to be formatted.
     * @return A presentable [String] containing the date.
     */
    private fun formatDate(date: Date): String {
        return DateFormat.format("MM/dd/yyyy", date).toString()
    }
}
