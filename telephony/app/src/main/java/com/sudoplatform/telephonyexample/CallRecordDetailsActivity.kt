package com.sudoplatform.telephonyexample

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.sudoplatform.sudotelephony.CallRecord
import com.sudoplatform.sudotelephony.CallRecordSubscriber
import com.sudoplatform.sudotelephony.Result
import com.sudoplatform.sudotelephony.TelephonySubscriber
import com.sudoplatform.sudotelephony.type.Direction
import kotlinx.android.synthetic.main.activity_call_record_details.*
import java.text.SimpleDateFormat
import java.util.*

class CallRecordDetailsActivity : AppCompatActivity(), CallRecordSubscriber {
    private lateinit var app: App
    private lateinit var callRecord: CallRecord
    private var toolbarMenu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_record_details)
        title = "Call Details"
        app = (application as App)
        callRecord = intent.getParcelableExtra("callRecord")
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        displayCallRecord()

        app.sudoTelephonyClient.calling.subscribeToCallRecords(this, "callRecordDetailSubscriberId")
    }

    override fun onSupportNavigateUp(): Boolean {
        app.sudoTelephonyClient.calling.unsubscribeFromCallRecords("callRecordDetailSubscriberId")
        onBackPressed()
        return true
    }

    private fun displayCallRecord() {
        textView_status.text = callRecord.state.name

        val direction = if (callRecord.direction == Direction.INBOUND) "Incoming" else "Outgoing"
        textView_direction.text = direction

        val date = Date.from(callRecord.created)
        val formatter = SimpleDateFormat("MM/dd/yyyy H:mm:aa")
        val formattedDate = formatter.format(date)
        textView_time.text = formattedDate

        textView_localNumber.text = formatAsUSNumber(callRecord.localPhoneNumber)
        textView_remoteNumber.text = formatAsUSNumber(callRecord.remotePhoneNumber)

        textView_duration.text = DateUtils.formatElapsedTime(callRecord.durationSeconds.toLong())

        if (callRecord.voicemail != null) {
            imageView_voicemail.visibility = View.VISIBLE
            textView_voicemail.visibility = View.GONE
            button_voicemail.setOnClickListener {
                val intent = Intent(app, VoicemailActivity::class.java)
                intent.putExtra("callRecord", callRecord)
                startActivity(intent)
            }
        } else {
            imageView_voicemail.visibility = View.GONE
            textView_voicemail.visibility = View.VISIBLE
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
            return when (item.itemId) {
                R.id.delete -> {
                    deleteCallRecord()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun deleteCallRecord() {
        callRecord.id.let { callRecordId ->
            showLoading()
            app.sudoTelephonyClient.calling.deleteCallRecord(callRecordId) { result ->
                hideLoading()
                runOnUiThread {
                    when (result) {
                        is Result.Success -> {
                            AlertDialog.Builder(this)
                                .setTitle("Call Record Deleted")
                                .setPositiveButton("OK") { _, _ ->  finish() }
                                .show()
                        }
                        is Result.Error -> {
                            AlertDialog.Builder(this)
                                .setTitle("Failed to delete call record")
                                .setMessage("${result.throwable}")
                                .setPositiveButton("Try Again") { _, _ ->  deleteCallRecord() }
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

    override fun callRecordReceived(callRecord: CallRecord) {
        if (callRecord.id == this.callRecord.id) {
            this.callRecord = callRecord
            runOnUiThread {
                displayCallRecord()
            }
        }
    }

    override fun connectionStatusChanged(state: TelephonySubscriber.ConnectionState) {
    }
}
