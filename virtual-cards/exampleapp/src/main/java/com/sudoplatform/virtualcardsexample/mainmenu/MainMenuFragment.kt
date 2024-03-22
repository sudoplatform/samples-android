/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.mainmenu

import android.content.Intent
import android.net.Uri
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
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudouser.SudoUserClient
import com.sudoplatform.sudouser.exceptions.RegisterException
import com.sudoplatform.sudovirtualcards.types.FundingSource
import com.sudoplatform.sudovirtualcards.types.VirtualCard
import com.sudoplatform.virtualcardsexample.App
import com.sudoplatform.virtualcardsexample.R
import com.sudoplatform.virtualcardsexample.createLoadingAlertDialog
import com.sudoplatform.virtualcardsexample.databinding.FragmentMainMenuBinding
import com.sudoplatform.virtualcardsexample.fundingsources.FundingSourcesFragment
import com.sudoplatform.virtualcardsexample.identityverification.IdentityVerificationFragment
import com.sudoplatform.virtualcardsexample.register.RegisterFragment
import com.sudoplatform.virtualcardsexample.showAlertDialog
import com.sudoplatform.virtualcardsexample.sudos.SudosFragment
import com.sudoplatform.virtualcardsexample.util.ObjectDelegate
import com.sudoplatform.virtualcardsexample.virtualcards.OrphanVirtualCardsFragment
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
 *  - [IdentityVerificationFragment]: If a user taps the "Sudo ID Verification" button, the
 *   [IdentityVerificationFragment] will be presented so the user can perform Sudo ID verification.
 *  - [FundingSourcesFragment]: If a user taps the "Funding Sources" button, the [FundingSourcesFragment]
 *   will be presented so the user can view or choose to create [FundingSource]s.
 *  - [SudosFragment]: If a user taps the "Sudos" button, the [SudosFragment] will be presented so the
 *   user can view or choose to create [Sudo]s.
 *  - [OrphanVirtualCardsFragment]: If a user taps the "Orphan Virtual Cards" button, the [OrphanVirtualCardsFragment]
 *   will be presented so the user can view each orphan [VirtualCard] and its details.
 *  - [RegisterFragment]: If a user taps the "Deregister" button, the [RegisterFragment] will be
 *   presented so the user can perform registration again.
 */
class MainMenuFragment : Fragment(), CoroutineScope {

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
            title = getString(R.string.virtual_cards)
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
                    R.id.info -> {
                        showAlertDialog(
                            titleResId = R.string.what_is_a_virtual_card,
                            messageResId = R.string.virtual_card_explanation,
                            positiveButtonResId = android.R.string.ok,
                            negativeButtonResId = R.string.learn_more,
                            onNegative = { learnMore() },
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        binding.secureIdVerificationButton.setOnClickListener {
            navController.navigate(
                MainMenuFragmentDirections.actionMainMenuFragmentToIdentityVerificationFragment(),
            )
        }
        binding.fundingSourcesButton.setOnClickListener {
            navController.navigate(
                MainMenuFragmentDirections.actionMainMenuFragmentToFundingSourcesFragment(),
            )
        }
        binding.sudosButton.setOnClickListener {
            navController.navigate(
                MainMenuFragmentDirections.actionMainMenuFragmentToSudosFragment(),
            )
        }
        binding.orphanVirtualCardsButton.setOnClickListener {
            navController.navigate(
                MainMenuFragmentDirections.actionMainMenuFragmentToOrphanVirtualCardsFragment(),
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
                    app.sudoVirtualCardsClient.reset()
                    app.sudoProfilesClient.reset()
                    app.sudoUserClient.reset()
                }
                hideLoading()
                navController.navigate(
                    MainMenuFragmentDirections.actionMainMenuFragmentToRegisterFragment(),
                )
            } catch (e: RegisterException) {
                Toast.makeText(requireContext(), getString(R.string.deregister_failure, e.localizedMessage), Toast.LENGTH_LONG).show()
            }
        }
    }

    /** Navigates to a Sudo Platform web page when the "Learn More" button is pressed. */
    private fun learnMore() {
        val openUrl = Intent(Intent.ACTION_VIEW)
        openUrl.data = Uri.parse(getString(R.string.virtual_card_doc_url))
        startActivity(openUrl)
    }

    /**
     * Sets main menu buttons and toolbar items to enabled/disabled.
     *
     * @param isEnabled [Boolean] If true, buttons and toolbar items will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        toolbarMenu.getItem(0)?.isEnabled = isEnabled
        toolbarMenu.getItem(1)?.isEnabled = isEnabled
        binding.secureIdVerificationButton.isEnabled = isEnabled
        binding.fundingSourcesButton.isEnabled = isEnabled
        binding.sudosButton.isEnabled = isEnabled
        binding.orphanVirtualCardsButton.isEnabled = isEnabled
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
        if (bindingDelegate.isAttached()) {
            setItemsEnabled(true)
        }
    }
}
