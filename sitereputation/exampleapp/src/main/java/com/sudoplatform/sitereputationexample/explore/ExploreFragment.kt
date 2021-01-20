/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-Licensee-Identifier: Apache-2.0
 */

package com.sudoplatform.sitereputationexample.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.sudoplatform.sitereputationexample.App
import com.sudoplatform.sitereputationexample.R
import com.sudoplatform.sitereputationexample.showAlertDialog
import com.sudoplatform.sudositereputation.SudoSiteReputationException
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_explore.checkButton
import kotlinx.android.synthetic.main.fragment_explore.checkedUrlSpinner
import kotlinx.android.synthetic.main.fragment_explore.checkedUrlText
import kotlinx.android.synthetic.main.fragment_explore.lastUpdatedTextView
import kotlinx.android.synthetic.main.fragment_explore.progressBar
import kotlinx.android.synthetic.main.fragment_explore.progressText
import kotlinx.android.synthetic.main.fragment_explore.resultLabel
import kotlinx.android.synthetic.main.fragment_explore.resultText
import kotlinx.android.synthetic.main.fragment_explore.updateButton
import kotlinx.android.synthetic.main.fragment_explore.view.checkButton
import kotlinx.android.synthetic.main.fragment_explore.view.checkedUrlSpinner
import kotlinx.android.synthetic.main.fragment_explore.view.toolbar
import kotlinx.android.synthetic.main.fragment_explore.view.updateButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This [ExploreFragment] allows the user to explore the working of the Site Reputation rules and
 * checking for malicious websites. It presents:
 *  - entry text view for the URL to be checked
 *  - spinner that offers suggested URLs to check
 *  - button to update the rules list
 *  - text view for the results of the check
 *
 * - Links From:
 *  - [RegisterFragment]: After registering this view is presented
 *
 * - Links To:
 *  - [SettingsFragment]: If a user taps the "Settings" button, the [Settings] will
 *   be presented so the user can sign out or reset data.
 */
class ExploreFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** Toolbar [Menu] displaying title and toolbar items. */
    private lateinit var toolbarMenu: Menu

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** The [Application] that holds references to the APIs this fragment needs */
    lateinit var app: App

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_explore, container, false)
        app = requireActivity().application as App

        val toolbar = (view.toolbar as Toolbar)
        toolbar.title = getString(R.string.explore)
        toolbar.inflateMenu(R.menu.nav_menu_explore_menu)
        toolbar.setOnMenuItemClickListener {
            when (it?.itemId) {
                R.id.settings -> {
                    navController.navigate(R.id.action_exploreFragment_to_settingsFragment)
                }
            }
            true
        }
        toolbarMenu = toolbar.menu

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        view.updateButton.setOnClickListener {
            launch {
                showLoading(R.string.updating)
                withContext(Dispatchers.IO) {
                    app.siteReputationClient.update()
                }
                setUpdatedText()
                hideLoading()
            }
        }

        view.checkButton.setOnClickListener {
            checkButtonClicked()
        }

        // Get latest updated date
        setUpdatedText()

        // Setup the suggestions for the URL to check
        val suggestions = resources.getStringArray(R.array.url_suggestions)
        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, suggestions)
        view.checkedUrlSpinner.setAdapter(adapter)
        view.checkedUrlSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                checkedUrlText.setText(suggestions[position])
                resultText.text = ""
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Don't care
            }
        }
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        super.onDestroy()
    }

    private fun checkButtonClicked() {
        resultText.text = ""

        val checkedUrl = checkedUrlText.text.toString().trim()
        if (checkedUrl.isEmpty()) {
            showAlertDialog(
                titleResId = R.string.explore,
                messageResId = R.string.empty_checked_url,
                positiveButtonResId = android.R.string.ok,
                onPositive = { }
            )
            return
        }

        launch {
            showLoading(R.string.checking)
            try {
                val siteReputation = app.siteReputationClient.getSiteReputation(
                    url = checkedUrl
                )
                if (siteReputation.isMalicious) {
                    resultText.setText(R.string.malicious)
                    resultText.setTextColor(resources.getColor(R.color.colorRed, null))
                } else {
                    resultText.setText(R.string.safe)
                    resultText.setTextColor(resources.getColor(R.color.colorGreen, null))
                }
            } catch (e: SudoSiteReputationException) {
                showAlertDialog(
                    titleResId = R.string.check_failed,
                    message = getString(R.string.check_failed_message, e),
                    negativeButtonResId = android.R.string.ok,
                    onNegative = { }
                )
            }
            hideLoading()
        }
    }

    private fun setUpdatedText() {
        val date = app.siteReputationClient.lastUpdatePerformedAt
        date?.let {
            val format = SimpleDateFormat(getString(R.string.updated_at_format), Locale.getDefault())
            lastUpdatedTextView.setText(getString(R.string.last_updated_at, format.format(it)))
        } ?: run {
            lastUpdatedTextView.setText(R.string.update_required)
        }
    }

    /**
     * Sets buttons and text fields to enabled/disabled.
     *
     * @param isEnabled If true, buttons and switches will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        updateButton?.isEnabled = isEnabled
        checkButton?.isEnabled = isEnabled
        checkedUrlText?.isEnabled = isEnabled
        checkedUrlSpinner?.isEnabled = isEnabled
        resultLabel?.isEnabled = isEnabled
        resultText?.isEnabled = isEnabled
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int = 0) {
        if (textResId != 0) {
            progressText.text = getString(textResId)
        }
        progressBar.visibility = View.VISIBLE
        progressText.visibility = View.VISIBLE
        setItemsEnabled(false)
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        progressBar?.visibility = View.GONE
        progressText?.visibility = View.GONE
        setItemsEnabled(true)
    }
}
