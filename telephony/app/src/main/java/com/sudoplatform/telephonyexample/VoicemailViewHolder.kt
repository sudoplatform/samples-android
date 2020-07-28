package com.sudoplatform.telephonyexample

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudotelephony.Voicemail
import kotlinx.android.synthetic.main.activity_voicemail.*
import java.text.SimpleDateFormat
import java.util.*

class VoicemailViewHolder(view: View): RecyclerView.ViewHolder(view) {

    private val phoneNumberField: TextView = view.findViewById(R.id.text_phone_number)
    private val timeField: TextView = view.findViewById(R.id.text_voicemail_date)
    private val iconImageView: ImageView = view.findViewById(R.id.iconImageView)

    companion object {
        fun inflate(parent: ViewGroup): VoicemailViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return VoicemailViewHolder(inflater.inflate(R.layout.layout_voicemail_cell, parent, false))
        }
    }

    fun bind(voicemail: Voicemail) {
        phoneNumberField.text = formatAsUSNumber(voicemail.remotePhoneNumber)
        val date = Date.from(voicemail.created)
        val formatter = SimpleDateFormat("MM/dd/yyyy H:mm:aa")
        val formattedDate = formatter.format(date)
        timeField.text = formattedDate
        iconImageView.setImageResource(R.drawable.ic_voicemail_24px)
        iconImageView.setColorFilter(phoneNumberField.currentTextColor)
    }
}