/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.sudos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.sudoprofiles.Sudo

/**
 * A [RecyclerView.ViewHolder] used to describe the [Sudo] item view and metadata about its place
 * within the [RecyclerView].
 *
 * The item view contains a label [TextView] of the [Sudo] name.
 *
 * @param view The [Sudo] item view component.
 */
class SudoViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val nameTextView: TextView = view.findViewById(R.id.name)

    companion object {
        fun inflate(parent: ViewGroup): SudoViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return SudoViewHolder(
                inflater.inflate(
                    R.layout.layout_item_cell,
                    parent,
                    false
                )
            )
        }
    }

    fun bind(sudoName: String) {
        nameTextView.text = sudoName
    }
}
