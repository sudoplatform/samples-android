/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.sudos

import android.os.Bundle
import android.view.LayoutInflater
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
import com.sudoplatform.emailexample.App
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.createLoadingAlertDialog
import com.sudoplatform.emailexample.emailaddresses.EmailAddressesFragment
import com.sudoplatform.emailexample.mainmenu.MainMenuFragment
import com.sudoplatform.emailexample.showAlertDialog
import com.sudoplatform.emailexample.swipe.SwipeLeftActionHelper
import com.sudoplatform.sudoprofiles.ListOption
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import com.sudoplatform.sudoprofiles.exceptions.SudoProfileException
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_sudos.*
import kotlinx.android.synthetic.main.fragment_sudos.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This [SudosFragment] presents a list of [Sudo]s.
 *
 * - Links From:
 *  - [MainMenuFragment]: A user chooses the "Sudos" option from the main menu view which will show
 *   this view with the list of [Sudo]s created. The assigned alias is used as the text for each [Sudo].
 *
 * - Links To:
 *  - [CreateSudoFragment]: If a user taps the "Create Sudo" button, the [CreateSudoFragment] will
 *   be presented so the user can create a new [Sudo].
 *  - [EmailAddressesFragment]: If a user chooses a [Sudo] from the list, the [EmailAddressesFragment]
 *   will be presented so the user can add a new email address to their [Sudo].
 */
class SudosFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** A reference to the [RecyclerView.Adapter] handling [Sudo] data. */
    private lateinit var adapter: SudoAdapter

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private lateinit var loading: AlertDialog

    /** A mutable list of [Sudo]s. */
    private val sudoList = mutableListOf<Sudo>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sudos, container, false)
        val toolbar = (view.toolbar as Toolbar)
        toolbar.title = getString(R.string.sudos)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView(view)
        navController = Navigation.findNavController(view)

        view.createSudoButton.setOnClickListener {
            navController.navigate(R.id.action_sudosFragment_to_createSudoFragment)
        }

        listSudos(ListOption.CACHE_ONLY)
    }

    override fun onResume() {
        super.onResume()
        listSudos(ListOption.REMOTE_ONLY)
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        super.onDestroy()
    }

    /**
     * List [Sudo]s from the [SudoProfilesClient].
     *
     * @param listOption Option of either retrieving [Sudo] data from the cache or network.
     */
    private fun listSudos(listOption: ListOption) {
        val app = requireActivity().application as App
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
                sudoList.sortWith(
                    Comparator { lhs, rhs ->
                        when {
                            lhs.createdAt.before(rhs.createdAt) -> -1
                            lhs.createdAt.after(rhs.createdAt) -> 1
                            else -> 0
                        }
                    }
                )
                adapter.notifyDataSetChanged()
            } catch (e: SudoProfileException) {
                showAlertDialog(
                    titleResId = R.string.list_sudos_failure,
                    message = e.localizedMessage ?: e.toString(),
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
        val app = requireActivity().application as App
        launch {
            try {
                showDeleteAlert(R.string.deleting_sudos)
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
                    message = e.localizedMessage ?: e.toString(),
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideDeleteAlert()
        }
    }

    /**
     * Configures the [RecyclerView] used to display the listed [Sudo] items and listens to item
     * select events to navigate to the [EmailAddressesFragment].
     */
    private fun configureRecyclerView(view: View) {
        adapter =
            SudoAdapter(sudoList) { sudo ->
                val bundle = bundleOf(
                    getString(R.string.sudo_id) to sudo.id,
                    getString(R.string.sudo_label) to sudo.label
                )
                navController.navigate(R.id.action_sudosFragment_to_emailAddressesFragment, bundle)
            }

        view.sudoRecyclerView.adapter = adapter
        view.sudoRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        configureSwipeToDelete()
    }

    /**
     * Sets buttons and recycler view to enabled/disabled.
     *
     * @param isEnabled If true, buttons and recycler view will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        createSudoButton?.isEnabled = isEnabled
        sudoRecyclerView?.isEnabled = isEnabled
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int = 0) {
        if (textResId != 0) {
            progressText.text = getString(textResId)
        }
        progressBar.visibility = View.VISIBLE
        progressText.visibility = View.VISIBLE
        sudoRecyclerView?.visibility = View.GONE
        setItemsEnabled(false)
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        progressBar?.visibility = View.GONE
        progressText?.visibility = View.GONE
        sudoRecyclerView?.visibility = View.VISIBLE
        setItemsEnabled(true)
    }

    /** Displays the loading [AlertDialog] indicating that a deletion operation is occurring. */
    private fun showDeleteAlert(@StringRes textResId: Int) {
        loading = createLoadingAlertDialog(textResId)
        loading.show()
    }

    /** Dismisses the loading [AlertDialog] indicating that a deletion operation has finished. */
    private fun hideDeleteAlert() {
        loading.dismiss()
    }

    /**
     * Configures the swipe to delete action by listening to [RecyclerView.ViewHolder] swipe events
     * and drawing the swipe view and delete icon.
     *
     * Swiping in from the left will perform a delete operation and remove the item from the view.
     */
    private fun configureSwipeToDelete() {
        val itemTouchCallback = SwipeLeftActionHelper(requireContext(), onSwipedAction = ::onSwiped)
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(sudoRecyclerView)
    }

    private fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val sudo = sudoList[viewHolder.adapterPosition]
        deleteSudo(sudo)
        sudoList.removeAt(viewHolder.adapterPosition)
        adapter.notifyItemRemoved(viewHolder.adapterPosition)
    }
}
