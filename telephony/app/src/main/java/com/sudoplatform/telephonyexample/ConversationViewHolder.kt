package com.sudoplatform.telephonyexample

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudotelephony.PhoneMessageConversation

class ConversationViewHolder(view: View): RecyclerView.ViewHolder(view) {

    private val phoneNumberField: TextView = view.findViewById(R.id.text_message_body)

    companion object {
        fun inflate(parent: ViewGroup): ConversationViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return ConversationViewHolder(inflater.inflate(R.layout.layout_conversation_cell, parent, false))
        }
    }

    fun bind(conversation: PhoneMessageConversation) {
        phoneNumberField.text = conversation.latestPhoneMessage?.remote?.let { formatAsUSNumber(it) }
    }
}