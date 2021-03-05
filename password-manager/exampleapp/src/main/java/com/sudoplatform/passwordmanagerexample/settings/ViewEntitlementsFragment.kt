/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.passwordmanagerexample.App
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.databinding.FragmentViewEntitlementsBinding
import com.sudoplatform.passwordmanagerexample.showAlertDialog
import com.sudoplatform.passwordmanagerexample.sudos.SudosFragment
import com.sudoplatform.passwordmanagerexample.util.ObjectDelegate
import com.sudoplatform.sudopasswordmanager.entitlements.EntitlementState
import com.sudoplatform.sudoprofiles.ListOption
import com.sudoplatform.sudoprofiles.exceptions.SudoProfileException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * This [ViewEntitlementsFragment] lists the how many resources the user is entitled to use
 * and how many have currently been consumed.
 *
 * - Links From:
 *  - [SettingsFragment]: If a user taps on the "View Entitlements" button they will be shown this view
 */
class ViewEntitlementsFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** View binding to the views defined in the layout */
    private val bindingDelegate = ObjectDelegate<FragmentViewEntitlementsBinding>()
    private val binding by bindingDelegate

    /** A reference to the [RecyclerView.Adapter] handling [EntitlementState] data. */
    private lateinit var adapter: ViewEntitlementsAdapter

    /** A mutable list of [EntitlementState]s. */
    private val entitlementsList = mutableListOf<ViewEntitlementsHolder>()

    /** The [Application] that holds references to the APIs this fragment needs */
    private lateinit var app: App

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bindingDelegate.attach(FragmentViewEntitlementsBinding.inflate(inflater, container, false))
        app = requireActivity().application as App
        binding.toolbar.root.title = getString(R.string.entitlements_title)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        listEntitlements()
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    private fun listEntitlements() {
        launch {
            try {
                showLoading()
                val (sudos, entitlement, entitlementState) = withContext(Dispatchers.IO) {
                    Triple(
                        app.sudoProfilesClient.listSudos(ListOption.REMOTE_ONLY).toMutableList(),
                        app.sudoPasswordManager.getEntitlement(),
                        app.sudoPasswordManager.getEntitlementState()
                    )
                }

                entitlementsList.clear()
                if (sudos.isNotEmpty()) {
                    // Sort the Sudos so they appear in the same order as they do on the SudosFragment
                    sudos.sortWith(SudosFragment.sudosComparator)

                    // Order the entitlements so they match the order of Sudos
                    for (sudo in sudos) {
                        val entitlementForThisSudo = entitlementState.find { it.sudoId == sudo.id }
                            ?: continue
                        entitlementsList.add(ViewEntitlementsHolder.WithSudos(entitlementForThisSudo))
                    }
                } else {
                    entitlement.forEach {
                        entitlementsList.add(ViewEntitlementsHolder.NoSudos(it))
                    }
                }
                adapter.notifyDataSetChanged()
            } catch (e: SudoProfileException) {
                showAlertDialog(
                    titleResId = R.string.list_sudos_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { listEntitlements() },
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideLoading()
        }
    }

    /**
     * Configures the [RecyclerView] used to display the listed [EntitlementState] items.
     */
    private fun configureRecyclerView() {
        adapter = ViewEntitlementsAdapter(entitlementsList)
        binding.entitlementsRecyclerView.adapter = adapter
        binding.entitlementsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.progressText.visibility = View.VISIBLE
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        if (bindingDelegate.isAttached()) {
            binding.progressBar.visibility = View.GONE
            binding.progressText.visibility = View.GONE
        }
    }
}
