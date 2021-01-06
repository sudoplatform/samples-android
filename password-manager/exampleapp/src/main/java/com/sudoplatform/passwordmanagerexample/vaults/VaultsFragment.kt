/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.vaults

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.passwordmanagerexample.App
import com.sudoplatform.passwordmanagerexample.MissingFragmentArgumentException
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.SUDO_ID_ARGUMENT
import com.sudoplatform.passwordmanagerexample.VAULT_ARGUMENT
import com.sudoplatform.passwordmanagerexample.createLoadingAlertDialog
import com.sudoplatform.passwordmanagerexample.showAlertDialog
import com.sudoplatform.passwordmanagerexample.swipe.SwipeLeftActionHelper
import com.sudoplatform.sudopasswordmanager.SudoPasswordManagerException
import com.sudoplatform.sudopasswordmanager.models.Vault
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_vaults.*
import kotlinx.android.synthetic.main.fragment_vaults.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This [VaultsFragment] presents a list of [Vault]s.
 *
 * - Links From:
 *  - [SudosFragment]: A user chooses the "Vaults" option from the Sudos view which will show
 *   this view with the list of [Vault]s created. The assigned alias is used as the text for each [Vault].
 *
 * - Links To:
 *  - [VaultFragment]: If a user chooses a [Vault] from the list, the [VaultFragment]
 *   will be presented so the user can add items to their [Vault].
 */
class VaultsFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** Toolbar [Menu] displaying title and toolbar items. */
    private lateinit var toolbarMenu: Menu

    /** A reference to the [RecyclerView.Adapter] handling [Vault] data. */
    private lateinit var adapter: VaultAdapter

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** The identifier of the Sudo that is the owner of the vaults listed here */
    private lateinit var sudoId: String

    /** The mutable list of [Vault]s that have been loaded from the SDK. */
    private val vaultList = mutableListOf<Vault>()

    private lateinit var app: App

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_vaults, container, false)

        app = requireActivity().application as App

        sudoId = requireArguments().getString(SUDO_ID_ARGUMENT)
            ?: throw MissingFragmentArgumentException("Sudo identifier missing")

        val toolbar = (view.toolbar as Toolbar)
        toolbar.title = getString(R.string.vaults)
        toolbar.inflateMenu(R.menu.nav_menu_with_lock_settings)
        toolbar.setOnMenuItemClickListener {
            when (it?.itemId) {
                R.id.lock -> {
                    launch {
                        withContext(Dispatchers.IO) {
                            app.sudoPasswordManager.lock()
                        }
                    }
                    navController.navigate(R.id.action_vaultsFragment_to_unlockVaultsFragment)
                }
                R.id.settings -> {
                    navController.navigate(R.id.action_vaultsFragment_to_settingsFragment)
                }
            }
            true
        }
        toolbarMenu = toolbar.menu
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView(view)
        navController = Navigation.findNavController(view)

        view.createVaultButton.setOnClickListener {
            createVault()
        }
    }

    override fun onResume() {
        super.onResume()
        listVaults()
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        super.onDestroy()
    }

    /**
     * List [Vault]s from the [PasswordManagerClient].
     */
    private fun listVaults() {
        launch {
            try {
                showLoading(R.string.loading_vaults)
                val vaults = withContext(Dispatchers.IO) {
                    app.sudoPasswordManager.listVaults().filter { vault ->
                        vault.owners.map { it.id }.contains(sudoId)
                    }
                }
                vaultList.clear()
                vaultList.addAll(vaults)
                vaultList.sortWith(
                    Comparator { lhs, rhs ->
                        when {
                            lhs.createdAt.before(rhs.createdAt) -> -1
                            lhs.createdAt.after(rhs.createdAt) -> 1
                            else -> 0
                        }
                    }
                )
                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                showAlertDialog(
                    titleResId = R.string.list_vaults_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { listVaults() },
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideLoading()
        }
    }

    /**
     * Create a new [Vault]
     */
    private fun createVault() {
        launch {
            try {
                showCreateDeleteAlert(R.string.creating_vault)
                withContext(Dispatchers.IO) {
                    app.sudoPasswordManager.createVault(sudoId)
                }
                hideCreateDeleteAlert()
                listVaults()
            } catch (e: SudoPasswordManagerException) {
                hideCreateDeleteAlert()
                showAlertDialog(
                    titleResId = R.string.create_vault_failure,
                    message = e.localizedMessage ?: "$e",
                    negativeButtonResId = android.R.string.cancel
                )
            }
        }
    }

    /**
     * Delete a selected [Vault] from the [VaultProfilesClient].
     *
     * @param vault The selected [Vault] to delete.
     */
    private fun deleteVault(vault: Vault) {
        launch {
            try {
                showCreateDeleteAlert(R.string.deleting_vaults)
                withContext(Dispatchers.IO) {
                    app.sudoPasswordManager.deleteVault(vault.id)
                }
                showAlertDialog(
                    titleResId = R.string.success,
                    positiveButtonResId = android.R.string.ok
                )
            } catch (e: SudoPasswordManagerException) {
                showAlertDialog(
                    titleResId = R.string.delete_vault_failure,
                    message = e.localizedMessage ?: "$e",
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideCreateDeleteAlert()
        }
    }

    /**
     * Configures the [RecyclerView] used to display the listed [Vault] items and listens to item
     * select events to navigate to the [LoginsFragment].
     */
    private fun configureRecyclerView(view: View) {
        adapter =
            VaultAdapter(requireContext(), vaultList) { vault ->
                val bundle = bundleOf(
                    VAULT_ARGUMENT to vault
                )
                navController.navigate(R.id.action_vaultsFragment_to_loginsFragment, bundle)
            }

        view.vaultRecyclerView.adapter = adapter
        view.vaultRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        configureSwipeToDelete()
    }

    /**
     * Sets buttons and recycler view to enabled/disabled.
     *
     * @param isEnabled If true, buttons and recycler view will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        createVaultButton?.isEnabled = isEnabled
        vaultRecyclerView?.isEnabled = isEnabled
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int = 0) {
        if (textResId != 0) {
            progressText.text = getString(textResId)
        }
        progressBar.visibility = View.VISIBLE
        progressText.visibility = View.VISIBLE
        vaultRecyclerView?.visibility = View.GONE
        setItemsEnabled(false)
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        progressBar?.visibility = View.GONE
        progressText?.visibility = View.GONE
        vaultRecyclerView?.visibility = View.VISIBLE
        setItemsEnabled(true)
    }

    /** Displays the loading [AlertDialog] indicating that a create or delete operation is occurring. */
    private fun showCreateDeleteAlert(@StringRes textResId: Int) {
        loading = createLoadingAlertDialog(textResId)
        loading?.show()
    }

    /** Dismisses the loading [AlertDialog] indicating that a create or delete operation has finished. */
    private fun hideCreateDeleteAlert() {
        loading?.dismiss()
    }

    /**
     * Configures the swipe to delete action by listening to [RecyclerView.ViewHolder] swipe events
     * and drawing the swipe view and delete icon.
     *
     * Swiping in from the left will perform a delete operation and remove the item from the view.
     */
    private fun configureSwipeToDelete() {
        val itemTouchCallback = SwipeLeftActionHelper(requireContext(), onSwipedAction = ::onSwiped)
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(vaultRecyclerView)
    }

    private fun onSwiped(viewHolder: RecyclerView.ViewHolder) {
        val vault = vaultList[viewHolder.adapterPosition]
        deleteVault(vault)
        vaultList.removeAt(viewHolder.adapterPosition)
        adapter.notifyItemRemoved(viewHolder.adapterPosition)
    }
}
