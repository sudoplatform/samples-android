/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
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
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.sudoplatform.emailexample.App
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.createLoadingAlertDialog
import com.sudoplatform.emailexample.databinding.FragmentMainMenuBinding
import com.sudoplatform.emailexample.register.RegisterFragment
import com.sudoplatform.emailexample.showAlertDialog
import com.sudoplatform.emailexample.sudos.SudosFragment
import com.sudoplatform.emailexample.util.ObjectDelegate
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudouser.SudoUserClient
import com.sudoplatform.sudouser.exceptions.SudoUserException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

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
class MainMenuFragment :
    Fragment(),
    CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentMainMenuBinding>()
    private val binding by bindingDelegate

    /** Toolbar [Menu] displaying title and toolbar items. */
    private lateinit var toolbarMenu: Menu

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingDelegate.attach(FragmentMainMenuBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.app_name)
            inflateMenu(R.menu.nav_menu_main_menu)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.deregister -> {
                        showAlertDialog(
                            titleResId = R.string.deregister,
                            messageResId = R.string.deregister_confirmation,
                            positiveButtonResId = R.string.deregister,
                            onPositive = { deregister() },
                            negativeButtonResId = android.R.string.cancel,
                        )
                    }
                }
                true
            }
            toolbarMenu = menu
        }
        app = requireActivity().application as App
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        binding.sudosButton.setOnClickListener {
            navController.navigate(
                MainMenuFragmentDirections.actionMainMenuFragmentToSudosFragment(),
            )
        }
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    /** Perform de-registration from the [SudoUserClient] and clear all local data. */
    private fun deregister() {
        launch {
            try {
                showLoading(R.string.deregistering)
                withContext(Dispatchers.IO) {
                    app.sudoUserClient.deregister()
                    app.sudoEmailClient.reset()
                    app.sudoProfilesClient.reset()
                    app.sudoUserClient.reset()
                }
                hideLoading()
                navController.navigate(
                    MainMenuFragmentDirections.actionMainMenuFragmentToRegisterFragment(),
                )
            } catch (e: SudoUserException) {
                Toast
                    .makeText(
                        requireContext(),
                        getString(R.string.deregister_failure, e.localizedMessage),
                        Toast.LENGTH_LONG,
                    ).show()
            }
        }
    }

    /**
     * Sets main menu buttons and toolbar items to enabled/disabled.
     *
     * @param isEnabled [Boolean] If true, buttons and toolbar items will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        toolbarMenu.getItem(0)?.isEnabled = isEnabled
        binding.sudosButton.isEnabled = isEnabled
    }

    /** Displays the loading [AlertDialog] indicating that an operation is occurring. */
    private fun showLoading(
        @StringRes textResId: Int,
    ) {
        loading = createLoadingAlertDialog(textResId)
        loading?.show()
        setItemsEnabled(false)
    }

    /** Dismisses the loading [AlertDialog] indicating that an operation has finished. */
    private fun hideLoading() {
        loading?.dismiss()
        if (bindingDelegate.isAttached()) {
            setItemsEnabled(true)
        }
    }
}
