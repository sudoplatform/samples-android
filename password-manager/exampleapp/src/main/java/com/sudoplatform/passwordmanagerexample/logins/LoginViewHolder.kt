/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.logins

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.sudopasswordmanager.models.VaultLogin

/**
 * A [RecyclerView.ViewHolder] used to describe the [VaultLogin] item view and metadata about its place
 * within the [RecyclerView].
 *
 * The item view contains two lines [TextView] of the [VaultLogin] name and url.
 *
 * @param view The [VaultLogin] item view component.
 */
class LoginViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val line1: TextView = view.findViewById(R.id.line1)
    private val line2: TextView = view.findViewById(R.id.line2)

    companion object {
        fun inflate(parent: ViewGroup): LoginViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return LoginViewHolder(inflater.inflate(R.layout.layout_item_two_row, parent, false))
        }
    }

    fun bind(login: VaultLogin) {
        line1.text = login.name
        line2.text = login.url
    }
}
