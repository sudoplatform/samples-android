/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
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
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudouser.SudoUserClient
import com.sudoplatform.sudouser.exceptions.RegisterException
import com.sudoplatform.sudovirtualcards.types.Card
import com.sudoplatform.sudovirtualcards.types.FundingSource
import com.sudoplatform.virtualcardsexample.App
import com.sudoplatform.virtualcardsexample.R
import com.sudoplatform.virtualcardsexample.cards.OrphanCardsFragment
import com.sudoplatform.virtualcardsexample.createLoadingAlertDialog
import com.sudoplatform.virtualcardsexample.fundingsources.FundingSourcesFragment
import com.sudoplatform.virtualcardsexample.identityverification.IdentityVerificationFragment
import com.sudoplatform.virtualcardsexample.register.RegisterFragment
import com.sudoplatform.virtualcardsexample.showAlertDialog
import com.sudoplatform.virtualcardsexample.sudos.SudosFragment
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_main_menu.*
import kotlinx.android.synthetic.main.fragment_main_menu.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
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
 *  - [IdentityVerificationFragment]: If a user taps the "Sudo ID Verification" button, the
 *   [IdentityVerificationFragment] will be presented so the user can perform Sudo ID verification.
 *  - [FundingSourcesFragment]: If a user taps the "Funding Sources" button, the [FundingSourcesFragment]
 *   will be presented so the user can view or choose to create [FundingSource]s.
 *  - [SudosFragment]: If a user taps the "Sudos" button, the [SudosFragment] will be presented so the
 *   user can view or choose to create [Sudo]s.
 *  - [OrphanCardsFragment]: If a user taps the "Orphan Cards" button, the [OrphanCardsFragment] will
 *   be presented so the user can view each orphan [Card] and its details.
 *  - [RegisterFragment]: If a user taps the "Deregister" button, the [RegisterFragment] will be
 *   presented so the user can perform registration again.
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
        toolbar.title = getString(R.string.virtual_cards)

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
                R.id.info -> {
                    showAlertDialog(
                        titleResId = R.string.what_is_a_virtual_card,
                        messageResId = R.string.virtual_card_explanation,
                        positiveButtonResId = android.R.string.ok,
                        negativeButtonResId = R.string.learn_more,
                        onNegative = { learnMore() }
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

        view.secureIdVerificationButton.setOnClickListener {
            navController.navigate(R.id.action_mainMenuFragment_to_identityVerificationFragment)
        }
        view.fundingSourcesButton.setOnClickListener {
            navController.navigate(R.id.action_mainMenuFragment_to_fundingSourcesFragment)
        }
        view.sudosButton.setOnClickListener {
            navController.navigate(R.id.action_mainMenuFragment_to_sudosFragment)
        }
        view.orphanCardsButton.setOnClickListener {
            navController.navigate(R.id.action_mainMenuFragment_to_orphanCardsFragment)
        }
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        super.onDestroy()
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
     * @param isEnabled If true, buttons and toolbar items will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        toolbarMenu.getItem(0)?.isEnabled = isEnabled
        toolbarMenu.getItem(1)?.isEnabled = isEnabled
        secureIdVerificationButton.isEnabled = isEnabled
        fundingSourcesButton.isEnabled = isEnabled
        sudosButton.isEnabled = isEnabled
        orphanCardsButton.isEnabled = isEnabled
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
