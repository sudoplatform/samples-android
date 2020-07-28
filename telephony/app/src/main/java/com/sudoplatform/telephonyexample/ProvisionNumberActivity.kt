package com.sudoplatform.telephonyexample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudotelephony.Result
import kotlinx.android.synthetic.main.activity_provision_number.*
import java.util.*

class ProvisionNumberActivity : AppCompatActivity() {

    lateinit var app: App

    private val numberList: ArrayList<String> = ArrayList()
    private val countryCodeList: ArrayList<String> = ArrayList()
    private lateinit var phoneNumberAdapter: PhoneNumberAdapter
    private lateinit var countryCodeAdapter: ArrayAdapter<String>

    private var sudo: Sudo? = null
    private var countryCode = ""
    private var toolbarMenu: Menu? = null
    private val MIC_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_provision_number)

        app = (application as App)
        this.sudo = intent.getSerializableExtra("sudo") as Sudo
        title = "Provision Phone Number"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Set up phone number recyclerview with adapter and itemSelectedListener
        phoneNumberAdapter = PhoneNumberAdapter(numberList) { number ->
            // provision the selected number
            AlertDialog.Builder(this)
                .setTitle("Provision")
                .setMessage("Are you sure you want to provision $number?")
                .setPositiveButton("Provision") { _, _ -> provision(number) }
                .setNegativeButton("Cancel") { _, _ -> }
                .show()
        }
        recyclerView_phoneNumbers.adapter = phoneNumberAdapter
        recyclerView_phoneNumbers.layoutManager = LinearLayoutManager(this)

        // set the first spinner item to "Select Country"
        countryCodeList.add("Select Country")
        // set up adapter and onItemSelected for country code spinner
        countryCodeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, countryCodeList)
        val countryCodeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, countryCodeList)
        spinner_countryCodes.adapter = countryCodeAdapter
        spinner_countryCodes.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View, position: Int, id: Long
            ) {
                if (position > 0) {
                    countryCode = countryCodeList[position]
                } else {
                    countryCode = ""
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                parent.setSelection(0)
            }
        }

        getSupportedCountries()

        // monitor area code text change and search for numbers when three digits are entered
        editText_areaCode.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                if (s.count() >= 3) {
                    search()
                } else {
                    numberList.clear()
                    phoneNumberAdapter.notifyDataSetChanged()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            MIC_PERMISSION_REQUEST_CODE
        )
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.nav_menu_provision, menu)
        toolbarMenu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            return when(item.itemId) {
                R.id.info -> {
                    AlertDialog.Builder(this)
                        .setTitle("Phone Numbers")
                        .setMessage("Sudo Platform phone numbers have an associated country code. The ZZ country code is used to simulate phone numbers without incurring costs.")
                        .setPositiveButton("Ok") { _, _ -> }
                        .setNegativeButton("Learn More") { _, _ ->  learnMore() }
                        .show()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun learnMore() {
        val openUrl = Intent(Intent.ACTION_VIEW)
        openUrl.data = Uri.parse("https://docs.sudoplatform.com/guides/telephony/phone-numbers")
        startActivity(openUrl)
    }

    private fun getSupportedCountries() {
        // Populate countryCodeList from getSupportedCountries result
        app.sudoTelephonyClient.getSupportedCountries { result ->
            when (result) {
                is Result.Success -> {
                    runOnUiThread {
                        countryCodeList.clear()
                        countryCodeList.add("Select Country")
                        countryCodeList.addAll(result.value.countries)
                        countryCodeAdapter.notifyDataSetChanged()
                        if (result.value.countries.isEmpty()) {
                            AlertDialog.Builder(this)
                                .setTitle("No supported countries")
                                .setPositiveButton("Try Again") { _, _ -> getSupportedCountries() }
                                .setNegativeButton("Cancel") { _, _ -> }
                                .show()
                        }
                    }
                }
                is Result.Error -> {
                    runOnUiThread {
                        AlertDialog.Builder(this)
                            .setTitle("Failed to get supported countries")
                            .setMessage("${result.throwable}")
                            .setPositiveButton("Try Again") { _, _ -> getSupportedCountries() }
                            .setNegativeButton("Cancel") { _, _ -> }
                            .show()
                    }
                }
                is Result.Absent -> {
                    runOnUiThread {
                        AlertDialog.Builder(this)
                            .setTitle("Failed to get supported countries")
                            .setPositiveButton("Try Again") { _, _ -> getSupportedCountries() }
                            .setNegativeButton("Cancel") { _, _ -> }
                            .show()
                    }
                }
            }
        }
    }

    private fun search() {
        numberList.clear()
        phoneNumberAdapter.notifyDataSetChanged()
        showLoading()
        textView_no_results.visibility = View.GONE
        app.sudoTelephonyClient.searchAvailablePhoneNumbers(countryCode, editText_areaCode.text.toString()) { result ->
            hideLoading()
            when (result) {
                is Result.Success -> {
                    runOnUiThread {
                        numberList.clear()
                        val numbers = result.value.numbers
                        for (number in numbers) {
                            numberList.add(number)
                        }
                        if (numbers.isEmpty()) {
                            textView_no_results.visibility = View.VISIBLE
                        }
                        phoneNumberAdapter.notifyDataSetChanged()
                    }
                }
                is Result.Error -> {
                    Log.e("TelephonyExample", "", result.throwable)
                }
            }
        }
    }

    /**
     * Actually provisions the number
     */

    private fun provision(number: String) {
        sudo?.id?.let { sudoId ->
            showLoading()
            // disable actions while provisioning
            runOnUiThread {
                editText_areaCode.isEnabled = false
                spinner_countryCodes.isEnabled = false
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
                toolbarMenu?.getItem(0)?.isEnabled = false
            }
            try {
                Thread(Runnable {
                    app.sudoTelephonyClient.provisionPhoneNumber(
                        countryCode,
                        number,
                        sudoId
                    ) { result ->
                        hideLoading()
                        runOnUiThread {
                            editText_areaCode.isEnabled = true
                            spinner_countryCodes.isEnabled = true
                            supportActionBar?.setDisplayHomeAsUpEnabled(true)
                            toolbarMenu?.getItem(0)?.isEnabled = true
                        }
                        when (result) {
                            is Result.Success -> {
                                Log.d("TelephonyExample", "provisioned number ${result.value.phoneNumber} successfully")
                                runOnUiThread {
                                    AlertDialog.Builder(this)
                                        .setTitle("Success")
                                        .setMessage("$number was successfully provisioned")
                                        .setPositiveButton("OK") { _, _ -> finish() }
                                        .show()
                                }
                            }
                            is Result.Error -> {
                                Log.d("TelephonyExample", "failed to provision number: ${result.throwable}")
                                runOnUiThread {
                                    AlertDialog.Builder(this)
                                        .setTitle("Provision Failed")
                                        .setMessage("${result.throwable}")
                                        .setPositiveButton("Try Again") { _, _ -> provision(number) }
                                        .setNegativeButton("Cancel") { _, _ -> }
                                        .show()
                                }
                            }
                        }
                    }
                }).start()
            } catch (e: Exception) {
                runOnUiThread {
                    AlertDialog.Builder(this)
                        .setTitle("Provision Failed")
                        .setMessage("$e")
                        .setPositiveButton("Try Again") { _, _ -> provision(number) }
                        .setNegativeButton("Cancel") { _, _ -> }
                        .show()
                }
            }
        } ?: run {
            AlertDialog.Builder(this)
                .setTitle("Provision Failed")
                .setMessage("Missing Sudo ID")
                .setPositiveButton("Try Again") { _, _ -> provision(number) }
                .setNegativeButton("Cancel") { _, _ -> }
                .show()
        }
    }

    private fun showLoading() = runOnUiThread {
        recyclerView_phoneNumbers.isEnabled = false
        progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() = runOnUiThread {
        recyclerView_phoneNumbers.isEnabled = true
        progressBar.visibility = View.GONE
    }
}
