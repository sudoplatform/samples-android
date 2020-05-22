package com.sudoplatform.telephonyexample

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudotelephony.PhoneNumber

class PhoneNumberAdapter(val items : ArrayList<String>, val itemSelectedListener: (String) -> Unit) : RecyclerView.Adapter<PhoneNumberViewHolder>() {

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhoneNumberViewHolder {
        return PhoneNumberViewHolder.inflate(parent)
    }

    override fun onBindViewHolder(holder: PhoneNumberViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)

        holder.itemView.setOnClickListener {
            itemSelectedListener(item)
        }
    }
}

class ProvisionedPhoneNumberAdapter(val items: ArrayList<PhoneNumber>, val itemSelectedListener: (PhoneNumber) -> Unit) : RecyclerView.Adapter<ProvisionedPhoneNumberViewHolder>() {
    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProvisionedPhoneNumberViewHolder {
        return ProvisionedPhoneNumberViewHolder.inflate(parent)
    }

    override fun onBindViewHolder(holder: ProvisionedPhoneNumberViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)

        holder.itemView.setOnClickListener {
            itemSelectedListener(item)
        }
    }

}
