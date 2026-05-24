/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmasks

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudoemail.types.EmailMask

/**
 * A [RecyclerView.Adapter] used to feed [EmailMask] data to the list view and handle creation
 * and replacement of views for the [EmailMask] data items.
 *
 * @property items [List<EmailMask>] List of [EmailMask] data items to display.
 * @property onItemClickListener Callback invoked when an [EmailMask] item is tapped.
 * @property onVerifyClick Optional callback invoked when the verify button is tapped on an [EmailMask] item.
 * @property showVerifyButton Optional function that determines whether the verify button should be shown for a given [EmailMask].
 */
class EmailMaskAdapter(
    private val items: List<EmailMask>,
    private val onItemClickListener: (EmailMask) -> Unit,
    private val onVerifyClick: ((EmailMask) -> Unit)? = null,
    private val showVerifyButton: ((EmailMask) -> Boolean)? = null,
) : RecyclerView.Adapter<EmailMaskViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): EmailMaskViewHolder =
        EmailMaskViewHolder.inflate(
            parent,
        )

    override fun getItemCount(): Int = items.count()

    override fun onBindViewHolder(
        holder: EmailMaskViewHolder,
        position: Int,
    ) {
        val emailMask = items[position]
        val shouldShowVerify = showVerifyButton?.invoke(emailMask) ?: false
        holder.bind(emailMask, onVerifyClick, shouldShowVerify)
        holder.itemView.setOnClickListener {
            onItemClickListener(items[holder.adapterPosition])
        }
    }
}
