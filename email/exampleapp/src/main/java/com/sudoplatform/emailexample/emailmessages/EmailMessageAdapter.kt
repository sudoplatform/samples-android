/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudoemail.types.EmailMessage

/**
 * A [RecyclerView.Adapter] used to feed [EmailMessage] data to the list view and handle creation
 * and replacement of views for the [EmailMessage] data items.
 *
 * @property items List of [EmailMessage] data items to display.
 * @property itemSelectedListener Callback which listens for list item select events.
 */
class EmailMessageAdapter(private val items: List<EmailMessage>, private val itemSelectedListener: (EmailMessage) -> Unit) : RecyclerView.Adapter<EmailMessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmailMessageViewHolder {
        return EmailMessageViewHolder.inflate(
            parent
        )
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onBindViewHolder(holder: EmailMessageViewHolder, position: Int) {
        val emailMessage = items[position]
        holder.bind(emailMessage)

        holder.itemView.setOnClickListener {
            itemSelectedListener(emailMessage)
        }
    }
}
