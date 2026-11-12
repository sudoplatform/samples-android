/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaliases

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudoemail.types.EmailAlias

/**
 * A [RecyclerView.Adapter] used to feed [EmailAlias] data to the list view and handle creation
 * and replacement of views for the [EmailAlias] data items.
 *
 * @property items [List<EmailAlias>] List of [EmailAlias] data items to display.
 * @property onItemClickListener Callback invoked when an [EmailAlias] item is tapped.
 * @property onVerifyClick Optional callback invoked when the verify button is tapped on an [EmailAlias] item.
 * @property showVerifyButton Optional function that determines whether the verify button should be shown for a given [EmailAlias].
 */
class EmailAliasAdapter(
    private val items: List<EmailAlias>,
    private val onItemClickListener: (EmailAlias) -> Unit,
    private val onVerifyClick: ((EmailAlias) -> Unit)? = null,
    private val showVerifyButton: ((EmailAlias) -> Boolean)? = null,
) : RecyclerView.Adapter<EmailAliasViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): EmailAliasViewHolder =
        EmailAliasViewHolder.inflate(
            parent,
        )

    override fun getItemCount(): Int = items.count()

    override fun onBindViewHolder(
        holder: EmailAliasViewHolder,
        position: Int,
    ) {
        val emailAlias = items[position]
        val shouldShowVerify = showVerifyButton?.invoke(emailAlias) ?: false
        holder.bind(emailAlias, onVerifyClick, shouldShowVerify)
        holder.itemView.setOnClickListener {
            onItemClickListener(items[holder.adapterPosition])
        }
    }
}
