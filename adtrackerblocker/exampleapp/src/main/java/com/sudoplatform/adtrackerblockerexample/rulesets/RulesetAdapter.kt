/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.adtrackerblockerexample.sudos

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudoadtrackerblocker.types.Ruleset

/**
 * A [RecyclerView.Adapter] used to feed [Ruleset] data to the list view and handle creation and
 * replacement of views for the [Ruleset] data items.
 *
 * @property items List of [Ruleset] data items to display.
 */
class RulesetAdapter(private val items: List<Ruleset>) : RecyclerView.Adapter<RulesetViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RulesetViewHolder {
        return RulesetViewHolder.inflate(
            parent
        )
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onBindViewHolder(holder: RulesetViewHolder, position: Int) {
        holder.bind(items[position])
    }
}
