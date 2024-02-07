/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailfolders

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.sudoplatform.emailexample.R
import com.sudoplatform.sudoemail.types.EmailFolder

/**
 * Enumeration of the types of supported email folders.
 */
enum class FolderTypes {
    INBOX,
    OUTBOX,
    SENT,
    DRAFTS,
    TRASH,
    BLOCKLIST,
}

/**
 * An Adapter used to feed [EmailFolder] data to the drop down spinner list.
 */
class EmailFolderAdapter(context: Context) : BaseAdapter() {

    private val inflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val emailFolderViewHolder: EmailFolderViewHolder
        if (convertView == null) {
            view = inflater.inflate(R.layout.layout_dropdown_item, parent, false)
            emailFolderViewHolder = EmailFolderViewHolder(view)
            view?.tag = emailFolderViewHolder
        } else {
            view = convertView
            emailFolderViewHolder = view.tag as EmailFolderViewHolder
        }
        emailFolderViewHolder.label.text = FolderTypes.values()[position].toString()

        return view
    }

    override fun getItem(position: Int): Any {
        return FolderTypes.values()[position].toString()
    }

    override fun getCount(): Int {
        return FolderTypes.values().size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
}
