/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.connection

import android.graphics.Color.WHITE
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.View
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.direlayexample.R
import com.sudoplatform.direlayexample.databinding.LayoutPostboxMessagesEventCellBinding
import com.sudoplatform.direlayexample.databinding.LayoutRelayMessageCellBinding
import com.sudoplatform.sudodirelay.types.RelayMessage
import java.util.Date

/**
 * A [RecyclerView.ViewHolder] used to describe the [ConnectionModels.DisplayItem] item view
 * and metadata about its place within the [RecyclerView].
 * There are two types of display item's handled by this view holder: [Event]s and [Message]s.
 */
sealed class ConnectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    open fun bind(item: ConnectionModels.DisplayItem) = Unit

    companion object {
        /** Value appended to start of temporary message's messageId - for UI purposes */
        const val TEMPORARY_SENDING_MESSAGE_PREFIX = "SENDING_"
    }

    /**
     * ViewHolder specific for [Event] items. Event item views contain a text description of the event
     *
     * @property binding The [LayoutPostboxMessagesEventCellBinding] item view binding component.
     */
    internal data class Event(val binding: LayoutPostboxMessagesEventCellBinding) :
        ConnectionViewHolder(binding.root) {
        override fun bind(item: ConnectionModels.DisplayItem) {
            check(item is ConnectionModels.DisplayItem.PostboxEvent)

            binding.eventText.text = item.event
        }
    }

    /**
     * ViewHolder specific for [Message] items.
     * The item view contains a direction colour, ciphertext and a date label.
     * Item view colours and margins are set according to whether the [RelayMessage] has a direction
     * of inbound or outbound
     *
     * @property binding The [LayoutRelayMessageCellBinding] item view binding component.
     */
    internal data class Message(val binding: LayoutRelayMessageCellBinding) :
        ConnectionViewHolder(binding.root) {
        override fun bind(item: ConnectionModels.DisplayItem) {
            check(item is ConnectionModels.DisplayItem.Message)

            val relayMessage = item.data

            if (relayMessage.direction == RelayMessage.Direction.OUTBOUND) {
                val leftMarginedLayoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                )

                leftMarginedLayoutParams.setMargins(
                    pixelDPToInt(binding, 64),
                    0,
                    0,
                    pixelDPToInt(binding, 8)
                )

                binding.root.layoutParams = leftMarginedLayoutParams

                binding.root.backgroundTintList =
                    binding.root.context.getColorStateList(R.color.outboundMessageCellColor)
                binding.messageBody.setTextColor(WHITE)
                binding.timestamp.setTextColor(WHITE)
            } else {
                val rightMarginedLayoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                )

                rightMarginedLayoutParams.setMargins(
                    0,
                    0,
                    pixelDPToInt(binding, 64),
                    pixelDPToInt(binding, 8)
                )

                binding.root.layoutParams = rightMarginedLayoutParams

                binding.root.backgroundTintList =
                    binding.root.context.getColorStateList(R.color.surface)
                binding.messageBody.setTextColor(binding.root.context.getColor(R.color.onSurface))
                binding.timestamp.setTextColor(binding.root.context.getColor(R.color.onSurface))
            }

            binding.messageBody.text = relayMessage.cipherText
            binding.timestamp.text = formatDate(relayMessage.timestamp)
            binding.sendingSpinner.visibility =
                if (relayMessage.messageId.contains(TEMPORARY_SENDING_MESSAGE_PREFIX)) {
                    View.VISIBLE
                } else View.GONE
        }
    }

    internal fun pixelDPToInt(binding: LayoutRelayMessageCellBinding, dp: Int) =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            binding.root.context.resources.displayMetrics
        ).toInt()

    /**
     * Formats a [Date] to a presentable String.
     *
     * @param date The [Date] to be formatted.
     * @return A presentable [String] containing the time of day.
     */
    internal fun formatDate(date: Date): String {
        return DateFormat.format("k:m, d/M/y", date).toString()
    }
}
