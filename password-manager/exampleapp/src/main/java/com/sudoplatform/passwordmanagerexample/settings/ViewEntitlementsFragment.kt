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
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.passwordmanagerexample.App
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.showAlertDialog
import com.sudoplatform.passwordmanagerexample.sudos.SudosFragment
import com.sudoplatform.sudopasswordmanager.entitlements.EntitlementState
import com.sudoplatform.sudoprofiles.ListOption
import com.sudoplatform.sudoprofiles.exceptions.SudoProfileException
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_sudos.view.*
import kotlinx.android.synthetic.main.fragment_view_entitlements.*
import kotlinx.android.synthetic.main.fragment_view_entitlements.view.*
import kotlinx.android.synthetic.main.fragment_view_entitlements.view.toolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This [ViewEntitlementsFragment] lists the how many resources the user is entitled to use
 * and how many have currently been consumed.
 *
 * - Links From:
 *  - [SettingsFragment]: If a user taps on the "View Entitlements" button they will be shown this view
 */
class ViewEntitlementsFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** A reference to the [RecyclerView.Adapter] handling [EntitlementState] data. */
    private lateinit var adapter: ViewEntitlementsAdapter

    /** A mutable list of [EntitlementState]s. */
    private val entitlementsList = mutableListOf<EntitlementState>()

    /** The [Application] that holds references to the APIs this fragment needs */
    private lateinit var app: App

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_view_entitlements, container, false)
        app = requireActivity().application as App
        val toolbar = (view.toolbar as Toolbar)
        toolbar.title = getString(R.string.entitlements_title)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView(view)
    }

    override fun onResume() {
        super.onResume()
        listEntitlements()
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        super.onDestroy()
    }

    private fun listEntitlements() {
        launch {
            try {
                showLoading()
                val (sudos, entitlements) = withContext(Dispatchers.IO) {
                    Pair(
                        app.sudoProfilesClient.listSudos(ListOption.REMOTE_ONLY).toMutableList(),
                        app.sudoPasswordManager.getEntitlementState()
                    )
                }
                // Sort the Sudos so they appear in the same order as they do on the SudosFragment
                sudos.sortWith(SudosFragment.sudosComparator)

                // Order the entitlements so they match the order of Sudos
                entitlementsList.clear()
                for (sudo in sudos) {
                    val entitlementForThisSudo = entitlements.find { it.sudoId == sudo.id }
                        ?: continue
                    entitlementsList.add(entitlementForThisSudo)
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
    private fun configureRecyclerView(view: View) {
        adapter = ViewEntitlementsAdapter(entitlementsList)
        view.entitlementsRecyclerView.adapter = adapter
        view.entitlementsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        progressText.visibility = View.VISIBLE
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        progressBar?.visibility = View.GONE
        progressText?.visibility = View.GONE
    }
}
