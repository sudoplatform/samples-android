package com.sudoplatform.telephonyexample

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudotelephony.PhoneNumber

class PhoneNumberViewHolder(view: View): RecyclerView.ViewHolder(view) {

    private val phoneNumberField: TextView = view.findViewById(R.id.text_message_body)

    companion object {
        fun inflate(parent: ViewGroup): PhoneNumberViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return PhoneNumberViewHolder(inflater.inflate(R.layout.layout_phone_number_cell, parent, false))
        }
    }

    fun bind(phoneNumber: String) {
        phoneNumberField.text = formatAsUSNumber(phoneNumber)
    }
}

class ProvisionedPhoneNumberViewHolder(view: View): RecyclerView.ViewHolder(view) {

    private val phoneNumberField: TextView = view.findViewById(R.id.text_message_body)

    companion object {
        fun inflate(parent: ViewGroup): ProvisionedPhoneNumberViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return ProvisionedPhoneNumberViewHolder(inflater.inflate(R.layout.layout_phone_number_cell, parent, false))
        }
    }

    fun bind(phoneNumber: PhoneNumber) {
        phoneNumberField.text = formatAsUSNumber(phoneNumber.phoneNumber)
    }
}