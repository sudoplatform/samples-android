package com.sudoplatform.telephonyexample

import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class TelephonyExampleFirebaseMessagingService: FirebaseMessagingService() {
    override fun onCreate() {
        super.onCreate()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // send message to VoiceCallActivity to handle an incoming call
        val intent = Intent(applicationContext, VoiceCallActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("incomingCallMessage", message)
        startActivity(intent)
    }
}