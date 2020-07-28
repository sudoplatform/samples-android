package com.sudoplatform.telephonyexample

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
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
import com.google.firebase.messaging.RemoteMessage
import com.sudoplatform.sudotelephony.*
import kotlinx.android.synthetic.main.activity_voice_call.*
import java.util.*

class VoiceCallActivity : AppCompatActivity(), ActiveCallListener, IncomingCallNotificationListener {
    private lateinit var app: App
    private lateinit var localNumber: PhoneNumber
    private lateinit var remoteNumber: String
    private lateinit var activeVoiceCall: ActiveVoiceCall
    private var incomingCall: IncomingCall? = null
    private var endCallMenu: Menu? = null
    private var isConnected: Boolean = false
    private var durationTimer: Timer? = null
    private var startTime: Long = 0

    // use a broadcast receiver to dismiss any extra VoiceCallActivities since
    // the messaging service will start a new one upon a canceled call
    private var unregisteredReceiver = false
    private val finishActivityReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_call)
        title = getString(R.string.title_active_voice_call)

        // Hide the back button
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        // Hide the mute and speaker toggles
        constraintLayoutMute.visibility = View.INVISIBLE

        app = (application as App)
        if (intent.hasExtra("incomingCallMessage")) {
            // receiving a call
            val message = intent.getParcelableExtra<RemoteMessage>("incomingCallMessage")
            app.sudoTelephonyClient.calling.handleIncomingPushNotification(message.data, this)
        } else {
            // making a call
            localNumber = intent.getParcelableExtra("localNumber") as PhoneNumber
            remoteNumber = intent.getStringExtra("remoteNumber") as String

            yourNumberText.text = formatAsUSNumber(localNumber.phoneNumber)
            remoteNumberText.text = formatAsUSNumber(remoteNumber)

            initiateCall()
        }
        registerReceiver(finishActivityReceiver, IntentFilter("finishActivity"))

        muteSwitch.setOnCheckedChangeListener { _, isChecked ->
            activeVoiceCall.setMuted(isChecked)
        }

        speakerSwitch.setOnCheckedChangeListener { _, isChecked ->
            activeVoiceCall.setAudioOutputToSpeaker(isChecked)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!unregisteredReceiver) {
            unregisterReceiver(finishActivityReceiver)
        }
    }

    private fun initiateCall() {
        callStatusText.setTextColor(yourNumberText.textColors.defaultColor)
        callStatusText.text = getString(R.string.status_initiating)

        app.sudoTelephonyClient.calling.createVoiceCall(localNumber, remoteNumber, this)
    }

    override fun onBackPressed() {
        endCall()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.nav_menu_with_end_call_button, menu)
        endCallMenu = menu
        // hide the end call button if awaiting user response to incoming call
        if (incomingCall != null) {
            endCallMenu?.getItem(0)?.isEnabled = false
        }
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
            // dismiss any active call screens
            finish()
            sendBroadcast(Intent("finishActivity"))
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

    private fun onIncoming() {
        isConnected = false
        callStatusText.setTextColor(getColor(R.color.colorGreen))
        callStatusText.text = getString(R.string.status_incoming)
        constraintLayoutDuration.visibility = View.GONE
        constraintLayoutMute.visibility = View.GONE
        constraintLayoutYourNumber.visibility = View.GONE
        button_accept.visibility = View.VISIBLE
        button_decline.visibility = View.VISIBLE
        remoteNumberText.text = formatAsUSNumber(incomingCall?.remoteNumber ?: "")
        button_accept.setOnClickListener {
            // accept call
            unregisterReceiver(finishActivityReceiver)
            unregisteredReceiver = true
            incomingCall?.acceptWithListener(this)
            incomingCall = null
        }
        button_decline.setOnClickListener {
            // decline call
            unregisterReceiver(finishActivityReceiver)
            unregisteredReceiver = true
            incomingCall?.decline()
            incomingCall = null
            endCall()
        }
    }

    private fun onConnect() {
        isConnected = true
        endCallMenu?.getItem(0)?.isEnabled = true
        callStatusText.setTextColor(getColor(R.color.colorGreen))
        callStatusText.text = getString(R.string.status_active)
        constraintLayoutMute.visibility = View.VISIBLE
        constraintLayoutDuration.visibility = View.VISIBLE
        button_accept.visibility = View.GONE
        button_decline.visibility = View.GONE
        constraintLayoutSpeaker.visibility = View.VISIBLE
        startDurationTimer()
    }

    private fun onDisconnect () {
        isConnected = false
        title = getString(R.string.title_voice_call)
        durationTimer?.cancel()
        callStatusText.setTextColor(Color.GRAY)
        callStatusText.text = getString(R.string.status_complete)
        constraintLayoutMute.visibility = View.INVISIBLE
        endCallMenu?.getItem(0)?.title = getString(R.string.button_done)
    }

    // ActiveCallListener methods

    override fun activeVoiceCallDidConnect(call: ActiveVoiceCall) {
        this.activeVoiceCall = call
        onConnect()
    }

    override fun activeVoiceCallDidFailToConnect(exception: Exception) = runOnUiThread {
        if (isDestroyed) { return@runOnUiThread }

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

    override fun activeVoiceCallDidChangeAudioDevice(
        call: ActiveVoiceCall,
        audioDevice: VoiceCallAudioDevice
    ) {
        speakerSwitch.isChecked = audioDevice == VoiceCallAudioDevice.SPEAKERPHONE
    }

    // IncomingCallNotificationListener methods
    override fun incomingCallReceived(call: IncomingCall) {
        incomingCall = call
        runOnUiThread {
            onIncoming()
        }
    }

    override fun incomingCallCanceled(call: IncomingCall, error: Throwable?) {
        endCall()
    }

    override fun connectionStatusChanged(state: TelephonySubscriber.ConnectionState) {
    }
}
