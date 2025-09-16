/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudoemail.types.EmailMessage
import com.sudoplatform.sudoemail.types.ScheduledDraftMessage

/**
 * A [RecyclerView.Adapter] used to feed [EmailMessage] data to the list view and handle creation
 * and replacement of views for the [EmailMessage] data items.
 *
 * @property items [List<EmailMessage>] List of [EmailMessage] data items to display.
 * @property scheduledMessages [List<ScheduledDraftMessage>] List of scheduled draft messages.
 * @property itemSelectedListener Callback which listens for list item select events.
 */
class EmailMessageAdapter(
    private val items: List<EmailMessage>,
    private val scheduledMessages: List<ScheduledDraftMessage>,
    private val itemSelectedListener: (EmailMessage) -> Unit,
) : RecyclerView.Adapter<EmailMessageViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): EmailMessageViewHolder =
        EmailMessageViewHolder.inflate(
            parent,
        )

    override fun getItemCount(): Int = items.count()

    override fun onBindViewHolder(
        holder: EmailMessageViewHolder,
        position: Int,
    ) {
        val emailMessage = items[position]
        val scheduledAt = scheduledMessages.find { it.id == emailMessage.id }?.sendAt
        holder.bind(emailMessage, scheduledAt)

        holder.itemView.setOnClickListener {
            itemSelectedListener(emailMessage)
        }
    }
}
