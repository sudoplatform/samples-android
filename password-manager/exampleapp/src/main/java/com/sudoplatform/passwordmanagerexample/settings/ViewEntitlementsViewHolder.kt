/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.settings

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.sudopasswordmanager.entitlements.EntitlementState

/**
 * A [RecyclerView.ViewHolder] used to describe the [EntitlementState] item view and metadata
 * about its place within the [RecyclerView].
 *
 * @param view The entitlement item view component.
 */
class ViewEntitlementsViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val sudoNameTextView: TextView = view.findViewById(R.id.sudoName)
    private val nameTextView: TextView = view.findViewById(R.id.entitlementName)
    private val limitTextView: TextView = view.findViewById(R.id.entitlementLimit)
    private val valueTextView: TextView = view.findViewById(R.id.entitlementValue)

    companion object {
        fun inflate(parent: ViewGroup): ViewEntitlementsViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return ViewEntitlementsViewHolder(
                inflater.inflate(
                    R.layout.layout_item_view_entitlement,
                    parent,
                    false
                )
            )
        }
    }

    fun bind(position: Int, entitlementState: EntitlementState) {
        val context = sudoNameTextView.context
        sudoNameTextView.text = context.getString(R.string.sudo_name_format, position)
        nameTextView.text = toPrettyName(context, entitlementState.name)
        limitTextView.text = "${entitlementState.limit}"
        valueTextView.text = "${entitlementState.value}"
    }

    private fun toPrettyName(context: Context, entitlementName: EntitlementState.Name): String {
        return when (entitlementName) {
            EntitlementState.Name.MAX_VAULTS_PER_SUDO -> context.getString(R.string.entitlement_max_vaults)
            else -> entitlementName.name
        }
    }
}
