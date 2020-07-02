package com.sudoplatform.telephonyexample

import android.graphics.Color
import android.opengl.Visibility
import android.os.Bundle
import android.os.SystemClock
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils
import com.sudoplatform.sudotelephony.*
import kotlinx.android.synthetic.main.activity_voice_call.*
import java.util.*
import kotlin.Exception

class VoiceCallActivity : AppCompatActivity(), ActiveCallListener {
    private lateinit var app: App
    private lateinit var localNumber: PhoneNumber
    private lateinit var remoteNumber: String
    private lateinit var activeVoiceCall: ActiveVoiceCall
    private var endCallMenu: Menu? = null
    private var isConnected: Boolean = false
    private var durationTimer: Timer? = null
    private var startTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_call)
        title = getString(R.string.title_active_voice_call)

        // Hide the back button
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        // Hide the mute and speaker toggles
        constraintLayoutMute.visibility = View.INVISIBLE
        constraintLayoutSpeaker.visibility = View.INVISIBLE

        app = (application as App)
        localNumber = intent.getParcelableExtra("localNumber") as PhoneNumber
        remoteNumber = intent.getStringExtra("remoteNumber") as String

        yourNumberText.text = formatAsUSNumber(localNumber.phoneNumber)
        remoteNumberText.text = formatAsUSNumber(remoteNumber)

        muteSwitch.setOnCheckedChangeListener { _, isChecked ->
            activeVoiceCall.setMuted(isChecked)
        }

        speakerSwitch.setOnCheckedChangeListener { _, isChecked ->
        }

        initiateCall()
    }

    private fun initiateCall() {
        callStatusText.setTextColor(yourNumberText.textColors.defaultColor)
        callStatusText.text = getString(R.string.status_initiating)

        app.sudoTelephonyClient.createVoiceCall(localNumber, remoteNumber, this)
    }

    override fun onBackPressed() {
        endCall()
        super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.nav_menu_with_end_call_button, menu)
        endCallMenu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            return when(item.itemId) {
                R.id.endCall -> {
                    endCall()
                    return true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun endCall() {
        if (isConnected) {
            activeVoiceCall.disconnect(null)
            onDisconnect()
        } else {
            finish()
            overridePendingTransition(R.anim.no_change, R.anim.slide_down)
        }
    }

    private fun startDurationTimer() {
        startTime = SystemClock.elapsedRealtime()
        val timer = Timer()
        durationTimer = timer
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val currentTime = SystemClock.elapsedRealtime()
                val elapsedSeconds = (currentTime - startTime) / 1000
                val elapsedString = DateUtils.formatElapsedTime(elapsedSeconds)
                ThreadUtils.runOnUiThread {
                    durationText.text = elapsedString
                }
            }

        },0, 1000)
    }

    private fun onConnect() {
        isConnected = true
        callStatusText.setTextColor(getColor(R.color.colorGreen))
        callStatusText.text = getString(R.string.status_active)
        constraintLayoutMute.visibility = View.VISIBLE
        // TODO: Need to get Speaker working
//        constraintLayoutSpeaker.visibility = View.VISIBLE
        startDurationTimer()
    }

    private fun onDisconnect () {
        isConnected = false
        title = getString(R.string.title_voice_call)
        durationTimer?.cancel()
        callStatusText.setTextColor(Color.GRAY)
        callStatusText.text = getString(R.string.status_complete)
        constraintLayoutMute.visibility = View.INVISIBLE
        constraintLayoutSpeaker.visibility = View.INVISIBLE
        endCallMenu?.getItem(0)?.title = getString(R.string.button_done)
    }

    override fun activeVoiceCallDidConnect(call: ActiveVoiceCall) {
        this.activeVoiceCall = call
        onConnect()
    }

    override fun activeVoiceCallDidFailToConnect(exception: Exception) {
        if (isDestroyed) { return }

        callStatusText.setTextColor(getColor(R.color.colorRed))
        callStatusText.text = getString(R.string.status_failed)

        AlertDialog.Builder(this)
            .setTitle("Failed to initiate call")
            .setMessage("${exception}")
            .setPositiveButton("Try Again") { _, _ -> initiateCall() }
            .setNegativeButton("Cancel") { _, _ -> }
            .show()
    }

    override fun activeVoiceCallDidDisconnect(call: ActiveVoiceCall, exception: Exception?) {
        onDisconnect()
    }

    override fun activeVoiceCallDidChangeMuteState(call: ActiveVoiceCall, isMuted: Boolean) {
    }

    override fun activeVoiceCallDidChangeSpeakerState(call: ActiveVoiceCall, isOnSpeaker: Boolean) {
    }

    override fun connectionStatusChanged(state: TelephonySubscriber.ConnectionState) {
    }
}