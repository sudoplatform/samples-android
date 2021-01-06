/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.logins

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudopasswordmanager.models.VaultLogin

/**
 * A [RecyclerView.Adapter] used to feed [VaultLogin] data to the list view and handle creation and
 * replacement of views for the [VaultLogin] data items.
 *
 * @property items List of [VaultLogin] data items to display.
 * @property itemSelectedListener Callback which listens for list item select events.
 */
class LoginAdapter(
    private val items: List<VaultLogin>,
    private val itemSelectedListener: (VaultLogin) -> Unit
) : RecyclerView.Adapter<LoginViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoginViewHolder {
        return LoginViewHolder.inflate(
            parent
        )
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onBindViewHolder(holder: LoginViewHolder, position: Int) {
        val login = items[position]

        holder.bind(login)
        holder.itemView.setOnClickListener {
            itemSelectedListener(login)
        }
    }
}
