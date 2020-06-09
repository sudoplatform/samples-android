package com.sudoplatform.telephonyexample

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.sudoplatform.sudotelephony.PhoneMessageConversation
import com.sudoplatform.sudotelephony.PhoneNumber

class PhoneCallListFragment : Fragment() {
    private lateinit var app: App
    private lateinit var number: PhoneNumber

    private lateinit var startCallButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_phone_call_list, container, false)
        startCallButton = rootView.findViewById(R.id.button_startCall)

        startCallButton.setOnClickListener {
            val intent = Intent(app, StartCallActivity::class.java)
            intent.putExtra("number", number)
            startActivity(intent)
        }
        return rootView
    }

    companion object {
        /**
         * @param mainApp The application
         * @param phoneNumber The phone number that will be making calls
         * @return A new instance of fragment PhoneCallListFragment.
         */
        @JvmStatic
        fun newInstance(mainApp: App, phoneNumber: PhoneNumber) =
            PhoneCallListFragment().apply {
                arguments = Bundle().apply {
                    app = mainApp
                    number = phoneNumber
                }
            }
    }
}
