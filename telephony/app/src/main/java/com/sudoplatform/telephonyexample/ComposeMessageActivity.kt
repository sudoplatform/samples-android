package com.sudoplatform.telephonyexample

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore.Images.Media
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.sudoplatform.sudotelephony.PhoneNumber
import com.sudoplatform.sudotelephony.Result
import kotlinx.android.synthetic.main.activity_compose_message.*
import kotlinx.android.synthetic.main.activity_compose_message.progressBar
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.URL

class ComposeMessageActivity : AppCompatActivity() {

    private val IMAGE_PICK_CODE = 1000
    private var attachmentImageUri: Uri? = null
    private var attachmentImageUrl: URL? = null
    private var toolbarMenu: Menu? = null
    private lateinit var localNumber: PhoneNumber
    private lateinit var app: App

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compose_message)
        title = "Compose Message"

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        app = (application as App)
        val parcelablePhoneNumber = intent.getParcelableExtra("number") as ParcelablePhoneNumber
        this.localNumber = parcelablePhoneNumber.toPhoneNumber()
        val remoteNumber: String? = intent.getStringExtra("remoteNumber")
        editText_recipientPhone.setText(remoteNumber)

        // open image picker for attachments / delete if image already added
        button_attachment.setOnClickListener {
            if (attachmentImageUrl != null) {
                attachmentImageUrl = null
                imageView.setImageURI(null)
                button_attachment.setImageResource(R.drawable.ic_attach_file_24px)
            } else {
                val intent = Intent(Intent.ACTION_PICK)
                intent.type = "image/*"
                startActivityForResult(intent, IMAGE_PICK_CODE)
            }
        }

        // monitor phone number and message text fields so the send button can be enabled when ready
        editText_recipientPhone.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                toolbarMenu?.getItem(0)?.isEnabled =
                    s.count() > 0 && editText_message.text.toString().isNotBlank()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        editText_message.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                toolbarMenu?.getItem(0)?.isEnabled =
                    s.count() > 0 && editText_recipientPhone.text.toString().isNotBlank()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })
    }

    //handle result of picked image
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE){
            attachmentImageUri = data?.data
            val bitmap = Media.getBitmap(contentResolver, attachmentImageUri)
            val wrapper = ContextWrapper(applicationContext)

            // Initialize a new file instance to save bitmap object
            var file = wrapper.getDir("Images", Context.MODE_PRIVATE)
            file = File(file, "myFile.jpg")

            try{
                // Compress the bitmap and save in jpg format
                val stream: OutputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                stream.flush()
                stream.close()
                attachmentImageUrl = URL("file", "", file.absolutePath)
            }catch (e: IOException){
                e.printStackTrace()
            }

            imageView.setImageURI(attachmentImageUri)
            button_attachment.setImageResource(R.drawable.ic_delete_outline_24px)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.nav_menu_compose_message, menu)
        toolbarMenu = menu
        // disable send button
        toolbarMenu?.getItem(0)?.isEnabled = false
        return true
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        return when(menuItem.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.send -> {
                sendMessage()
                true
            }
            else -> super.onOptionsItemSelected(menuItem)
        }
    }

    private fun sendMessage() {
        val destinationNumber = editText_recipientPhone.text.toString()
        val message = editText_message.text.toString()

        if (attachmentImageUrl != null) {
            // send MMS message with selected image attachment
            showLoading()
            app.sudoTelephonyClient.sendMMSMessage(localNumber, destinationNumber, message, attachmentImageUrl!!) { result ->
                hideLoading()
                runOnUiThread {
                    when (result) {
                        is Result.Success -> {
                            AlertDialog.Builder(this)
                                .setTitle("Message Sent")
                                .setPositiveButton("OK") { _, _ -> finish() }
                                .show()
                        }
                        is Result.Error -> {
                            AlertDialog.Builder(this)
                                .setTitle("Message Failed to Send")
                                .setMessage("${result.throwable}")
                                .setPositiveButton("Try Again") { _, _ -> sendMessage() }
                                .setNegativeButton("Cancel") { _, _ -> }
                                .show()
                        }
                    }
                }
            }
        } else {
            // send SMS message
            showLoading()
            app.sudoTelephonyClient.sendSMSMessage(localNumber, destinationNumber, message) { result ->
                hideLoading()
                runOnUiThread {
                    when (result) {
                        is Result.Success -> {
                            AlertDialog.Builder(this)
                                .setTitle("Message Sent")
                                .setPositiveButton("OK") { _, _ -> finish() }
                                .show()
                        }
                        is Result.Error -> {
                            AlertDialog.Builder(this)
                                .setTitle("Message Failed to Send")
                                .setMessage("${result.throwable}")
                                .setPositiveButton("Try Again") { _, _ -> sendMessage() }
                                .setNegativeButton("Cancel") { _, _ -> }
                                .show()
                        }
                    }
                }
            }
        }
    }

    private fun showLoading() = runOnUiThread {
        progressBar.visibility = View.VISIBLE
        editText_recipientPhone.isEnabled = false
        editText_message.isEnabled = false
        toolbarMenu?.getItem(0)?.isEnabled = false
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    private fun hideLoading() = runOnUiThread {
        progressBar.visibility = View.GONE
        editText_recipientPhone.isEnabled = true
        editText_message.isEnabled = true
        toolbarMenu?.getItem(0)?.isEnabled = true
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}

