/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.adtrackerblockerexample.rulesets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.adtrackerblockerexample.App
import com.sudoplatform.adtrackerblockerexample.R
import com.sudoplatform.adtrackerblockerexample.databinding.FragmentRulesetsBinding
import com.sudoplatform.adtrackerblockerexample.showAlertDialog
import com.sudoplatform.adtrackerblockerexample.sudos.RulesetAdapter
import com.sudoplatform.adtrackerblockerexample.util.ObjectDelegate
import com.sudoplatform.sudoadtrackerblocker.SudoAdTrackerBlockerException
import com.sudoplatform.sudoadtrackerblocker.types.Ruleset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * This [RulesetsFragment] presents a list of [Ruleset]s.
 *
 * - Links From:
 *  - [RegisterFragment]: After registering this view is presented with the list of available [Ruleset]s.
 *
 * - Links To:
 *  - [SettingsFragment]: If a user taps the "Settings" button, the [Settings] will
 *   be presented so the user can sign out or reset data.
 */
class RulesetsFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** View binding to the views defined in the layout */
    private val bindingDelegate = ObjectDelegate<FragmentRulesetsBinding>()
    private val binding by bindingDelegate

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** A reference to the [RecyclerView.Adapter] handling [Ruleset] data. */
    private lateinit var adapter: RulesetAdapter

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** A mutable list of [Ruleset]s. */
    private val rulesetList = mutableListOf<Ruleset>()

    /** The [Application] that holds references to the APIs this fragment needs */
    lateinit var app: App

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            requireActivity().finish()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bindingDelegate.attach(FragmentRulesetsBinding.inflate(inflater, container, false))
        app = requireActivity().application as App

        with(binding.toolbar.root) {
            title = getString(R.string.rulesets)
            inflateMenu(R.menu.nav_menu_rulesets_menu)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.settings -> {
                        navController.navigate(R.id.action_rulesetsFragment_to_settingsFragment)
                    }
                    R.id.exceptionsList -> {
                        navController.navigate(R.id.action_rulesetsFragment_to_exceptionsListFragment)
                    }
                    R.id.explore -> {
                        navController.navigate(R.id.action_rulesetsFragment_to_exploreFragment)
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
        configureRecyclerView()
        listRulesets()
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    private fun listRulesets() {
        launch {
            try {
                showLoading()
                val rulesets = withContext(Dispatchers.IO) {
                    app.adTrackerBlockerClient.listRulesets()
                }
                rulesetList.clear()
                for (ruleset in rulesets) {
                    rulesetList.add(ruleset)
                }
                adapter.notifyDataSetChanged()
                hideLoading()
            } catch (e: SudoAdTrackerBlockerException) {
                showAlertDialog(
                    titleResId = R.string.list_rulesets_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { listRulesets() },
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideLoading()
        }
    }
    /**
     * Configures the [RecyclerView] used to display the listed [Ruleset] items.
     */
    private fun configureRecyclerView() {
        adapter = RulesetAdapter(rulesetList)
        binding.rulesetRecyclerView.adapter = adapter
        binding.rulesetRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    /**
     * Sets buttons and recycler view to enabled/disabled.
     *
     * @param isEnabled If true, buttons and recycler view will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        binding.rulesetRecyclerView.isEnabled = isEnabled
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int = 0) {
        if (textResId != 0) {
            binding.progressText.text = getString(textResId)
        }
        binding.progressBar.visibility = View.VISIBLE
        binding.progressText.visibility = View.VISIBLE
        binding.rulesetRecyclerView.visibility = View.GONE
        setItemsEnabled(false)
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.progressText.visibility = View.GONE
        binding.rulesetRecyclerView.visibility = View.VISIBLE
        setItemsEnabled(true)
    }
}
