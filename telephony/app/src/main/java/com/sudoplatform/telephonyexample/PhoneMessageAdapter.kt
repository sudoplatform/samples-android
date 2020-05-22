package com.sudoplatform.telephonyexample

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudotelephony.PhoneMessage
import com.sudoplatform.sudotelephony.PhoneNumber

class PhoneMessageAdapter(val items : ArrayList<PhoneMessage>, val itemSelectedListener: (PhoneMessage) -> Unit) : RecyclerView.Adapter<PhoneMessageViewHolder>() {

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhoneMessageViewHolder {
        return PhoneMessageViewHolder.inflate(parent)
    }

    override fun onBindViewHolder(holder: PhoneMessageViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)

        holder.itemView.setOnClickListener {
            itemSelectedListener(item)
        }
    }
}
