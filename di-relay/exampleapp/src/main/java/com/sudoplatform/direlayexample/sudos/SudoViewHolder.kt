/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.sudos

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.direlayexample.databinding.LayoutItemCellBinding
import com.sudoplatform.sudoprofiles.Sudo

/**
 * A [RecyclerView.ViewHolder] used to describe the [Sudo] item view and metadata about its place
 * within the [RecyclerView].
 *
 * The item view contains a label [TextView] of the [Sudo] name.
 *
 * @property binding The [Sudo] item view binding component.
 */
class SudoViewHolder(private val binding: LayoutItemCellBinding) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun inflate(parent: ViewGroup): SudoViewHolder {
            val binding = LayoutItemCellBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ),
                parent,
                false
            )
            return SudoViewHolder(binding)
        }
    }

    fun bind(sudoName: String) {
        binding.name.text = sudoName
    }
}
