package com.sudoplatform.telephonyexample

import android.app.AlertDialog
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaDataSource
import android.media.MediaPlayer
import android.os.Bundle
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.sudoplatform.sudotelephony.CallRecord
import com.sudoplatform.sudotelephony.Result
import com.sudoplatform.sudotelephony.Voicemail
import kotlinx.android.synthetic.main.activity_voicemail.*
import java.text.SimpleDateFormat
import java.util.*


class VoicemailActivity : AppCompatActivity() {
    private var voicemail: Voicemail? = null
    private var callRecord: CallRecord? = null
    private lateinit var app: App
    private lateinit var mediaPlayer: MediaPlayer
    private var audioSource: MediaDataSource? = null
    private var toolbarMenu: Menu? = null
    private lateinit var audioManager: AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voicemail)
        title = "Voicemail"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        // set audio to play through voicecall speaker
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = false
        mediaPlayer = MediaPlayer()

        app = (application as App)
        if (intent.hasExtra("voicemail")) {
            voicemail = intent.getParcelableExtra("voicemail")
            displayVoicemail()
        }
        if (intent.hasExtra("callRecord")) {
            callRecord = intent.getParcelableExtra("callRecord")
            displayVoicemail()
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
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
                    deleteVoicemail()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun displayVoicemail() {
        val date = Date.from(voicemail?.created ?: callRecord?.created)
        val formatter = SimpleDateFormat("MM/dd/yyyy H:mm:aa")
        val formattedDate = formatter.format(date)
        timeText.text = formattedDate
        yourNumberText.text = formatAsUSNumber(voicemail?.localPhoneNumber ?: callRecord?.localPhoneNumber ?: "")
        remoteNumberText.text = formatAsUSNumber(voicemail?.remotePhoneNumber ?: callRecord?.remotePhoneNumber ?: "")
        durationText.text = DateUtils.formatElapsedTime((voicemail?.durationSeconds ?: callRecord?.voicemail?.durationSeconds ?: 1).toLong())

        button_play.setOnClickListener {
            audioSource?.let {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                    button_play.text = "Play"
                } else {
                    playAudio()
                }
            } ?: run {
                downloadAudio()
            }
        }

        button_speaker.setOnClickListener {
            audioManager.isSpeakerphoneOn = !audioManager.isSpeakerphoneOn
            button_speaker.text = if (audioManager.isSpeakerphoneOn) "Speaker On" else "Speaker Off"
        }
    }

    fun deleteVoicemail() {
        showLoading("Deleting voicemail...")
        app.sudoTelephonyClient.calling.deleteVoicemail(voicemail?.id ?: callRecord?.voicemail?.id ?: "") { result ->
            hideLoading()
            runOnUiThread {
                when (result) {
                    is Result.Success -> {
                        finish()
                    }
                    is Result.Error -> {
                        AlertDialog.Builder(this)
                            .setTitle("Failed to delete voicemail")
                            .setMessage("${result.throwable}")
                            .setPositiveButton("Try Again") { _, _ -> deleteVoicemail() }
                            .setNegativeButton("Cancel") { _, _ -> }
                            .show()
                    }
                }
            }
        }
    }

    fun downloadAudio() {
        val media = (voicemail?.media ?: callRecord?.voicemail?.media) ?: return
        showLoading("downloading audio...")
        app.sudoTelephonyClient.downloadData(media) { result ->
            hideLoading()
            when (result) {
                is Result.Success -> {
                    audioSource = ByteArrayMediaDataSource(result.value)
                    playAudio()
                }
                is Result.Error -> {
                    runOnUiThread {
                        AlertDialog.Builder(this)
                            .setTitle("Failed to download audio")
                            .setMessage("${result.throwable}")
                            .setPositiveButton("Try Again") { _, _ -> downloadAudio() }
                            .setNegativeButton("Cancel") { _, _ -> }
                            .show()
                    }
                }
            }
        }
    }

    fun playAudio() {
        mediaPlayer.reset()
        mediaPlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        mediaPlayer.setDataSource(audioSource)
        mediaPlayer.prepare()
        mediaPlayer.setOnCompletionListener {
            button_play.text = "Play"
        }
        mediaPlayer.start()
        button_play.text = "Stop"
    }

    private fun showLoading(text: String? = "") = runOnUiThread {
        toolbarMenu?.getItem(0)?.isEnabled = false
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        progressBar.translationZ = 1F
        progressText.translationZ = 1F
        progressText.text = text
        progressText.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() = runOnUiThread {
        toolbarMenu?.getItem(0)?.isEnabled = true
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        progressText.visibility = View.GONE
        progressBar.visibility = View.GONE
    }
}

class ByteArrayMediaDataSource(private val data: ByteArray) : MediaDataSource() {
    override fun readAt(
        position: Long,
        buffer: ByteArray,
        offset: Int,
        size: Int
    ): Int {
        if (position >= data.size) return -1 // -1 indicates EOF

        val endPosition: Int = (position + size).toInt()
        var size2: Int = size
        if (endPosition > data.size)
            size2 -= endPosition - data.size

        System.arraycopy(data, position.toInt(), buffer, offset, size2)
        return size2
    }

    override fun getSize(): Long {
        return data.size.toLong()
    }

    override fun close() {
    }
}