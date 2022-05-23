/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.sudos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
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
import com.sudoplatform.sudoprofiles.ListOption
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import com.sudoplatform.sudoprofiles.exceptions.SudoProfileException
import com.sudoplatform.virtualcardsexample.App
import com.sudoplatform.virtualcardsexample.R
import com.sudoplatform.virtualcardsexample.createLoadingAlertDialog
import com.sudoplatform.virtualcardsexample.databinding.FragmentSudosBinding
import com.sudoplatform.virtualcardsexample.mainmenu.MainMenuFragment
import com.sudoplatform.virtualcardsexample.showAlertDialog
import com.sudoplatform.virtualcardsexample.swipe.SwipeLeftActionHelper
import com.sudoplatform.virtualcardsexample.util.ObjectDelegate
import com.sudoplatform.virtualcardsexample.virtualcards.VirtualCardsFragment
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
 *  - [MainMenuFragment]: A user chooses the "Sudos" option from the main menu view which will show
 *  this view with the list of [Sudo]s created. The assigned alias is used as the text for each [Sudo].
 *
 * - Links To:
 *  - [CreateSudoFragment]: If a user taps the "Create Sudo" button, the [CreateSudoFragment] will
 *   be presented so the user can create a new [Sudo].
 *  - [VirtualCardsFragment]: If a user chooses a [Sudo] from the list, the [VirtualCardsFragment]
 *   will be presented so the user can add a new card to their [Sudo].
 */
class SudosFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentSudosBinding>()
    private val binding by bindingDelegate

    /** Toolbar [Menu] displaying title and toolbar items. */
    private lateinit var toolbarMenu: Menu

    /** A reference to the [RecyclerView.Adapter] handling [Sudo] data. */
    private lateinit var adapter: SudoAdapter

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** A mutable list of [Sudo]s. */
    private val sudoList = mutableListOf<Sudo>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingDelegate.attach(FragmentSudosBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.sudos)
            inflateMenu(R.menu.nav_menu_info)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.info -> {
                        showAlertDialog(
                            titleResId = R.string.what_is_a_sudo,
                            messageResId = R.string.sudo_explanation,
                            positiveButtonResId = android.R.string.ok,
                            negativeButtonResId = R.string.learn_more,
                            onNegative = { learnMore() }
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
        configureRecyclerView()
        navController = Navigation.findNavController(view)

        binding.createSudoButton.setOnClickListener {
            navController.navigate(
                SudosFragmentDirections.actionSudosFragmentToCreateSudoFragment()
            )
        }

        listSudos(ListOption.CACHE_ONLY)
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
        showLoading(R.string.loading_sudos)
        launch {
            try {
                val sudos = withContext(Dispatchers.IO) {
                    app.sudoProfilesClient.listSudos(listOption)
                }
                sudoList.clear()
                for (sudo in sudos) {
                    sudoList.add(sudo)
                }
                sudoList.sortWith { lhs, rhs ->
                    when {
                        lhs.createdAt.before(rhs.createdAt) -> -1
                        lhs.createdAt.after(rhs.createdAt) -> 1
                        else -> 0
                    }
                }
                adapter.notifyDataSetChanged()
            } catch (e: SudoProfileException) {
                showAlertDialog(
                    titleResId = R.string.list_sudos_failure,
                    message = e.localizedMessage ?: e.toString(),
                    positiveButtonResId = R.string.try_again,
                    onPositive = { listSudos(ListOption.REMOTE_ONLY) },
                    negativeButtonResId = android.R.string.cancel
                )
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * Delete a selected [Sudo] from the [SudoProfilesClient].
     *
     * @param sudo The selected [Sudo] to delete.
     */
    private fun deleteSudo(sudo: Sudo) {
        showDeleteAlert(R.string.deleting_sudos)
        launch {
            try {
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
            } finally {
                hideDeleteAlert()
            }
        }
    }

    /**
     * Configures the [RecyclerView] used to display the listed [Sudo] items and listens to item
     * select events to navigate to the [VirtualCardsFragment].
     */
    private fun configureRecyclerView() {
        adapter =
            SudoAdapter(sudoList) { sudo ->
                navController.navigate(
                    SudosFragmentDirections.actionSudosFragmentToVirtualCardsFragment(
                        sudo,
                    )
                )
            }
        binding.sudoRecyclerView.adapter = adapter
        binding.sudoRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        configureSwipeToDelete()
    }

    /** Navigates to a Sudo Platform web page when the "Learn More" button is pressed. */
    private fun learnMore() {
        val openUrl = Intent(Intent.ACTION_VIEW)
        openUrl.data = Uri.parse(getString(R.string.virtual_card_doc_url))
        startActivity(openUrl)
    }

    /**
     * Sets toolbar items, buttons and recycler view to enabled/disabled.
     *
     * @param isEnabled If true, toolbar items, buttons and recycler view will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        toolbarMenu.getItem(0)?.isEnabled = isEnabled
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

    /** Displays the loading [AlertDialog] indicating that a deletion operation is occurring. */
    private fun showDeleteAlert(@StringRes textResId: Int) {
        loading = createLoadingAlertDialog(textResId)
        loading?.show()
    }

    /** Dismisses the loading [AlertDialog] indicating that a deletion operation has finished. */
    private fun hideDeleteAlert() {
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

    private fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val sudo = sudoList[viewHolder.adapterPosition]
        deleteSudo(sudo)
        sudoList.removeAt(viewHolder.adapterPosition)
        adapter.notifyItemRemoved(viewHolder.adapterPosition)
    }
}
