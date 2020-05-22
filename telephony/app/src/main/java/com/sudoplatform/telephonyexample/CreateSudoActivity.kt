package com.sudoplatform.telephonyexample

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.sudoplatform.sudoprofiles.CreateSudoResult
import com.sudoplatform.sudoprofiles.Sudo
import kotlinx.android.synthetic.main.activity_create_sudo.*
import java.util.*


class CreateSudoActivity : AppCompatActivity() {
    private var toolbarMenu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_sudo)
        title = "Create Sudo"

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.nav_menu_create_sudo, menu)
        toolbarMenu = menu
        return true
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        return when(menuItem.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.create -> {
                createSudo()
                true
            }
            else -> super.onOptionsItemSelected(menuItem)
        }
    }

    private fun createSudo() {
        val name = editText.text.toString().trim()
        if (name.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Enter a name for your Sudo")
                .setPositiveButton("Ok") { _, _ -> }
                .show()
            return
        }
        val sudo = Sudo(UUID.randomUUID().toString())
        sudo.label = name
        showLoading()
        (application as App).sudoProfilesClient.createSudo(sudo) { result ->
            when (result) {
                is CreateSudoResult.Success -> {
                    runOnUiThread {
                        AlertDialog.Builder(this)
                            .setTitle("Success!")
                            .setPositiveButton("Ok") { _, _ ->  finish() }
                            .show()
                    }
                }
                is CreateSudoResult.Failure -> {
                    runOnUiThread {
                        AlertDialog.Builder(this)
                            .setTitle("Something went wrong")
                            .setMessage(result.error.toString())
                            .setPositiveButton("Try Again") { _, _ -> createSudo() }
                            .setNegativeButton("Cancel") { _, _ -> }
                            .show()
                    }
                }
            }
            hideLoading()
        }
    }

    private fun showLoading() = runOnUiThread {
        progressBar.visibility = View.VISIBLE
        editText.isEnabled = false
        toolbarMenu?.getItem(0)?.isEnabled = false
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    private fun hideLoading() = runOnUiThread {
        progressBar.visibility = View.GONE
        editText.isEnabled = true
        toolbarMenu?.getItem(0)?.isEnabled = true
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}
