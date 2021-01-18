/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.adtrackerblockerexample.explore

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.sudoplatform.adtrackerblockerexample.App
import com.sudoplatform.adtrackerblockerexample.R
import com.sudoplatform.adtrackerblockerexample.showAlertDialog
import com.sudoplatform.sudoadtrackerblocker.SudoAdTrackerBlockerClient
import com.sudoplatform.sudoadtrackerblocker.types.Ruleset
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_explore.*
import kotlinx.android.synthetic.main.fragment_explore.view.*
import kotlinx.android.synthetic.main.layout_exception_url_dialog.*
import kotlinx.android.synthetic.main.layout_exception_url_dialog.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This [ExploreFragment] allows the user to explore the working of the blocking
 * rulesets and exceptions. It presents:
 *  - entry text view for the URL to be checked
 *  - spinner that offers suggested URLs to check
 *  - switches to enable or disable the advertising, privacy and social media rulesets
 *  - button to start the checking of the URL
 *  - text view for the results of the check
 *
 * - Links From:
 *  - [RulesetsFragment]: From the menu on the [RulesetsFragment]
 */
class ExploreFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** The [Application] that holds references to the APIs this fragment needs */
    private lateinit var app: App

    /** Keep track of which Rulesets are active so reduce the amount of reloading */
    private val activeRulesets = mutableSetOf<Ruleset.Type>()

    /** A flag to cause a ruleset update once before rulesets are activated */
    private var haveRulesetsBeenUpdated = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_explore, container, false)

        app = requireActivity().application as App

        val toolbar = (view.toolbar as Toolbar)
        toolbar.title = getString(R.string.explore)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkButton.setOnClickListener {
            checkButtonClicked()
        }

        // Setup the suggestions for the URL to check
        val suggestions = resources.getStringArray(R.array.url_suggestions)
        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, suggestions)
        checkedUrlSpinner.setAdapter(adapter)
        checkedUrlSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                checkedUrlText.setText(suggestions[position])
                resultText.text = ""
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Don't care
            }
        }

        launch {
            val activeRulesets = withContext(Dispatchers.IO) {
                app.adTrackerBlockerClient.getActiveRulesets()
            }
            rulesetAdsSwitch.isChecked = Ruleset.Type.AD_BLOCKING in activeRulesets
            rulesetPrivacySwitch.isChecked = Ruleset.Type.PRIVACY in activeRulesets
            rulesetSocialSwitch.isChecked = Ruleset.Type.SOCIAL in activeRulesets
            val exceptions = withContext(Dispatchers.IO) {
                app.adTrackerBlockerClient.getExceptions()
            }
            exceptionsLabel.text = getString(R.string.explore_welcome, exceptions.size)
        }
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        super.onDestroy()
    }

    private fun checkButtonClicked() {

        resultText.setTextColor(resources.getColor(android.R.color.black, null))
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

        val sourceUrl = sourceUrlText.text.toString().trim()

        launch {
            if (!haveRulesetsBeenUpdated) {
                showLoading(R.string.updating_rulesets)
                updateRulesets()
                haveRulesetsBeenUpdated = true
            }

            showLoading(R.string.activating_rulesets)
            setActiveRulesets()

            resultText.setText(R.string.checking_url_start)
            if (checkIsUrlBlocked(checkedUrl, sourceUrl)) {
                resultText.setText(R.string.url_blocked)
                resultText.setTextColor(resources.getColor(R.color.colorRed, null))
            } else {
                resultText.setText(R.string.url_not_blocked)
                resultText.setTextColor(resources.getColor(R.color.colorGreen, null))
            }
            hideLoading()
        }
    }

    /**
     * Perform an update of the rulesets to ensure the latest are available.
     */
    private suspend fun updateRulesets() {
        withContext(Dispatchers.IO) {
            app.adTrackerBlockerClient.updateRulesets()
        }
    }

    /**
     * Examine the switches that enabled and enable the corresponding rulesets
     * and activate the ones the user has selected.
     */
    private suspend fun setActiveRulesets() {
        val selectedRulesets = mutableSetOf<Ruleset.Type>()
        if (rulesetAdsSwitch.isChecked) {
            selectedRulesets.add(Ruleset.Type.AD_BLOCKING)
        }
        if (rulesetPrivacySwitch.isChecked) {
            selectedRulesets.add(Ruleset.Type.PRIVACY)
        }
        if (rulesetSocialSwitch.isChecked) {
            selectedRulesets.add(Ruleset.Type.SOCIAL)
        }
        if (selectedRulesets != activeRulesets) {
            // There has been a change in the user's selected rulesets
            activeRulesets.clear()
            activeRulesets.addAll(selectedRulesets)
            withContext(Dispatchers.IO) {
                app.adTrackerBlockerClient.setActiveRulesets(activeRulesets.toTypedArray())
            }
        }
    }

    private suspend fun checkIsUrlBlocked(checkedUrl: String, sourceUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            app.adTrackerBlockerClient.checkUrl(
                url = ensureHttpPrefix(checkedUrl),
                sourceUrl = ensureHttpPrefix(sourceUrl)
            ) == SudoAdTrackerBlockerClient.CheckUrlResult.BLOCKED
        }
    }

    private fun ensureHttpPrefix(s: String): String {
        val trimmed = s.trim()
        var uri = Uri.parse(trimmed)
        if (uri.scheme == null) {
            uri = Uri.parse("https://$trimmed")
        }
        return uri.toString()
    }

    /**
     * Sets buttons and switches to enabled/disabled.
     *
     * @param isEnabled If true, buttons and switches will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        checkButton?.isEnabled = isEnabled
        checkedUrlText?.isEnabled = isEnabled
        checkedUrlSpinner?.isEnabled = isEnabled
        sourceUrlText?.isEnabled = isEnabled
        rulesetAdsSwitch?.isEnabled = isEnabled
        rulesetPrivacySwitch?.isEnabled = isEnabled
        rulesetSocialSwitch?.isEnabled = isEnabled
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
