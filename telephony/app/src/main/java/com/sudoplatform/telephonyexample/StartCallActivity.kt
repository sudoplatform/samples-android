package com.sudoplatform.telephonyexample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils
import com.sudoplatform.sudotelephony.*
import kotlinx.android.synthetic.main.activity_start_call.*

class StartCallActivity : AppCompatActivity() {
    private lateinit var app: App
    private lateinit var localNumber: PhoneNumber
    private var sendMenu: Menu? = null
    private val MIC_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_call)

        app = (application as App)
        localNumber = intent.getParcelableExtra("number") as PhoneNumber
        title = getString(R.string.title_make_voice_call)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        recipientClearButton.isEnabled = false
        recipientClearButton.setOnClickListener {
            recipientField.setText("")
        }

        recipientField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                recipientClearButton.isEnabled = s.count() > 0
                sendMenu?.getItem(0)?.isEnabled = s.count() > 0
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        recipientField.requestFocus()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            MIC_PERMISSION_REQUEST_CODE
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        // Handle permission result
        if (requestCode == MIC_PERMISSION_REQUEST_CODE) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // permission not granted
            } else {
                // permission granted
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.nav_menu_with_start_call_button, menu)
        sendMenu = menu
        sendMenu?.getItem(0)?.isEnabled = false
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            return when(item.itemId) {
                R.id.startCall -> {
                    startVoiceCall()
                    return true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun startVoiceCall() {
        val remoteNumber = recipientField.text.toString()

        val intent = Intent(app, VoiceCallActivity::class.java)
        intent.putExtra("localNumber", localNumber)
        intent.putExtra("remoteNumber", remoteNumber)
        startActivity(intent)
        // Slide up the new activity modally
        overridePendingTransition(R.anim.slide_up, R.anim.no_change)
    }
}
