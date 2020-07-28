package com.sudoplatform.telephonyexample

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudotelephony.CallRecord

class CallRecordAdapter(val items: ArrayList<CallRecord>, val itemSelectedListener: (CallRecord) -> Unit) : RecyclerView.Adapter<CallRecordViewHolder>() {
    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallRecordViewHolder {
        return CallRecordViewHolder.inflate(parent)
    }

    override fun onBindViewHolder(holder: CallRecordViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)

        holder.itemView.setOnClickListener {
            itemSelectedListener(item)
        }
    }
}
