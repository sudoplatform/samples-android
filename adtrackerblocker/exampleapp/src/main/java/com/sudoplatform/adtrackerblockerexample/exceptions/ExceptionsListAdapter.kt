/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.adtrackerblockerexample.exceptions

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudoadtrackerblocker.types.BlockingException

/**
 * A [RecyclerView.Adapter] used to feed [BlockingException]s to the list view and handle creation and
 * replacement of views for the exceptions.
 *
 * @property items List of [BlockingException] data items to display.
 */
class ExceptionsListAdapter(private val items: List<BlockingException>) : RecyclerView.Adapter<ExceptionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExceptionViewHolder {
        return ExceptionViewHolder.inflate(parent)
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onBindViewHolder(holder: ExceptionViewHolder, position: Int) {
        holder.bind(items[position])
    }
}
