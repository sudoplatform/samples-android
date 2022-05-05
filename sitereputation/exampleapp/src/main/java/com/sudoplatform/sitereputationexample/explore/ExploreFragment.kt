/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-Licensee-Identifier: Apache-2.0
 */

package com.sudoplatform.sitereputationexample.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.sudoplatform.sitereputationexample.App
import com.sudoplatform.sitereputationexample.R
import com.sudoplatform.sitereputationexample.databinding.FragmentExploreBinding
import com.sudoplatform.sitereputationexample.showAlertDialog
import com.sudoplatform.sitereputationexample.util.ObjectDelegate
import com.sudoplatform.sudositereputation.SudoSiteReputationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.CoroutineContext

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

    /** View binding to the views defined in the layout */
    private val bindingDelegate = ObjectDelegate<FragmentExploreBinding>()
    private val binding by bindingDelegate

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [Application] that holds references to the APIs this fragment needs */
    lateinit var app: App

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bindingDelegate.attach(FragmentExploreBinding.inflate(inflater, container, false))

        app = requireActivity().application as App

        with(binding.toolbar.root) {
            title = getString(R.string.explore)
            inflateMenu(R.menu.nav_menu_explore_menu)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.settings -> {
                        navController.navigate(R.id.action_exploreFragment_to_settingsFragment)
                    }
                }
                true
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        binding.updateButton.setOnClickListener {
            launch {
                showLoading(R.string.updating)
                withContext(Dispatchers.IO) {
                    app.siteReputationClient.update()
                }
                setUpdatedText()
                hideLoading()
            }
        }

        binding.checkButton.setOnClickListener {
            checkButtonClicked()
        }

        // Get latest updated date
        setUpdatedText()

        // Setup the suggestions for the URL to check
        val suggestions = resources.getStringArray(R.array.url_suggestions)
        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, suggestions)
        binding.checkedUrlSpinner.setAdapter(adapter)
        binding.checkedUrlSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                binding.checkedUrlText.setText(suggestions[position])
                binding.resultText.text = ""
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Don't care
            }
        }
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    private fun checkButtonClicked() {
        binding.resultText.text = ""

        val checkedUrl = binding.checkedUrlText.text.toString().trim()
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
                    binding.resultText.setText(R.string.malicious)
                    binding.resultText.setTextColor(resources.getColor(R.color.colorRed, null))
                } else {
                    binding.resultText.setText(R.string.safe)
                    binding.resultText.setTextColor(resources.getColor(R.color.colorGreen, null))
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
            binding.lastUpdatedTextView.setText(getString(R.string.last_updated_at, format.format(it)))
        } ?: run {
            binding.lastUpdatedTextView.setText(R.string.update_required)
        }
    }

    /**
     * Sets buttons and text fields to enabled/disabled.
     *
     * @param isEnabled If true, buttons and switches will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        with(binding) {
            updateButton.isEnabled = isEnabled
            checkButton.isEnabled = isEnabled
            checkedUrlText.isEnabled = isEnabled
            checkedUrlSpinner.isEnabled = isEnabled
            resultLabel.isEnabled = isEnabled
            resultText.isEnabled = isEnabled
        }
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int = 0) {
        if (textResId != 0) {
            binding.progressText.text = getString(textResId)
        }
        binding.progressBar.visibility = View.VISIBLE
        binding.progressText.visibility = View.VISIBLE
        setItemsEnabled(false)
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.progressText.visibility = View.GONE
        setItemsEnabled(true)
    }
}
