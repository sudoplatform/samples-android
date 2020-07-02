package com.sudoplatform.telephonyexample

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread
import com.sudoplatform.sudotelephony.*

class ConversationListFragment : Fragment(), PhoneMessageSubscriber {
    private var conversationList: ArrayList<PhoneMessageConversation> = ArrayList()
    private lateinit var app: App
    private lateinit var number: PhoneNumber
    private lateinit var adapter: ConversationAdapter

    private lateinit var composeButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_conversation_list, container, false)
        composeButton = rootView.findViewById(R.id.button_composeMessage)
        recyclerView = rootView.findViewById(R.id.recyclerView_conversations)
        progressBar = rootView.findViewById(R.id.progressBar)
        progressText = rootView.findViewById(R.id.progressText)

        composeButton.setOnClickListener {
            val intent = Intent(app, ComposeMessageActivity::class.java)
            intent.putExtra("number", number)
            startActivity(intent)
        }

        adapter = ConversationAdapter(conversationList) { conversation ->
            val intent = Intent(app, ConversationDetailsActivity::class.java)
            intent.putExtra("conversation", conversation)
            intent.putExtra("number", number)
            startActivity(intent)
        }

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(app.applicationContext)

        return rootView
    }

    override fun onResume() {
        super.onResume()
        listConversations()
        app.sudoTelephonyClient.subscribeToMessages(this, null)
    }

    private fun listConversations() {
        fun fetchPageOfConversations(listToken: String?) {
            app.sudoTelephonyClient.getConversations(number, null, listToken) { result ->
                runOnUiThread {
                    when (result) {
                        is Result.Success -> {
                            if (listToken == null) {
                                conversationList.clear()
                            }
                            conversationList.addAll(result.value.items)
                            if (result.value.nextToken != null) {
                                fetchPageOfConversations(result.value.nextToken)
                            } else {
                                adapter.notifyDataSetChanged()
                                hideLoading()
                            }
                        }
                        is Result.Error -> {
                            hideLoading()
                            AlertDialog.Builder(app.applicationContext)
                                .setTitle("Failed to list conversations")
                                .setMessage(result.throwable.toString())
                                .setPositiveButton("Try Again") { _, _ -> listConversations() }
                                .setNegativeButton("Cancel") { _, _ -> }
                                .show()
                        }
                    }
                }
            }
        }
        showLoading()
        fetchPageOfConversations(null)
    }

    private fun showLoading(text: String? = "") = runOnUiThread {
        progressText.text = text
        progressBar.visibility = View.VISIBLE
        progressText.visibility = View.VISIBLE
    }

    private fun hideLoading() = runOnUiThread {
        progressBar.visibility = View.GONE
        progressText.visibility = View.GONE
    }

    override fun connectionStatusChanged(state: TelephonySubscriber.ConnectionState) { }

    override fun phoneMessageReceived(phoneMessage: PhoneMessage) {
        if (phoneMessage.local == number.phoneNumber) {
            listConversations()
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameter.
         *
         * @param mainApp The application
         * @param phoneNumber The phone number that the conversations belong to
         * @return A new instance of fragment ConversationListFragment.
         */

        @JvmStatic
        fun newInstance(mainApp: App, phoneNumber: PhoneNumber) =
            ConversationListFragment().apply {
                arguments = Bundle().apply {
                    app = mainApp
                    number = phoneNumber
                }
            }
    }
}
