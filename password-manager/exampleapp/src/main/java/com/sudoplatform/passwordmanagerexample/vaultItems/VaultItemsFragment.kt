/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.vaultItems

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.passwordmanagerexample.App
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.createLoadingAlertDialog
import com.sudoplatform.passwordmanagerexample.databinding.FragmentVaultItemsBinding
import com.sudoplatform.passwordmanagerexample.showAlertDialog
import com.sudoplatform.passwordmanagerexample.swipe.SwipeLeftActionHelper
import com.sudoplatform.passwordmanagerexample.util.ObjectDelegate
import com.sudoplatform.passwordmanagerexample.vaults.VaultsFragment
import com.sudoplatform.sudopasswordmanager.models.Vault
import com.sudoplatform.sudopasswordmanager.models.VaultBankAccount
import com.sudoplatform.sudopasswordmanager.models.VaultCreditCard
import com.sudoplatform.sudopasswordmanager.models.VaultItem
import com.sudoplatform.sudopasswordmanager.models.VaultLogin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * This [VaultItemsFragment] presents a list of [VaultLogin]s.
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
class VaultItemsFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** View binding to the views defined in the layout */
    private val bindingDelegate = ObjectDelegate<FragmentVaultItemsBinding>()
    private val binding by bindingDelegate

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** A reference to the [RecyclerView.Adapter] handling [VaultLogin] data. */
    private lateinit var adapter: VaultItemAdapter

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** The mutable list of [VaultLogin]s that have been loaded from the SDK. */
    private val itemList = mutableListOf<VaultItem>()

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: VaultItemsFragmentArgs by navArgs()

    /** Vault contents being shown and modified */
    private lateinit var vault: Vault

    private lateinit var app: App

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bindingDelegate.attach(FragmentVaultItemsBinding.inflate(inflater, container, false))

        app = requireActivity().application as App
        vault = args.vault

        with(binding.toolbar.root) {
            title = getString(R.string.items)
            inflateMenu(R.menu.nav_menu_with_lock_settings)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.lock -> {
                        launch {
                            withContext(Dispatchers.IO) {
                                app.sudoPasswordManager.lock()
                            }
                        }
                        navController.navigate(VaultItemsFragmentDirections.actionVaultItemsFragmentToUnlockVaultsFragment())
                    }
                    R.id.settings -> {
                        navController.navigate(VaultItemsFragmentDirections.actionVaultItemsFragmentToSettingsFragment())
                    }
                }
                true
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView()
        navController = Navigation.findNavController(view)

        binding.createItemButton.setOnClickListener {
            createItem()
        }
    }

    override fun onResume() {
        super.onResume()
        listItems()
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    /**
     * List [VaultLogin]s from the [PasswordManagerClient].
     */
    private fun listItems() {
        launch {
            try {
                showLoading(R.string.loading_items)
                val items = withContext(Dispatchers.IO) {
                    app.sudoPasswordManager.listVaultItems(vault)
                }
                itemList.clear()
                for (item in items) {
                    itemList.add(item)
                }
                itemList.sortWith(
                    Comparator { lhs, rhs ->
                        when {
                            lhs.createdAt.before(rhs.createdAt) -> -1
                            lhs.createdAt.after(rhs.createdAt) -> 1
                            else -> 0
                        }
                    }
                )
                adapter.notifyDataSetChanged()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showAlertDialog(
                    titleResId = R.string.list_items_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { listItems() },
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideLoading()
        }
    }

    /**
     * Create a new [VaultItem]
     */
    private fun createItem() {
        val itemTypes = arrayOf(getString(R.string.login), getString(R.string.credit_card), getString(R.string.bank_account))
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.create_vault_item)
            .setItems(itemTypes) { _, which ->
                when (which) {
                    0 -> {
                        navController.navigate(VaultItemsFragmentDirections.actionVaultItemsFragmentToCreateLoginFragment(vault))
                    }
                    1 -> {
                        navController.navigate(VaultItemsFragmentDirections.actionVaultItemsFragmentToCreateCreditCardFragment(vault))
                    }
                    2 -> {
                        navController.navigate(VaultItemsFragmentDirections.actionVaultItemsFragmentToCreateBankAccountFragment(vault))
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .show()
    }

    /**
     * Delete a selected [VaultItem] from the [Vault].
     *
     * @param login The selected [VaultItem] to delete.
     */
    private fun deleteItem(item: VaultItem) {
        launch {
            try {
                showCreateDeleteAlert(R.string.deleting_items)
                withContext(Dispatchers.IO) {
                    app.sudoPasswordManager.removeVaultItem(item.id, vault)
                }
                showAlertDialog(
                    titleResId = R.string.success,
                    positiveButtonResId = android.R.string.ok
                )
            } catch (e: CancellationException) {
                throw e
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

    /**
     * Configures the [RecyclerView] used to display the listed [VaultLogin] items and listens to item
     * select events to navigate to the [EditLoginFragment].
     */
    private fun configureRecyclerView() {
        adapter = VaultItemAdapter(itemList) { item ->
            (item as? VaultLogin)?.let {
                navController.navigate(VaultItemsFragmentDirections.actionVaultItemsFragmentToEditLoginFragment(vault, it))
            }
            (item as? VaultCreditCard)?.let {
                navController.navigate(VaultItemsFragmentDirections.actionVaultItemsFragmentToEditCreditCardFragment(vault, it))
            }
            (item as? VaultBankAccount)?.let {
                navController.navigate(VaultItemsFragmentDirections.actionVaultItemsFragmentToEditBankAccountFragment(vault, it))
            }
        }
        binding.itemRecyclerView.adapter = adapter
        binding.itemRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        configureSwipeToDelete()
    }

    /**
     * Sets buttons and recycler view to enabled/disabled.
     *
     * @param isEnabled If true, buttons and recycler view will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        binding.createItemButton.isEnabled = isEnabled
        binding.itemRecyclerView.isEnabled = isEnabled
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int = 0) {
        if (textResId != 0) {
            binding.progressText.text = getString(textResId)
        }
        binding.progressBar.visibility = View.VISIBLE
        binding.progressText.visibility = View.VISIBLE
        binding.itemRecyclerView.visibility = View.GONE
        setItemsEnabled(false)
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        if (bindingDelegate.isAttached()) {
            binding.progressBar.visibility = View.GONE
            binding.progressText.visibility = View.GONE
            binding.itemRecyclerView.visibility = View.VISIBLE
            setItemsEnabled(true)
        }
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
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.itemRecyclerView)
    }

    private fun onSwiped(viewHolder: RecyclerView.ViewHolder) {
        val item = itemList[viewHolder.adapterPosition]
        deleteItem(item)
        itemList.removeAt(viewHolder.adapterPosition)
        adapter.notifyItemRemoved(viewHolder.adapterPosition)
    }
}
