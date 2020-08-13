/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.shared

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.virtualcardsexample.R
import kotlinx.android.synthetic.main.layout_form_cell.view.*

/**
 * A [RecyclerView.ViewHolder] used to describe the input form item view and metadata about its
 * place within the [RecyclerView].
 *
 * The item view contains a [title] and an [inputField].
 *
 * @param view The input form item view component.
 */
class InputFormViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    val title: TextView = view.title
    val inputField: EditText = view.inputField

    companion object {
        fun inflate(parent: ViewGroup): InputFormViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return InputFormViewHolder(
                inflater.inflate(
                    R.layout.layout_form_cell,
                    parent,
                    false
                )
            )
        }
    }

    fun bind(inputFormCell: InputFormCell) {
        title.text = inputFormCell.label
        inputField.setText(inputFormCell.inputFieldText)
        inputField.hint = inputFormCell.inputFieldHint
    }
}
