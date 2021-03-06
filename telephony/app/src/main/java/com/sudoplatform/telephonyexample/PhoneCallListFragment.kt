package com.sudoplatform.telephonyexample

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread
import com.sudoplatform.sudotelephony.*
import kotlinx.android.synthetic.main.fragment_phone_call_list.*

class PhoneCallListFragment : Fragment(), CallRecordSubscriber {
    private lateinit var app: App
    private lateinit var number: PhoneNumber

    private var callRecordList: ArrayList<CallRecord> = ArrayList()
    private val adapter = CallRecordAdapter(callRecordList) { call ->
        val intent = Intent(app, CallRecordDetailsActivity::class.java)
        intent.putExtra("callRecord", call)
        intent.putExtra("number", number)
        startActivity(intent)
    }

    private lateinit var startCallButton: Button
    private lateinit var recyclerView: RecyclerView

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

        recyclerView = rootView.findViewById(R.id.recyclerView_calls)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(app.applicationContext)

        return rootView
    }

    override fun onResume() {
        super.onResume()
        listCallRecords()
        app.sudoTelephonyClient.calling.subscribeToCallRecords(this, "callRecordSubscriberId")
    }

    private fun listCallRecords() {
        fun fetchPageOfRecords(listToken: String?) {
            app.sudoTelephonyClient.calling.getCallRecords(number, null, listToken) { result ->
                runOnUiThread {
                    when (result) {
                        is Result.Success -> {
                            if (listToken == null) {
                                callRecordList.clear()
                            }
                            callRecordList.addAll(result.value.items)
                            if (result.value.nextToken != null) {
                                fetchPageOfRecords(result.value.nextToken)
                            } else {
                                adapter.notifyDataSetChanged()
                                hideLoading()
                            }
                        }
                        is Result.Absent -> {
                            callRecordList.clear()
                            adapter.notifyDataSetChanged()
                            hideLoading()
                        }
                        is Result.Error -> {
                            hideLoading()
                            AlertDialog.Builder(app.applicationContext)
                                .setTitle("Failed to list call records")
                                .setMessage(result.throwable.toString())
                                .setPositiveButton("Try Again") { _, _ -> listCallRecords() }
                                .setNegativeButton("Cancel") { _, _ -> }
                                .show()
                        }
                    }
                }
            }
        }
        showLoading()
        fetchPageOfRecords(null)
    }

    private fun showLoading() = runOnUiThread {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() = runOnUiThread {
        progressBar?.let {
            it.visibility = View.GONE
        }
    }

    override fun connectionStatusChanged(state: TelephonySubscriber.ConnectionState) {}

    override fun callRecordReceived(callRecord: CallRecord) {
        if (callRecord.phoneNumberId == number.id) {
            listCallRecords()
        }
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
