package com.sudoplatform.telephonyexample

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudotelephony.Voicemail

class VoicemailAdapter(val items: ArrayList<Voicemail>, val itemSelectedListener: (Voicemail) -> Unit) : RecyclerView.Adapter<VoicemailViewHolder>() {
    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoicemailViewHolder {
        return VoicemailViewHolder.inflate(parent)
    }

    override fun onBindViewHolder(holder: VoicemailViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)

        holder.itemView.setOnClickListener {
            itemSelectedListener(item)
        }
    }
}
