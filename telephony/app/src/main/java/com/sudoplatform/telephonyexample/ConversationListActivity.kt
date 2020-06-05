package com.sudoplatform.telephonyexample

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.sudoplatform.sudotelephony.*
import kotlinx.android.synthetic.main.activity_conversation_list.*
import kotlin.collections.ArrayList

class ConversationListActivity : AppCompatActivity(), PhoneMessageSubscriber {
    private lateinit var app: App
    private lateinit var number: PhoneNumber
    private lateinit var adapter: ConversationAdapter
    private val conversationList: ArrayList<PhoneMessageConversation> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation_list)

        app = (application as App)
        number = intent.getParcelableExtra("number") as PhoneNumber
        title = "Phone Number"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        textViewYourNumber.text = formatAsUSNumber(number.phoneNumber)

        button_composeMessage.setOnClickListener {
            val intent = Intent(this, ComposeMessageActivity::class.java)
            intent.putExtra("number", number)
            startActivity(intent)
        }

        adapter = ConversationAdapter(conversationList) { conversation ->
            val intent = Intent(this, ConversationDetailsActivity::class.java)
            intent.putExtra("conversation", conversation)
            intent.putExtra("number", number)
            startActivity(intent)
        }

        recyclerView_conversations.adapter = adapter
        recyclerView_conversations.layoutManager = LinearLayoutManager(this)
    }

    override fun onResume() {
        super.onResume()
        listConversations()
        app.sudoTelephonyClient.subscribeToPhoneMessages(this, null)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.nav_menu_with_delete_button, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            return when(item.itemId) {
                R.id.delete -> {
                    AlertDialog.Builder(this)
                        .setTitle("Delete Number")
                        .setMessage("Are you sure you want to delete this number? You will lose access to it and all associated messages.")
                        .setPositiveButton("Delete") { _, _ ->  deleteNumber() }
                        .setNegativeButton("Cancel") { _, _ -> }
                        .show()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun deleteNumber() {
        showLoading("Deleting Number...")
        app.sudoTelephonyClient.deletePhoneNumber(number.phoneNumber) { result ->
            hideLoading()
            runOnUiThread {
                when (result) {
                    is Result.Success -> {
                        AlertDialog.Builder(this)
                            .setTitle("Deleted Number")
                            .setPositiveButton("OK") { _, _ -> finish() }
                            .show()
                    }

                    is Result.Error -> {
                        AlertDialog.Builder(this)
                            .setTitle("Failed to delete number")
                            .setMessage(result.throwable.toString())
                            .setPositiveButton("Try Again") { _, _ -> deleteNumber() }
                            .setNegativeButton("Cancel") { _, _ -> }
                            .show()
                    }
                }
            }
        }
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
                            AlertDialog.Builder(this)
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

    override fun connectionStatusChanged(state: TelephonySubscriber.ConnectionState) {
    }

    override fun phoneMessageReceived(phoneMessage: PhoneMessage) {
        if (phoneMessage.local == number.phoneNumber) {
            listConversations()
        }
    }
}
