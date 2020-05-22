package com.sudoplatform.telephonyexample

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudotelephony.PhoneMessageConversation
import com.sudoplatform.sudotelephony.PhoneNumber
import com.sudoplatform.sudotelephony.fragment.Conversation

class ConversationAdapter(val items: ArrayList<PhoneMessageConversation>, val itemSelectedListener: (PhoneMessageConversation) -> Unit) : RecyclerView.Adapter<ConversationViewHolder>() {
    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        return ConversationViewHolder.inflate(parent)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)

        holder.itemView.setOnClickListener {
            itemSelectedListener(item)
        }
    }
}
