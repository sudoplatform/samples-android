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
    SENT,
    DRAFTS,
    TRASH,
    BLOCKLIST,
}

/**
 * An Adapter used to feed [EmailFolder] data to the drop down spinner list.
 */
class EmailFolderAdapter(val context: Context, private val onClickListener: () -> Unit) : BaseAdapter() {

    private val inflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val emailFolderViewHolder: EmailFolderViewHolder
        if (convertView == null) {
            view = inflater.inflate(R.layout.layout_dropdown_folder_item, parent, false)
            emailFolderViewHolder = EmailFolderViewHolder(view)
            view?.tag = emailFolderViewHolder
        } else {
            view = convertView
            emailFolderViewHolder = view.tag as EmailFolderViewHolder
        }
        emailFolderViewHolder.label.text = FolderTypes.entries[position].toString()
        if (FolderTypes.entries[position].toString() == "TRASH") {
            emailFolderViewHolder.imageView.visibility = View.VISIBLE
        }
        emailFolderViewHolder.imageView.setOnClickListener {
            onClickListener()
        }
        return view
    }

    override fun getItem(position: Int): Any {
        return FolderTypes.entries[position].toString()
    }

    override fun getCount(): Int {
        return FolderTypes.entries.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
}
