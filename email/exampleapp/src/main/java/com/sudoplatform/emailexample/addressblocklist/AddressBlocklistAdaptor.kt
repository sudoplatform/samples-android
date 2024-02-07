/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.addressblocklist

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudoapiclient.sudoApiClientLogger

class AddressBlocklistAdaptor(
    private val addresses: MutableList<String>,
    private val setItemSelected: (Boolean, String) -> Unit,
) : RecyclerView.Adapter<AddressBlocklistHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressBlocklistHolder {
        return AddressBlocklistHolder.inflate(parent)
    }

    override fun getItemCount(): Int {
        return addresses.count()
    }

    override fun onBindViewHolder(holder: AddressBlocklistHolder, position: Int) {
        val address = addresses[position]
        sudoApiClientLogger.debug(address)
        holder.bind(address) {
            setItemSelected(it, address)
        }
    }
}
