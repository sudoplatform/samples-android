/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages.emailAttachments

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudoemail.types.EmailAttachment

/**
 * A [RecyclerView.Adapter] used to feed [EmailAttachment] data to the list view and handle creation
 * and replacement of views for the [EmailAttachment] data items.
 *
 * @property items [List<EmailAttachment>] List of [EmailAttachment] data items to display.
 * @property itemSelectedListener Callback which listens for list item select events.
 */
class EmailAttachmentAdapter(private val items: List<EmailAttachment>, private val itemSelectedListener: (EmailAttachment) -> Unit) : RecyclerView.Adapter<EmailAttachmentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmailAttachmentViewHolder {
        return EmailAttachmentViewHolder.inflate(
            parent,
        )
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onBindViewHolder(holder: EmailAttachmentViewHolder, position: Int) {
        val emailAttachment = items[position]
        holder.bind(emailAttachment)

        holder.itemView.setOnClickListener {
            itemSelectedListener(emailAttachment)
        }
    }
}
