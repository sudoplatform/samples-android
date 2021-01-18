/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.adtrackerblockerexample.exceptions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.adtrackerblockerexample.R
import com.sudoplatform.sudoadtrackerblocker.types.BlockingException

/**
 * A [RecyclerView.ViewHolder] used to describe the exceptions list item view and metadata
 * about its place within the [RecyclerView].
 *
 * @param view The view component for the [BlockingException]. It shows the URL
 * and an icon to indicate if it is a host or page exception.
 */
class ExceptionViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val textView: TextView = view.findViewById(R.id.name)
    private val imageView: ImageView = view.findViewById(R.id.imageView)

    companion object {
        fun inflate(parent: ViewGroup): ExceptionViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return ExceptionViewHolder(
                inflater.inflate(
                    R.layout.layout_item_cell,
                    parent,
                    false
                )
            )
        }
    }

    fun bind(exception: BlockingException) {
        textView.text = exception.source
        if (exception.type == BlockingException.Type.HOST) {
            imageView.setImageResource(R.drawable.ic_host_exception)
        } else {
            imageView.setImageResource(R.drawable.ic_page_exception)
        }
        // To help with Espresso automated testing
        imageView.contentDescription = exception.type.name
    }
}
