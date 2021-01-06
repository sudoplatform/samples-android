/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.settings

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudopasswordmanager.entitlements.EntitlementState

/**
 * A [RecyclerView.Adapter] used to feed [EntitlementState] data to the list
 * view and handle creation and replacement of views for the data items.
 *
 * @property entitlements List of [EntitlementState] for the Sudos.
 */
class ViewEntitlementsAdapter(private val entitlements: List<EntitlementState>) : RecyclerView.Adapter<ViewEntitlementsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewEntitlementsViewHolder {
        return ViewEntitlementsViewHolder.inflate(
            parent
        )
    }

    override fun getItemCount(): Int {
        return entitlements.count()
    }

    override fun onBindViewHolder(holder: ViewEntitlementsViewHolder, position: Int) {
        val entitlementState = entitlements[position]
        holder.bind(position, entitlementState)
    }
}
