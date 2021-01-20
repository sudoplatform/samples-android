/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sitereputationexample.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.sudoplatform.sitereputationexample.App
import com.sudoplatform.sitereputationexample.R
import com.sudoplatform.sitereputationexample.createLoadingAlertDialog
import com.sudoplatform.sitereputationexample.showAlertDialog
import com.sudoplatform.sudouser.SudoUserClient
import com.sudoplatform.sudouser.exceptions.RegisterException
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_settings.clearStorageButton
import kotlinx.android.synthetic.main.fragment_settings.view.clearStorageButton
import kotlinx.android.synthetic.main.fragment_settings.view.signOutButton
import kotlinx.android.synthetic.main.fragment_settings.view.toolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** The [Application] that holds references to the APIs this fragment needs */
    private lateinit var app: App

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        app = requireActivity().application as App

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val toolbar = (view.toolbar as Toolbar)
        toolbar.title = getString(R.string.settings)

        return view
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        view.signOutButton.setOnClickListener {
            val prefs = requireContext().getSharedPreferences(App.SIGN_IN_PREFERENCES, Context.MODE_PRIVATE)
            val usedFSSO = prefs.getBoolean(App.FSSO_USED_PREFERENCE, false)
            if (usedFSSO == true) {
                launch {
                    withContext(Dispatchers.IO) {
                        app.siteReputationClient.clearStorage()
                    }
                    app.doFSSOSSignout()
                }
            } else {
                deregister()
            }
        }

        view.clearStorageButton.setOnClickListener {
            showAlertDialog(
                titleResId = R.string.clear_storage_title,
                messageResId = R.string.clear_storage_confirmation,
                positiveButtonResId = R.string.clear_storage,
                onPositive = { clearStorage() },
                negativeButtonResId = android.R.string.cancel
            )
        }
    }

    /** Perform clearStorage of the [SiteReputationClient] */
    private fun clearStorage() {
        launch {
            try {
                showLoading(R.string.clearing_storage)

                withContext(Dispatchers.IO) {
                    app.siteReputationClient.clearStorage()
                }

                hideLoading()
                Toast.makeText(
                    requireContext(),
                    getString(R.string.clear_storage_success),
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: RegisterException) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.clear_storage_failure, e.localizedMessage),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /** Perform de-registration from the [SudoUserClient] and clear all local data. */
    private fun deregister() {
        launch {
            try {
                showLoading(R.string.deregistering)
                withContext(Dispatchers.IO) {
                    app.sudoUserClient.deregister()
                    app.siteReputationClient.clearStorage()
                }
                hideLoading()
                navController.navigate(R.id.action_settingsFragment_to_registerFragment)
            } catch (error: Exception) {
                app.logger.error("Failed to deregister: $error")
                Toast.makeText(
                    requireContext(),
                    getString(R.string.deregister_failure, error.localizedMessage),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Sets main menu buttons and toolbar items to enabled/disabled.
     *
     * @param isEnabled If true, buttons and toolbar items will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        clearStorageButton.isEnabled = isEnabled
    }

    /** Displays the loading [AlertDialog] indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int) {
        loading = createLoadingAlertDialog(textResId)
        loading?.show()
        setItemsEnabled(false)
    }

    /** Dismisses the loading [AlertDialog] indicating that an operation has finished. */
    private fun hideLoading() {
        loading?.dismiss()
        setItemsEnabled(true)
    }
}
