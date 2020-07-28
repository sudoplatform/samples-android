package com.sudoplatform.telephonyexample

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudotelephony.CallRecord
import com.sudoplatform.sudotelephony.CallRecordState
import com.sudoplatform.sudotelephony.type.Direction

class CallRecordViewHolder(view: View): RecyclerView.ViewHolder(view) {

    private val phoneNumberField: TextView = view.findViewById(R.id.text_phone_number)
    private val iconImageView: ImageView = view.findViewById(R.id.iconImageView)

    companion object {
        fun inflate(parent: ViewGroup): CallRecordViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return CallRecordViewHolder(inflater.inflate(R.layout.layout_call_record_cell, parent, false))
        }
    }

    fun bind(callRecord: CallRecord) {
        phoneNumberField.text = formatAsUSNumber(callRecord.remotePhoneNumber)
        iconImageView.setImageResource(when (callRecord.direction) {
            Direction.OUTBOUND -> when (callRecord.state) {
                CallRecordState.UNANSWERED -> R.drawable.ic_call_made_missed_24px
                else -> R.drawable.ic_call_made_24px
            }
            Direction.INBOUND -> when (callRecord.state) {
                CallRecordState.UNANSWERED -> R.drawable.ic_call_received_missed_24px
                else -> R.drawable.ic_call_received_24px
            }
        })
        iconImageView.setColorFilter(phoneNumberField.currentTextColor)
    }
}