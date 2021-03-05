/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.passwordmanagerexample.sudos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.passwordmanagerexample.App
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.createLoadingAlertDialog
import com.sudoplatform.passwordmanagerexample.databinding.FragmentSudosBinding
import com.sudoplatform.passwordmanagerexample.showAlertDialog
import com.sudoplatform.passwordmanagerexample.swipe.SwipeLeftActionHelper
import com.sudoplatform.passwordmanagerexample.util.ObjectDelegate
import com.sudoplatform.sudoprofiles.ListOption
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import com.sudoplatform.sudoprofiles.exceptions.SudoProfileException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * This [SudosFragment] presents a list of [Sudo]s.
 *
 * - Links From:
 *  - [RegisterFragment]: After registering this view with the list of [Sudo]s created.
 *  The assigned alias is used as the text for each [Sudo].
 *
 * - Links To:
 *  - [VaultsFragment]: If a user chooses a [Sudo] from the list, the [VaultsFragment]
 *   will be presented so the user can add a new vault to their [Sudo].
 */
class SudosFragment : Fragment(), CoroutineScope {

    companion object {
        val sudosComparator: Comparator<Sudo> = Comparator { lhs, rhs ->
            when {
                lhs.createdAt.before(rhs.createdAt) -> -1
                lhs.createdAt.after(rhs.createdAt) -> 1
                else -> 0
            }
        }
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** View binding to the views defined in the layout */
    private val bindingDelegate = ObjectDelegate<FragmentSudosBinding>()
    private val binding by bindingDelegate

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** A reference to the [RecyclerView.Adapter] handling [Sudo] data. */
    private lateinit var adapter: SudoAdapter

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** A mutable list of [Sudo]s. */
    private val sudoList = mutableListOf<Sudo>()

    /** The [Application] that holds references to the APIs this fragment needs */
    private lateinit var app: App

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bindingDelegate.attach(FragmentSudosBinding.inflate(inflater, container, false))

        app = requireActivity().application as App

        with(binding.toolbar.root) {
            title = getString(R.string.sudos)
            inflateMenu(R.menu.nav_menu_sudos_menu)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.lock -> {
                        launch {
                            withContext(Dispatchers.IO) {
                                app.sudoPasswordManager.lock()
                            }
                        }
                        navController.navigate(SudosFragmentDirections.actionSudosFragmentToUnlockVaultsFragment())
                    }
                    R.id.info -> {
                        showInfo()
                    }
                    R.id.settings -> {
                        navController.navigate(SudosFragmentDirections.actionSudosFragmentToSettingsFragment())
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

        binding.createSudoButton.setOnClickListener {
            createSudoWithoutClaims()
        }
    }

    override fun onResume() {
        super.onResume()
        listSudos(ListOption.REMOTE_ONLY)
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    /**
     * List [Sudo]s from the [SudoProfilesClient].
     *
     * @param listOption Option of either retrieving [Sudo] data from the cache or network.
     */
    private fun listSudos(listOption: ListOption) {
        launch {
            try {
                showLoading(R.string.loading_sudos)
                val sudos = withContext(Dispatchers.IO) {
                    app.sudoProfilesClient.listSudos(listOption)
                }
                sudoList.clear()
                for (sudo in sudos) {
                    sudoList.add(sudo)
                }
                sudoList.sortWith(sudosComparator)
                adapter.notifyDataSetChanged()
            } catch (e: SudoProfileException) {
                showAlertDialog(
                    titleResId = R.string.list_sudos_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { listSudos(ListOption.REMOTE_ONLY) },
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideLoading()
        }
    }

    /**
     * Delete a selected [Sudo] from the [SudoProfilesClient].
     *
     * @param sudo The selected [Sudo] to delete.
     */
    private fun deleteSudo(sudo: Sudo) {
        launch {
            try {
                showCreateDeleteAlert(R.string.deleting_sudos)
                withContext(Dispatchers.IO) {
                    app.sudoProfilesClient.deleteSudo(sudo)
                }
                showAlertDialog(
                    titleResId = R.string.success,
                    positiveButtonResId = android.R.string.ok
                )
            } catch (e: SudoProfileException) {
                showAlertDialog(
                    titleResId = R.string.delete_sudo_failure,
                    message = e.localizedMessage ?: "$e",
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideCreateDeleteAlert()
        }
    }
    /**
     * Create an empty sudo without claims (all parameters nil).
     * This allows testing across multiple devices without transferring keys belonging to the `SudoProfiles` SDK.
     */
    private fun createSudoWithoutClaims() {
        val sudo = Sudo(null)
        launch {
            try {
                showCreateDeleteAlert(R.string.creating_sudo)
                withContext(Dispatchers.IO) {
                    app.sudoProfilesClient.createSudo(sudo)
                }
                hideCreateDeleteAlert()
                listSudos(ListOption.REMOTE_ONLY)
            } catch (e: SudoProfileException) {
                hideCreateDeleteAlert()
                showAlertDialog(
                    titleResId = R.string.something_wrong,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { createSudoWithoutClaims() },
                    negativeButtonResId = android.R.string.cancel
                )
            }
        }
    }

    /**
     * Configures the [RecyclerView] used to display the listed [Sudo] items and listens to item
     * select events to navigate to the [VaultsFragment].
     */
    private fun configureRecyclerView() {
        adapter =
            SudoAdapter(sudoList) { sudo ->
                navController.navigate(SudosFragmentDirections.actionSudosFragmentToVaultsFragment(sudo.id!!))
            }

        binding.sudoRecyclerView.adapter = adapter
        binding.sudoRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        configureSwipeToDelete()
    }

    private fun showInfo() {
        showAlertDialog(
            titleResId = R.string.info_title,
            messageResId = R.string.create_sudo_learn_more,
            positiveButtonResId = android.R.string.ok,
            negativeButtonResId = R.string.learn_more,
            onNegative = { learnMoreAboutSudos() }
        )
    }

    /**
     * Sets buttons and recycler view to enabled/disabled.
     *
     * @param isEnabled If true, buttons and recycler view will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        binding.createSudoButton.isEnabled = isEnabled
        binding.sudoRecyclerView.isEnabled = isEnabled
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int = 0) {
        if (textResId != 0) {
            binding.progressText.text = getString(textResId)
        }
        binding.progressBar.visibility = View.VISIBLE
        binding.progressText.visibility = View.VISIBLE
        binding.sudoRecyclerView.visibility = View.GONE
        setItemsEnabled(false)
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        if (bindingDelegate.isAttached()) {
            binding.progressBar.visibility = View.GONE
            binding.progressText.visibility = View.GONE
            binding.sudoRecyclerView.visibility = View.VISIBLE
            setItemsEnabled(true)
        }
    }

    /** Displays the loading [AlertDialog] indicating that a creation/deletion operation is occurring. */
    private fun showCreateDeleteAlert(@StringRes textResId: Int) {
        loading = createLoadingAlertDialog(textResId)
        loading?.show()
    }

    /** Dismisses the loading [AlertDialog] indicating that a creation/deletion operation has finished. */
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
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.sudoRecyclerView)
    }

    private fun onSwiped(viewHolder: RecyclerView.ViewHolder) {
        val sudo = sudoList[viewHolder.adapterPosition]
        deleteSudo(sudo)
        sudoList.removeAt(viewHolder.adapterPosition)
        adapter.notifyItemRemoved(viewHolder.adapterPosition)
    }
}
