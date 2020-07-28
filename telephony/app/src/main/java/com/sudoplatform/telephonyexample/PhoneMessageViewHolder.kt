package com.sudoplatform.telephonyexample

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudotelephony.PhoneMessage
import com.sudoplatform.sudotelephony.type.Direction
import java.text.SimpleDateFormat
import java.util.*

class PhoneMessageViewHolder(view: View): RecyclerView.ViewHolder(view) {

    private val messageBodyText: TextView = view.findViewById(R.id.text_message_body)
    private val messageDateText: TextView = view.findViewById(R.id.text_message_date)
    private val sendReceiveImageView: ImageView = view.findViewById(R.id.imageView_send_receive)

    companion object {
        fun inflate(parent: ViewGroup): PhoneMessageViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return PhoneMessageViewHolder(inflater.inflate(R.layout.layout_phone_message_cell, parent, false))
        }
    }

    fun bind(message: PhoneMessage) {
        messageBodyText.text = message.body
        val date = Date.from(message.created)
        val formatter = SimpleDateFormat("MM/dd/yyyy H:mm:aa")
        val formattedDate = formatter.format(date)
        messageDateText.text = formattedDate
        if (message.direction == Direction.OUTBOUND) {
            sendReceiveImageView.setImageResource(R.drawable.ic_call_made_24px)
        } else {
            sendReceiveImageView.setImageResource(R.drawable.ic_call_received_24px)
        }
    }
}
