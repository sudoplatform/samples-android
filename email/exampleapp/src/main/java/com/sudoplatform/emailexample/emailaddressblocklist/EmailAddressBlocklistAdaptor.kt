/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaddressblocklist

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * A [RecyclerView.Adapter] used to feed blocked email address data to the list view and handle
 * creation and replacement of views for the data items.
 *
 * @property items [List<String>] List of block email addresses to display.
 * @property setItemSelected Callback which listens for item select events.
 */
class EmailAddressBlocklistAdaptor(
    private val items: List<String>,
    private val setItemSelected: (Boolean, String) -> Unit,
) : RecyclerView.Adapter<EmailAddressBlocklistViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmailAddressBlocklistViewHolder {
        return EmailAddressBlocklistViewHolder.inflate(parent)
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onBindViewHolder(holder: EmailAddressBlocklistViewHolder, position: Int) {
        val emailAddress = items[position]

        holder.bind(emailAddress) {
            setItemSelected(it, emailAddress)
        }
    }
}
