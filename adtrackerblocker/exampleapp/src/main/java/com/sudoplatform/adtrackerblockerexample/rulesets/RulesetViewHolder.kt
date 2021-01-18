/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.adtrackerblockerexample.sudos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.adtrackerblockerexample.R
import com.sudoplatform.sudoadtrackerblocker.types.Ruleset

/**
 * A [RecyclerView.ViewHolder] used to describe the [Ruleset] item view and metadata about its place
 * within the [RecyclerView].
 *
 * The item view contains a label [TextView] of the [Ruleset] name.
 *
 * @param view The [Ruleset] item view component.
 */
class RulesetViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val typeTextView: TextView = view.findViewById(R.id.type)

    companion object {
        fun inflate(parent: ViewGroup): RulesetViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return RulesetViewHolder(
                inflater.inflate(
                    R.layout.layout_ruleset_cell,
                    parent,
                    false
                )
            )
        }
    }

    fun bind(ruleset: Ruleset) {
        typeTextView.text = ruleset.type.toString()
    }
}
