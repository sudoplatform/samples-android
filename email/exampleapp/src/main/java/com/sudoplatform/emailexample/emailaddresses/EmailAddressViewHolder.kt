/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaddresses

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.emailexample.R
import com.sudoplatform.sudoemail.types.EmailAddress
import kotlinx.android.synthetic.main.layout_item_cell.view.*

/**
 * A [RecyclerView.ViewHolder] used to describe the [EmailAddress] item view and metadata about its
 * place within the [RecyclerView].
 *
 * The item view contains a label [nameTextView] of the address.
 *
 * @property view The [EmailAddress] item view component.
 */
class EmailAddressViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

    private val nameTextView: TextView = view.name

    companion object {
        fun inflate(parent: ViewGroup): EmailAddressViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return EmailAddressViewHolder(
                inflater.inflate(
                    R.layout.layout_item_cell,
                    parent,
                    false
                )
            )
        }
    }

    fun bind(emailAddress: EmailAddress) {
        nameTextView.text = emailAddress.emailAddress
    }
}
