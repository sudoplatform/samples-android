/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages.emailAttachments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.databinding.LayoutEmailAttachmentCellBinding
import com.sudoplatform.sudoemail.types.EmailAttachment

/**
 * A [RecyclerView.ViewHolder] used to describe the [EmailAttachment] item view and metadata about
 * its place within the [RecyclerView].
 *
 * The item view contains a fileName label.
 *
 * @property binding [LayoutEmailAttachmentCellBinding] The [EmailAttachment] item view binding
 *  component.
 */
class EmailAttachmentViewHolder(private val binding: LayoutEmailAttachmentCellBinding) : RecyclerView.ViewHolder(binding.root) {

    companion object {

        const val MAX_FILE_NAME_LENGTH = 25

        fun inflate(parent: ViewGroup): EmailAttachmentViewHolder {
            val binding = LayoutEmailAttachmentCellBinding.inflate(
                LayoutInflater.from(
                    parent.context,
                ),
                parent,
                false,
            )
            return EmailAttachmentViewHolder(binding)
        }
    }

    fun bind(emailAttachment: EmailAttachment) {
        val fileSize = emailAttachment.data.size / 1000
        var fileName = emailAttachment.fileName
        if (fileName.length > MAX_FILE_NAME_LENGTH) {
            fileName = fileName.substring(0, MAX_FILE_NAME_LENGTH) + "..."
        }

        val mimeType = emailAttachment.mimeType
        val icon = when {
            mimeType.contains("image/") -> R.drawable.ic_baseline_attachment_image_24px
            mimeType.contains("video/") -> R.drawable.ic_baseline_attachment_media_24px
            mimeType.contains("application/pdf") -> R.drawable.ic_baseline_attachment_pdf_24px
            else -> R.drawable.ic_baseline_attachment_file_24px
        }

        binding.imageView.setImageResource(icon)
        binding.fileName.text = fileName
        binding.fileSize.text = binding.root.context.resources.getString(
            R.string.file_size, fileSize.toString(),
        )
    }
}
