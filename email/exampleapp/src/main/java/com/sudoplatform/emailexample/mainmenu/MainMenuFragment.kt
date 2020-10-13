/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.mainmenu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.sudoplatform.emailexample.App
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.createLoadingAlertDialog
import com.sudoplatform.emailexample.register.RegisterFragment
import com.sudoplatform.emailexample.showAlertDialog
import com.sudoplatform.emailexample.sudos.SudosFragment
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudouser.SudoUserClient
import com.sudoplatform.sudouser.exceptions.RegisterException
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_main_menu.*
import kotlinx.android.synthetic.main.fragment_main_menu.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This [MainMenuFragment] presents a menu screen so that the user can navigate through each of the
 * menu items.
 *
 * Links From:
 *  - [RegisterFragment]: A user successfully registers or signs in to the app.
 *
 * Links To:
 *  - [SudosFragment]: If a user taps the "Sudos" button, the [SudosFragment] will be presented so
 *   the user can view or choose to create [Sudo]s.
 */
class MainMenuFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** Toolbar [Menu] displaying title and toolbar items. */
    private lateinit var toolbarMenu: Menu

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private lateinit var loading: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main_menu, container, false)
        val toolbar = (view.toolbar as Toolbar)
        toolbar.title = getString(R.string.app_name)

        toolbar.inflateMenu(R.menu.nav_menu_main_menu)
        toolbar.setOnMenuItemClickListener {
            when (it?.itemId) {
                R.id.deregister -> {
                    showAlertDialog(
                        titleResId = R.string.deregister,
                        messageResId = R.string.deregister_confirmation,
                        positiveButtonResId = R.string.deregister,
                        onPositive = { deregister() },
                        negativeButtonResId = android.R.string.cancel
                    )
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

        view.sudosButton.setOnClickListener {
            navController.navigate(R.id.action_mainMenuFragment_to_sudosFragment)
        }
    }

    /** Perform de-registration from the [SudoUserClient] and clear all local data. */
    private fun deregister() {
        launch {
            try {
                showLoading(R.string.deregistering)
                val app = requireActivity().application as App
                withContext(Dispatchers.IO) {
                    app.sudoUserClient.deregister()
                }
                hideLoading()
                navController.navigate(R.id.action_mainMenuFragment_to_registerFragment)
            } catch (e: RegisterException) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.deregister_failure, e.localizedMessage),
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
        toolbarMenu.getItem(0)?.isEnabled = isEnabled
        sudosButton.isEnabled = isEnabled
    }

    /** Displays the loading [AlertDialog] indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int) {
        loading = createLoadingAlertDialog(textResId)
        loading.show()
        setItemsEnabled(false)
    }

    /** Dismisses the loading [AlertDialog] indicating that an operation has finished. */
    private fun hideLoading() {
        loading.dismiss()
        setItemsEnabled(true)
    }
}
