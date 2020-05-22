package com.sudoplatform.telephonyexample

import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.sudoplatform.sudotelephony.MediaObject
import com.sudoplatform.sudotelephony.PhoneMessage
import com.sudoplatform.sudotelephony.Result
import com.sudoplatform.sudotelephony.type.MessageDirection
import kotlinx.android.synthetic.main.activity_message_details.*
import kotlinx.android.synthetic.main.activity_message_details.progressBar
import java.text.SimpleDateFormat
import java.util.*

class MessageDetailsActivity : AppCompatActivity() {
    private lateinit var app: App
    private var message: PhoneMessage? = null
    private var toolbarMenu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_details)
        title = "Message Details"
        app = (application as App)
        getMessage(intent.getStringExtra("messageId"))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun updateUI() {
        if (message != null) {
            val direction = if (message!!.direction == MessageDirection.INBOUND) "Incoming" else "Outgoing"
            val type = if (message!!.media.isEmpty()) "SMS" else "MMS"
            val typeText =  "$direction $type"
            textView_messageType.text = typeText

            val date = Date.from(message!!.created)
            val formatter = SimpleDateFormat("MM/dd/yyyy H:mm:aa")
            val formattedDate = formatter.format(date)
            textView_messageTime.text = formattedDate

            textView_messageRemote.text = formatAsUSNumber(message!!.remote)

            textView_messageStatus.text = message!!.state.name

            textView_messageBody.text = message!!.body

            if (message!!.media.isNotEmpty()) {
                loadMMS(message!!.media.first())
            }
        }
    }

    private fun loadMMS(media: MediaObject) {
        try {
            app.sudoTelephonyClient.downloadData(media) { result ->
                runOnUiThread {
                    when (result) {
                        is Result.Success -> {
                            val bitmap =
                                BitmapFactory.decodeByteArray(result.value, 0, result.value.size)
                            imageView_mms.setImageBitmap(bitmap)
                        }
                        is Result.Error -> {
                            AlertDialog.Builder(this)
                                .setTitle("Failed to load media")
                                .setMessage("${result.throwable}")
                                .setPositiveButton("Try Again") { _, _ -> loadMMS(media) }
                                .setNegativeButton("Cancel") { _, _ -> }
                                .show()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AlertDialog.Builder(this)
                .setTitle("Failed to load media")
                .setMessage("$e")
                .setPositiveButton("Try Again") { _, _ -> loadMMS(media) }
                .setNegativeButton("Cancel") { _, _ -> }
                .show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.nav_menu_with_delete_button, menu)
        toolbarMenu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            return when(item.itemId) {
                R.id.delete -> {
                    deleteMessage()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getMessage(id: String) {
        showLoading()
        app.sudoTelephonyClient.getMessage(id) { result ->
            hideLoading()
            runOnUiThread {
                when (result) {
                    is Result.Success -> {
                        message = result.value
                        updateUI()
                    }
                    is Result.Error -> {
                        AlertDialog.Builder(this)
                            .setTitle("Failed to load message")
                            .setMessage("${result.throwable}")
                            .setPositiveButton("Try Again") { _, _ ->  getMessage(id) }
                            .setNegativeButton("Cancel") { _, _ -> }
                            .show()
                    }
                }
            }
        }
    }

    private fun deleteMessage() {
        message?.id?.let { messageId ->
            showLoading()
            app.sudoTelephonyClient.deleteMessage(messageId) { result ->
                hideLoading()
                runOnUiThread {
                    when (result) {
                        is Result.Success -> {
                            AlertDialog.Builder(this)
                                .setTitle("Message Deleted")
                                .setPositiveButton("OK") { _, _ ->  finish() }
                                .show()
                        }
                        is Result.Error -> {
                            AlertDialog.Builder(this)
                                .setTitle("Failed to delete message")
                                .setMessage("${result.throwable}")
                                .setPositiveButton("Try Again") { _, _ ->  deleteMessage() }
                                .setNegativeButton("Cancel") { _, _ -> }
                                .show()
                        }
                    }
                }
            }
        }
    }

    private fun showLoading() = runOnUiThread {
        toolbarMenu?.getItem(0)?.isEnabled = false
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() = runOnUiThread {
        toolbarMenu?.getItem(0)?.isEnabled = true
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        progressBar.visibility = View.GONE
    }
}
