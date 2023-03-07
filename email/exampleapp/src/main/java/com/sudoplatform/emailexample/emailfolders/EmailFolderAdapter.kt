/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
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
 * An Adapter used to feed [EmailFolder] data to the drop down spinner list.
 *
 * @property items List of [EmailFolder] data items to display.
 */
class EmailFolderAdapter(context: Context, private val items: List<EmailFolder>) : BaseAdapter() {

    private val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

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
        emailFolderViewHolder.label.text = items[position].folderName

        return view
    }

    override fun getItem(position: Int): Any {
        return items[position]
    }

    override fun getCount(): Int {
        return items.count()
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
}
