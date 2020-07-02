package com.sudoplatform.telephonyexample

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.sudoplatform.sudotelephony.*
import kotlinx.android.synthetic.main.activity_conversation_details.*

class ConversationDetailsActivity : AppCompatActivity(), PhoneMessageSubscriber {
    private lateinit var app: App
    private var conversation: PhoneMessageConversation? = null
    private lateinit var adapter: PhoneMessageAdapter
    private val messageList: ArrayList<PhoneMessage> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation_details)
        title = "Conversation"

        app = (application as App)

        adapter = PhoneMessageAdapter(messageList) { phoneMessage ->
            // phone message selected
            val intent = Intent(this, MessageDetailsActivity::class.java)
            intent.putExtra("message", phoneMessage)
            startActivity(intent)
        }
        recyclerView_messages.adapter = adapter
        recyclerView_messages.layoutManager = LinearLayoutManager(this)

        conversation = intent.getParcelableExtra("conversation")
        textView_yourNumber.text = conversation?.latestPhoneMessage?.local?.let { formatAsUSNumber(it) }
        textView_remoteNumber.text = conversation?.latestPhoneMessage?.remote?.let { formatAsUSNumber(it) }

        val phoneNumber = intent.getParcelableExtra("number") as PhoneNumber
        button_composeMessage2.setOnClickListener {
            val intent = Intent(this, ComposeMessageActivity::class.java)
            intent.putExtra("number", phoneNumber)
            intent.putExtra("remoteNumber", conversation?.latestPhoneMessage?.remote)
            app.sudoTelephonyClient.unsubscribeFromPhoneMessages("subId")
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        app.sudoTelephonyClient.subscribeToMessages(this, "subId")
        getMessages()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun getMessages() {
        fun fetchPageOfMessages(listToken: String?) {
            app.sudoTelephonyClient.getMessages(conversation!!.id, null, listToken) { result ->
                runOnUiThread {
                    when (result) {
                        is Result.Success -> {
                            if (listToken == null) {
                                messageList.clear()
                            }
                            messageList.addAll(result.value.items)
                            if (result.value.nextToken != null) {
                                fetchPageOfMessages(result.value.nextToken)
                            } else {
                                adapter.notifyDataSetChanged()
                                hideLoading()
                            }
                        }
                        is Result.Error -> {
                            hideLoading()
                            messageList.clear()
                            adapter.notifyDataSetChanged()
                            AlertDialog.Builder(this)
                                .setTitle("Failed to get messages")
                                .setMessage(result.throwable.toString())
                                .setPositiveButton("Try Again") { _, _ -> getMessages() }
                                .setNegativeButton("Cancel") { _, _ -> }
                                .show()
                        }
                        is Result.Absent -> {
                            hideLoading()
                            if (listToken == null) {
                                messageList.clear()
                            }
                            adapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        }
        showLoading()
        fetchPageOfMessages(null)
    }

    private fun showLoading() = runOnUiThread {
        progressBar.visibility = View.VISIBLE
        button_composeMessage2.isEnabled = false
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    private fun hideLoading() = runOnUiThread {
        progressBar.visibility = View.GONE
        button_composeMessage2.isEnabled = true
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    // subscription methods
    override fun connectionStatusChanged(state: TelephonySubscriber.ConnectionState) {
    }

    override fun phoneMessageReceived(phoneMessage: PhoneMessage) {
        // only get messages when one is received for this phone number
        if (phoneMessage.local == conversation?.latestPhoneMessage?.local) {
            getMessages()
        }
    }
}

