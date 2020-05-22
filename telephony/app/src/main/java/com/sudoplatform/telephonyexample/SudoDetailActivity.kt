package com.sudoplatform.telephonyexample

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudotelephony.PhoneNumber
import com.sudoplatform.sudotelephony.Result
import com.sudoplatform.sudotelephony.type.PhoneNumberState
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_sudo_detail.*
import java.io.Serializable
import java.util.*

@Parcelize
data class ParcelablePhoneNumber(
    var id: String,
    val phoneNumber: String,
    val state: PhoneNumberState,
    val version: Int,
    val created: Date,
    val updated: Date
): Parcelable {
    companion object {
        fun fromPhoneNumber(phoneNumber: PhoneNumber): ParcelablePhoneNumber {
            return ParcelablePhoneNumber(phoneNumber.id,
                phoneNumber.phoneNumber,
                phoneNumber.state,
                phoneNumber.version,
                phoneNumber.created,
                phoneNumber.updated)
        }
    }
    fun toPhoneNumber(): PhoneNumber {
        return PhoneNumber(id, phoneNumber, state, version, created, updated)
    }
}


class SudoDetailActivity : AppCompatActivity() {
    private lateinit var sudo: Sudo
    private val numberList: ArrayList<PhoneNumber> = ArrayList()
    private lateinit var adapter: ProvisionedPhoneNumberAdapter
    private lateinit var app: App

    override fun onCreate(
        savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sudo_detail)
        app = (application as App)
        this.sudo = intent.getSerializableExtra("sudo") as Sudo
        title = sudo.label
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        adapter = ProvisionedPhoneNumberAdapter(numberList) { number ->
            val intent = Intent(this, ConversationListActivity::class.java)
            intent.putExtra("number", ParcelablePhoneNumber.fromPhoneNumber(number))
            startActivity(intent)
        }

        phone_numbers_recyclerView.adapter = adapter
        phone_numbers_recyclerView.layoutManager = LinearLayoutManager(this)
        listPhoneNumbers()

        button_provisionNumber.setOnClickListener {
            val intent = Intent(this, ProvisionNumberActivity::class.java)
            intent.putExtra("sudo", sudo)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        listPhoneNumbers()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun listPhoneNumbers() {
        try {
            app.sudoTelephonyClient.listPhoneNumbers(sudo.id, null, null) { result ->
                when (result) {
                    is Result.Success -> {
                        numberList.clear()
                        for (number: PhoneNumber in result.value.items) {
                            numberList.add(number)
                        }
                        adapter.notifyDataSetChanged()
                    }
                    is Result.Error -> {
                        runOnUiThread {
                            AlertDialog.Builder(this)
                                .setTitle("Failed to list phone numbers")
                                .setMessage("${result.throwable}")
                                .setPositiveButton("Try Again") { _, _ -> listPhoneNumbers() }
                                .setNegativeButton("Cancel") { _, _ -> }
                                .show()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            runOnUiThread {
                AlertDialog.Builder(this)
                    .setTitle("Failed to list phone numbers")
                    .setMessage("$e")
                    .setPositiveButton("Try Again") { _, _ -> listPhoneNumbers() }
                    .setNegativeButton("Cancel") { _, _ -> }
                    .show()
            }
        }
    }
}
