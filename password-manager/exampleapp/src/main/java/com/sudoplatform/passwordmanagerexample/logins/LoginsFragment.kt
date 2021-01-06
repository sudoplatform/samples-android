/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.logins

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
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.VAULT_ARGUMENT
import com.sudoplatform.passwordmanagerexample.VAULT_LOGIN_ARGUMENT
import com.sudoplatform.passwordmanagerexample.createLoadingAlertDialog
import com.sudoplatform.passwordmanagerexample.showAlertDialog
import com.sudoplatform.passwordmanagerexample.swipe.SwipeLeftActionHelper
import com.sudoplatform.passwordmanagerexample.vaults.VaultsFragment
import com.sudoplatform.sudopasswordmanager.models.Vault
import com.sudoplatform.sudopasswordmanager.models.VaultLogin
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_logins.createLoginButton
import kotlinx.android.synthetic.main.fragment_logins.loginRecyclerView
import kotlinx.android.synthetic.main.fragment_logins.view.createLoginButton
import kotlinx.android.synthetic.main.fragment_logins.view.loginRecyclerView
import kotlinx.android.synthetic.main.fragment_vaults.progressBar
import kotlinx.android.synthetic.main.fragment_vaults.progressText
import kotlinx.android.synthetic.main.fragment_vaults.view.toolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This [LoginsFragment] presents a list of [VaultLogin]s.
 *
 * - Links From:
 *  - [VaultsFragment]: A user selects a vault which will show this view with the associated [VaultLogin]s.
 *
 * - Links To:
 *  - [CreateLoginFragment]: If a user clicks on the "Create Login" button the [CreateLoginFragment]
 *   will be presented so they can create a new [VaultLogin]
 *  - [EditLoginFragment]: If a user chooses a [VaultLogin] from the list, the [EditLoginFragment]
 *   will be presented so the user can add edit an existing [VaultLogin].
 */
class LoginsFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** Toolbar [Menu] displaying title and toolbar items. */
    private lateinit var toolbarMenu: Menu

    /** A reference to the [RecyclerView.Adapter] handling [VaultLogin] data. */
    private lateinit var adapter: LoginAdapter

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** The mutable list of [VaultLogin]s that have been loaded from the SDK. */
    private val loginList = mutableListOf<VaultLogin>()

    private var vault: Vault? = null

    private lateinit var app: App

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_logins, container, false)

        app = requireActivity().application as App

        val toolbar = (view.toolbar as Toolbar)
        toolbar.title = getString(R.string.logins)
        toolbar.inflateMenu(R.menu.nav_menu_with_lock_settings)
        toolbar.setOnMenuItemClickListener {
            when (it?.itemId) {
                R.id.lock -> {
                    launch {
                        withContext(Dispatchers.IO) {
                            app.sudoPasswordManager.lock()
                        }
                    }
                    navController.navigate(R.id.action_loginsFragment_to_unlockVaultsFragment)
                }
                R.id.settings -> {
                    navController.navigate(R.id.action_loginsFragment_to_settingsFragment)
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

        view.createLoginButton.setOnClickListener {
            createLogin()
        }

        vault = requireArguments().getParcelable(VAULT_ARGUMENT)
    }

    override fun onResume() {
        super.onResume()
        listLogins()
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        super.onDestroy()
    }

    /**
     * List [VaultLogin]s from the [PasswordManagerClient].
     */
    private fun listLogins() {
        vault?.let { vault ->
            launch {
                try {
                    showLoading(R.string.loading_logins)
                    val logins = withContext(Dispatchers.IO) {
                        app.sudoPasswordManager.listVaultItems(vault)
                    }
                    loginList.clear()
                    for (login in logins) {
                        loginList.add(login as VaultLogin)
                    }
                    loginList.sortWith(
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
                        titleResId = R.string.list_logins_failure,
                        message = e.localizedMessage ?: "$e",
                        positiveButtonResId = R.string.try_again,
                        onPositive = { listLogins() },
                        negativeButtonResId = android.R.string.cancel
                    )
                }
                hideLoading()
            }
        }
    }

    /**
     * Create a new [VaultLogin]
     */
    private fun createLogin() {
        val bundle = bundleOf(
            VAULT_ARGUMENT to vault
        )
        navController.navigate(R.id.action_loginsFragment_to_createLoginFragment, bundle)
    }

    /**
     * Delete a selected [VaultLogin] from the [Vault].
     *
     * @param login The selected [VaultLogin] to delete.
     */
    private fun deleteLogin(login: VaultLogin) {
        vault?.let { vault ->
            launch {
                try {
                    showCreateDeleteAlert(R.string.deleting_logins)
                    withContext(Dispatchers.IO) {
                        app.sudoPasswordManager.removeVaultItem(login.id, vault)
                    }
                    showAlertDialog(
                        titleResId = R.string.success,
                        positiveButtonResId = android.R.string.ok
                    )
                } catch (e: Exception) {
                    showAlertDialog(
                        titleResId = R.string.delete_vault_failure,
                        message = e.localizedMessage ?: "$e",
                        negativeButtonResId = android.R.string.cancel
                    )
                }
                hideCreateDeleteAlert()
            }
        }
    }

    /**
     * Configures the [RecyclerView] used to display the listed [VaultLogin] items and listens to item
     * select events to navigate to the [EditLoginFragment].
     */
    private fun configureRecyclerView(view: View) {
        adapter = LoginAdapter(loginList) { login ->
            val bundle = bundleOf(
                VAULT_ARGUMENT to vault,
                VAULT_LOGIN_ARGUMENT to login
            )
            navController.navigate(R.id.action_loginsFragment_to_editLoginFragment, bundle)
        }
        view.loginRecyclerView.adapter = adapter
        view.loginRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        configureSwipeToDelete()
    }

    /**
     * Sets buttons and recycler view to enabled/disabled.
     *
     * @param isEnabled If true, buttons and recycler view will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        createLoginButton?.isEnabled = isEnabled
        loginRecyclerView?.isEnabled = isEnabled
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int = 0) {
        if (textResId != 0) {
            progressText.text = getString(textResId)
        }
        progressBar.visibility = View.VISIBLE
        progressText.visibility = View.VISIBLE
        loginRecyclerView?.visibility = View.GONE
        setItemsEnabled(false)
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        progressBar?.visibility = View.GONE
        progressText?.visibility = View.GONE
        loginRecyclerView?.visibility = View.VISIBLE
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
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(loginRecyclerView)
    }

    private fun onSwiped(viewHolder: RecyclerView.ViewHolder) {
        val login = loginList[viewHolder.adapterPosition]
        deleteLogin(login)
        loginList.removeAt(viewHolder.adapterPosition)
        adapter.notifyItemRemoved(viewHolder.adapterPosition)
    }
}
