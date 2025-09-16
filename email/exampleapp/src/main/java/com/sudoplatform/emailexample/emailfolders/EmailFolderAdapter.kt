/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
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
 * Enumeration of special folder tab labels.
 */
enum class SpecialFolderTabLabels(
    val displayName: String,
) {
    DRAFTS("DRAFTS"),
    BLOCKLIST("BLOCKLIST"),
    CREATE("CREATE NEW FOLDER"),
}

private val standardFolderNames = listOf("INBOX", "OUTBOX", "SENT", "TRASH")

/**
 * An Adapter used to feed [EmailFolder] data to the drop down spinner list.
 */
class EmailFolderAdapter(
    val context: Context,
    private val folderNames: List<String>,
    private val onDeleteMessages: () -> Unit,
    private val onDeleteCustomFolder: (name: String) -> Unit,
    private val onEditCustomFolder: (name: String) -> Unit,
) : BaseAdapter() {
    private val inflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup?,
    ): View {
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
        val label = folderNames[position]
        emailFolderViewHolder.label.text = label
        if (label == "TRASH") {
            emailFolderViewHolder.deleteImageView.visibility = View.VISIBLE
            emailFolderViewHolder.deleteImageView.setOnClickListener {
                onDeleteMessages()
            }
        } else if (!SpecialFolderTabLabels.entries.map { it.displayName }.contains(label) && !standardFolderNames.contains(label)) {
            emailFolderViewHolder.deleteImageView.visibility = View.VISIBLE
            emailFolderViewHolder.editImageView.visibility = View.VISIBLE
            emailFolderViewHolder.deleteImageView.setOnClickListener {
                onDeleteCustomFolder(label)
            }
            emailFolderViewHolder.editImageView.setOnClickListener {
                onEditCustomFolder(label)
            }
        } else {
            emailFolderViewHolder.deleteImageView.visibility = View.GONE
            emailFolderViewHolder.editImageView.visibility = View.GONE
        }
        return view
    }

    override fun getItem(position: Int): Any = folderNames[position]

    override fun getCount(): Int = folderNames.size

    override fun getItemId(position: Int): Long = position.toLong()
}
