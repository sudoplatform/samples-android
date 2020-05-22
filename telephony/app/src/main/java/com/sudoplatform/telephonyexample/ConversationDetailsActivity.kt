package com.sudoplatform.telephonyexample

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.sudoplatform.sudotelephony.PhoneMessage
import com.sudoplatform.sudotelephony.PhoneMessageConversation
import com.sudoplatform.sudotelephony.PhoneMessageSubscriber
import com.sudoplatform.sudotelephony.Result
import kotlinx.android.synthetic.main.activity_conversation_details.*

class ConversationDetailsActivity : AppCompatActivity(), PhoneMessageSubscriber {
    private lateinit var app: App
    private lateinit var conversationId: String
    private var conversation: PhoneMessageConversation? = null
    private lateinit var adapter: PhoneMessageAdapter
    private val messageList: ArrayList<PhoneMessage> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation_details)
        title = "Conversation"

        app = (application as App)

        adapter = PhoneMessageAdapter(messageList) { item ->
            // phone message selected
            val intent = Intent(this, MessageDetailsActivity::class.java)
            intent.putExtra("messageId", item.id)
            startActivity(intent)
        }
        recyclerView_messages.adapter = adapter
        recyclerView_messages.layoutManager = LinearLayoutManager(this)

        conversationId = intent.getStringExtra("conversation")
        getConversation()

        val parcelablePhoneNumber = intent.getParcelableExtra("number") as ParcelablePhoneNumber
        button_composeMessage2.setOnClickListener {
            val intent = Intent(this, ComposeMessageActivity::class.java)
            intent.putExtra("number", parcelablePhoneNumber)
            intent.putExtra("remoteNumber", conversation?.latestPhoneMessage?.remote)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        getMessages()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun getConversation() {
        showLoading()
        app.sudoTelephonyClient.getConversation(conversationId) { result ->
            runOnUiThread {
                when (result) {
                    is Result.Success -> {
                        conversation = result.value
                        getMessages()
                        updateUI()
                    }
                    is Result.Error -> {
                        hideLoading()
                    }
                }
            }
        }
    }

    private fun getMessages() {
        showLoading()
        app.sudoTelephonyClient.getMessages(conversationId, null, null) { result ->
            hideLoading()
            runOnUiThread {
                when (result) {
                    is Result.Success -> {
                        messageList.clear()
                        messageList.addAll(result.value.items)
                        adapter.notifyDataSetChanged()
                    }
                    is Result.Error -> {
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
                        messageList.clear()
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }

        app.sudoTelephonyClient.subscribeToPhoneMessages(this, null)
    }

    private fun updateUI() {
        textView_yourNumber.text = conversation?.latestPhoneMessage?.local?.let { formatAsUSNumber(it) }
        textView_remoteNumber.text = conversation?.latestPhoneMessage?.remote?.let { formatAsUSNumber(it) }
    }

    private fun showLoading() = runOnUiThread {
        progressBar.visibility = View.VISIBLE
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    private fun hideLoading() = runOnUiThread {
        progressBar.visibility = View.GONE
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    // subscription methods
    override fun connectionStatusChanged(state: PhoneMessageSubscriber.ConnectionState) {
    }

    override fun phoneMessageReceived(phoneMessage: PhoneMessage) {
        // only get messages when one is received for this phone number
        if (phoneMessage.local == conversation?.latestPhoneMessage?.local) {
            getMessages()
        }
    }
}

