/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.shared

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.virtualcardsexample.databinding.LayoutFormCellBinding

/**
 * A [RecyclerView.ViewHolder] used to describe the input form item view and metadata about its
 * place within the [RecyclerView].
 *
 * The item view contains a title and an [inputField].
 *
 * @property binding The input form item view binding component.
 */
class InputFormViewHolder(private val binding: LayoutFormCellBinding) : RecyclerView.ViewHolder(binding.root) {

    val inputField: EditText = binding.inputField

    companion object {
        fun inflate(parent: ViewGroup): InputFormViewHolder {
            val binding = LayoutFormCellBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ),
                parent,
                false
            )
            return InputFormViewHolder(binding)
        }
    }

    fun bind(inputFormCell: InputFormCell) {
        binding.title.text = inputFormCell.label
        binding.inputField.setText(inputFormCell.inputFieldText)
        binding.inputField.hint = inputFormCell.inputFieldHint
    }
}
