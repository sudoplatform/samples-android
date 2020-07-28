package com.sudoplatform.telephonyexample

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.sudoplatform.sudoprofiles.ListOption
import com.sudoplatform.sudoprofiles.ListSudosResult
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudouser.ApiResult
import com.sudoplatform.sudotelephony.Result
import kotlinx.android.synthetic.main.activity_sudos.*
import java.util.*

/**
 * Main activity for the TelephonySDK demo app.
 */
class SudosActivity : AppCompatActivity() {

    private val sudoList: ArrayList<Sudo> = ArrayList()
    private lateinit var adapter: SudoAdapter
    private var toolbarMenu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sudos)
        setTitle(R.string.activity_sudos)

        button_createSudo.setOnClickListener {
            val intent = Intent(this, CreateSudoActivity::class.java)
            startActivity(intent)
        }

        adapter = SudoAdapter(sudoList) { sudo ->
            val intent = Intent(this, SudoDetailActivity::class.java)
            intent.putExtra("sudo", sudo)
            startActivity(intent)
        }

        sudo_recyclerView.adapter = adapter
        sudo_recyclerView.layoutManager = LinearLayoutManager(this)
        listSudos(ListOption.CACHE_ONLY)

        // now that the user is signed in, register for incoming calls if google play services is available
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
            FirebaseInstanceId.getInstance().instanceId
                .addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        // failed to get instance id
                        return@OnCompleteListener
                    }

                    val fcmToken = task.result!!.token
                    (application as App).sudoTelephonyClient.calling.registerForIncomingCalls(fcmToken) { result ->
                        when (result) {
                            is Result.Success -> {
                                // successfully registered
                            }
                            is Result.Error -> {
                                runOnUiThread {
                                    Toast.makeText(this, "Unable to register for incoming calls: ${result.throwable}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                })
        } else {
            GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)
        }
    }

    override fun onResume() {
        super.onResume()
        listSudos(ListOption.REMOTE_ONLY)
    }

    override fun onBackPressed() { moveTaskToBack(true) }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.nav_menu_sudos, menu)
        toolbarMenu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            return when(item.itemId) {
                R.id.deregister -> {
                    AlertDialog.Builder(this)
                        .setTitle("Deregister")
                        .setMessage("Are you sure you want to deregister? You will no longer have access to these Sudos or Phone Numbers")
                        .setPositiveButton("Deregister") { _, _ ->  deregister() }
                        .setNegativeButton("Cancel") { _, _ -> }
                        .show()
                    true
                }
                R.id.info -> {
                    AlertDialog.Builder(this)
                        .setTitle("What is a Sudo?")
                        .setMessage("Phone numbers must belong to a Sudo. A Sudo is a digital identity created and owned by a real person")
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

    private fun deregister() {
        showLoading()
        (application as App).sudoUserClient.deregister { result ->
            hideLoading()
            when (result) {
                is ApiResult.Success -> {
                    finish()
                }
                is ApiResult.Failure -> {
                    runOnUiThread {
                        Toast.makeText(this, "Unable to deregister: ${result.error}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun learnMore() {
        val openUrl = Intent(android.content.Intent.ACTION_VIEW)
        openUrl.data = Uri.parse("https://docs.sudoplatform.com/concepts/sudo-digital-identities")
        startActivity(openUrl)
    }

    private fun listSudos(listOption: ListOption) {
        showLoading("Loading Sudos")
        (application as App).sudoProfilesClient.listSudos(listOption) { result ->
            hideLoading()
            when (result) {
                is ListSudosResult.Success -> {
                    runOnUiThread {
                        sudoList.clear()
                        for (sudo in result.sudos) {
                            sudoList.add(sudo)
                        }
                        adapter.notifyDataSetChanged()
                    }
                }
                is ListSudosResult.Failure -> {
                    runOnUiThread {
                        AlertDialog.Builder(this)
                            .setTitle("Failed to list sudos")
                            .setMessage(result.error.toString())
                            .setPositiveButton("Try Again") { _, _ -> listSudos(ListOption.REMOTE_ONLY) }
                            .setNegativeButton("Cancel") { _, _ -> }
                            .show()
                    }
                }
            }
        }

    }

    private fun showLoading(text: String? = "") = runOnUiThread {
        progressText.text = text
        progressBar.visibility = View.VISIBLE
        progressText.visibility = View.VISIBLE
        toolbarMenu?.getItem(0)?.isEnabled = false
        toolbarMenu?.getItem(1)?.isEnabled = false
        button_createSudo.isEnabled = false
        sudo_recyclerView.isEnabled = false
    }

    private fun hideLoading() = runOnUiThread {
        progressBar.visibility = View.GONE
        progressText.visibility = View.GONE
        toolbarMenu?.getItem(0)?.isEnabled = true
        toolbarMenu?.getItem(1)?.isEnabled = true
        button_createSudo.isEnabled = true
        sudo_recyclerView.isEnabled = true
    }
}



