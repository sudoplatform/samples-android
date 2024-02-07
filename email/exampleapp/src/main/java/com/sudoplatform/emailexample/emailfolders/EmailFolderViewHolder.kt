/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailfolders

import android.view.View
import android.widget.TextView
import com.sudoplatform.emailexample.R
import com.sudoplatform.sudoemail.types.EmailFolder

/**
 * A View Holder used to describe the [EmailFolder] item view which contains a text view of
 * the folder name.
 *
 * @property row [View] The view which contains the [EmailFolder] item.
 */
class EmailFolderViewHolder(private val row: View?) {
    val label: TextView = row?.findViewById(R.id.textView) as TextView
}
