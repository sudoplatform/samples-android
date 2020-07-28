package com.sudoplatform.telephonyexample

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils
import com.google.android.material.tabs.TabLayout
import com.sudoplatform.sudotelephony.*
import kotlinx.android.synthetic.main.activity_phone_number_actions.*

class PhoneNumberActionsActivity : AppCompatActivity() {
    private lateinit var app: App
    private lateinit var number: PhoneNumber

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_number_actions)

        app = (application as App)
        number = intent.getParcelableExtra("number") as PhoneNumber
        title = getString(R.string.title_phone_number)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        textViewYourNumber.text = formatAsUSNumber(number.phoneNumber)

        val adapter = PhoneNumberActionsFragmentAdapter(app, number, tabLayout.tabCount, supportFragmentManager)
        viewPager.adapter = adapter
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) { }
            override fun onPageScrolled( position: Int, positionOffset: Float, positionOffsetPixels: Int ) { }
            override fun onPageSelected(position: Int) {
                if (tabLayout.selectedTabPosition != position) {
                    val tab = tabLayout.getTabAt(position)
                    tab?.select()
                }
            }
        })

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) {}
            override fun onTabUnselected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> app.sudoTelephonyClient.unsubscribeFromPhoneMessages("messagesSubscriberId")
                    1 -> app.sudoTelephonyClient.calling.unsubscribeFromCallRecords("callRecordSubscriberId")
                    2 -> app.sudoTelephonyClient.calling.unsubscribeFromVoicemails("voicemailSubscriberId")
                }
            }
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (viewPager.currentItem != tab.position) {
                    viewPager.currentItem = tab.position
                }
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        app.sudoTelephonyClient.calling.unsubscribeFromCallRecords("callRecordSubscriberId")
        app.sudoTelephonyClient.calling.unsubscribeFromVoicemails("voicemailSubscriberId")
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.nav_menu_with_delete_button, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            return when(item.itemId) {
                R.id.delete -> {
                    AlertDialog.Builder(this)
                        .setTitle("Delete Number")
                        .setMessage("Are you sure you want to delete this number? You will lose access to it and all associated messages.")
                        .setPositiveButton("Delete") { _, _ ->  deleteNumber() }
                        .setNegativeButton("Cancel") { _, _ -> }
                        .show()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun deleteNumber() {
        showLoading("Deleting Number...")
        app.sudoTelephonyClient.deletePhoneNumber(number.phoneNumber) { result ->
            hideLoading()
            runOnUiThread {
                when (result) {
                    is Result.Success -> {
                        AlertDialog.Builder(this)
                            .setTitle("Deleted Number")
                            .setPositiveButton("OK") { _, _ -> finish() }
                            .show()
                    }

                    is Result.Error -> {
                        AlertDialog.Builder(this)
                            .setTitle("Failed to delete number")
                            .setMessage(result.throwable.toString())
                            .setPositiveButton("Try Again") { _, _ -> deleteNumber() }
                            .setNegativeButton("Cancel") { _, _ -> }
                            .show()
                    }
                }
            }
        }
    }

    private fun showLoading(text: String? = "") = ThreadUtils.runOnUiThread {
        // Brings the loading indicator for delete number out from behind the ViewPager
        progressBar.translationZ = 1F
        progressText.translationZ = 1F

        progressText.text = text
        progressBar.visibility = View.VISIBLE
        progressText.visibility = View.VISIBLE
    }

    private fun hideLoading() = ThreadUtils.runOnUiThread {
        progressBar.visibility = View.GONE
        progressText.visibility = View.GONE

        // Send the loading indicator back behind the ViewPager
        progressBar.translationZ = 0F
        progressText.translationZ = 0F
    }
}
