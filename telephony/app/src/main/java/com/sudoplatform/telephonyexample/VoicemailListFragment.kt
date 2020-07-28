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
import kotlinx.android.synthetic.main.fragment_voicemail_list.*

class VoicemailListFragment : Fragment(), VoicemailSubscriber {
    private lateinit var app: App
    private lateinit var number: PhoneNumber

    private var voicemailList: ArrayList<Voicemail> = ArrayList()
    private val adapter = VoicemailAdapter(voicemailList) { voicemail ->
        val intent = Intent(app, VoicemailActivity::class.java)
        intent.putExtra("voicemail", voicemail)
        startActivity(intent)
    }

    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_voicemail_list, container, false)

        recyclerView = rootView.findViewById(R.id.recyclerView_voicemails)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(app.applicationContext)

        return rootView
    }

    override fun onResume() {
        super.onResume()
        listVoicemails()
        app.sudoTelephonyClient.calling.subscribeToVoicemails(this, "voicemailSubscriberId")
    }

    private fun listVoicemails() {
        fun fetchPageOfRecords(listToken: String?) {
            app.sudoTelephonyClient.calling.getVoicemails(number, null, listToken) { result ->
                runOnUiThread {
                    when (result) {
                        is Result.Success -> {
                            if (listToken == null) {
                                voicemailList.clear()
                            }
                            voicemailList.addAll(result.value.items)
                            if (result.value.nextToken != null) {
                                fetchPageOfRecords(result.value.nextToken)
                            } else {
                                adapter.notifyDataSetChanged()
                                hideLoading()
                            }
                        }
                        is Result.Absent -> {
                            voicemailList.clear()
                            adapter.notifyDataSetChanged()
                            hideLoading()
                        }
                        is Result.Error -> {
                            hideLoading()
                            AlertDialog.Builder(app.applicationContext)
                                .setTitle("Failed to list voicemails")
                                .setMessage(result.throwable.toString())
                                .setPositiveButton("Try Again") { _, _ -> listVoicemails() }
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

    override fun voicemailUpdated(voicemail: Voicemail) {
        if (voicemail.phoneNumberId == number.id) {
            listVoicemails()
        }
    }

    companion object {
        /**
         * @param mainApp The application
         * @param phoneNumber The phone number that the voicemails belong to
         * @return A new instance of fragment VoicemailListFragment.
         */
        @JvmStatic
        fun newInstance(mainApp: App, phoneNumber: PhoneNumber) =
            VoicemailListFragment().apply {
                arguments = Bundle().apply {
                    app = mainApp
                    number = phoneNumber
                }
            }
    }
}