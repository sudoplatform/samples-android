package com.sudoplatform.telephonyexample

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudotelephony.PhoneNumber
import com.sudoplatform.sudotelephony.Result
import kotlinx.android.synthetic.main.activity_sudo_detail.*
import java.util.*

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
            intent.putExtra("number", number)
            startActivity(intent)
        }

        phone_numbers_recyclerView.adapter = adapter
        phone_numbers_recyclerView.layoutManager = LinearLayoutManager(this)

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
        fun fetchPageOfPhoneNumbers(listToken: String?) {
            try {
                app.sudoTelephonyClient.listPhoneNumbers(sudo.id, null, listToken) { result ->
                    when (result) {
                        is Result.Success -> {
                            if (listToken == null) {
                                numberList.clear()
                            }
                            for (number: PhoneNumber in result.value.items) {
                                numberList.add(number)
                            }
                            if (result.value.nextToken != null) {
                                fetchPageOfPhoneNumbers(result.value.nextToken)
                            } else {
                                // no more pages to load
                                adapter.notifyDataSetChanged()
                            }
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
        fetchPageOfPhoneNumbers(null)
    }
}
