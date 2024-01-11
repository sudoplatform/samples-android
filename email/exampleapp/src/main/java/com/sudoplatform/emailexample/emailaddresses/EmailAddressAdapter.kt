/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaddresses

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudoemail.types.EmailAddress

/**
 * A [RecyclerView.Adapter] used to feed [EmailAddress] data to the list view and handle creation
 * and replacement of views for the [EmailAddress] data items.
 *
 * @property items [List<EmailMessage>] List of [EmailAddress] data items to display.
 * @property itemSelectedListener Callback which listens for list item select events.
 */
class EmailAddressAdapter(private val items: List<EmailAddress>, private val itemSelectedListener: (EmailAddress) -> Unit) : RecyclerView.Adapter<EmailAddressViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmailAddressViewHolder {
        return EmailAddressViewHolder.inflate(
            parent,
        )
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onBindViewHolder(holder: EmailAddressViewHolder, position: Int) {
        val emailAddress = items[position]
        holder.bind(emailAddress)

        holder.itemView.setOnClickListener {
            itemSelectedListener(emailAddress)
        }
    }
}
