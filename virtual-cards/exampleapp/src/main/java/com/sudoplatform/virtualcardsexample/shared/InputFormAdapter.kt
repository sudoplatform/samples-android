/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.shared

import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * A [RecyclerView.Adapter] used to feed [InputFormCell] data to the list view and handle creation
 * and replacement of views for the [InputFormCell] data items.
 *
 * @property items List of [InputFormCell] data items to display.
 * @property onInputChanged Callback which listens for changes to input fields.
 */
class InputFormAdapter(private val items: List<InputFormCell>, private val onInputChanged: (Int, String) -> Unit) : RecyclerView.Adapter<InputFormViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): InputFormViewHolder {
        return InputFormViewHolder.inflate(
            parent
        )
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onBindViewHolder(holder: InputFormViewHolder, position: Int) {
        val inputFormCell = items[position]
        holder.bind(inputFormCell)

        holder.inputField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                onInputChanged(position, p0.toString())
            }
        })
    }
}
