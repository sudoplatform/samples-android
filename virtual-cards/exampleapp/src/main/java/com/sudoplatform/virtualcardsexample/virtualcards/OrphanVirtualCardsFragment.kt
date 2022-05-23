/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.virtualcards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudoprofiles.ListOption
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import com.sudoplatform.sudoprofiles.exceptions.SudoProfileException
import com.sudoplatform.sudovirtualcards.SudoVirtualCardsClient
import com.sudoplatform.sudovirtualcards.types.CachePolicy
import com.sudoplatform.sudovirtualcards.types.ListAPIResult
import com.sudoplatform.sudovirtualcards.types.VirtualCard
import com.sudoplatform.virtualcardsexample.App
import com.sudoplatform.virtualcardsexample.R
import com.sudoplatform.virtualcardsexample.databinding.FragmentOrphanVirtualCardsBinding
import com.sudoplatform.virtualcardsexample.mainmenu.MainMenuFragment
import com.sudoplatform.virtualcardsexample.showAlertDialog
import com.sudoplatform.virtualcardsexample.util.ObjectDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * This [OrphanVirtualCardsFragment] presents a list of orphan [VirtualCard]s which are associated
 * with deleted Sudos.
 *
 * - Links From:
 *  - [MainMenuFragment]: A user chooses the "Orphan Virtual Cards" option from the main menu which
 *   will show this view with the list of orphan virtual cards. The orphan card's [VirtualCard.metadata]
 *   property is used as the text for each virtual card.
 *
 * - Links To:
 *  - [VirtualCardDetailFragment]: If a user chooses an orphan [VirtualCard] from the list, the
 *   [VirtualCardDetailFragment] will be presented so the user can view virtual card details and
 *   transactions.
 */
class OrphanVirtualCardsFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentOrphanVirtualCardsBinding>()
    private val binding by bindingDelegate

    /** A reference to the [RecyclerView.Adapter] handling orphan [VirtualCard] data. */
    private lateinit var adapter: VirtualCardAdapter

    /** A mutable list of orphan [VirtualCard]s that are associated with a deleted [Sudo]'s identifier. */
    private var orphanCardList = mutableListOf<VirtualCard>()

    /** A mutable list of [Sudo]s to check for deleted [Sudo]s. */
    private var sudoList = mutableListOf<Sudo>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingDelegate.attach(FragmentOrphanVirtualCardsBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.orphan_virtual_cards)
        }
        app = requireActivity().application as App
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView()
        navController = Navigation.findNavController(view)

        listSudos(ListOption.REMOTE_ONLY)
        listOrphanVirtualCards(CachePolicy.REMOTE_ONLY)
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    /**
     * List orphan [VirtualCard]s from the [SudoVirtualCardsClient].
     *
     * @param cachePolicy Option of either retrieving [VirtualCard] data from the cache or network.
     */
    private fun listOrphanVirtualCards(cachePolicy: CachePolicy) {
        launch {
            try {
                showLoading()
                val orphanVirtualCards = withContext(Dispatchers.IO) {
                    app.sudoVirtualCardsClient.listVirtualCards(cachePolicy = cachePolicy)
                }
                when (orphanVirtualCards) {
                    is ListAPIResult.Success -> {
                        orphanCardList.clear()
                        val sudoIds = sudoList.map { it.id ?: "" }
                        for (card in orphanVirtualCards.result.items) {
                            if (card.owners.all { !sudoIds.contains(it.id) }) {
                                orphanCardList.add(card)
                            }
                        }
                        setEmptyOrphanVirtualCardsLabel()
                        adapter.notifyDataSetChanged()
                    }
                    is ListAPIResult.Partial -> {
                        val cause = orphanVirtualCards.result.failed.first().cause
                        showAlertDialog(
                            titleResId = R.string.list_orphan_virtual_cards_failure,
                            message = cause.localizedMessage ?: "$cause",
                            positiveButtonResId = R.string.try_again,
                            onPositive = { listOrphanVirtualCards(CachePolicy.REMOTE_ONLY) },
                            negativeButtonResId = android.R.string.cancel
                        )
                    }
                }
            } catch (e: SudoVirtualCardsClient.VirtualCardException) {
                showAlertDialog(
                    titleResId = R.string.list_orphan_virtual_cards_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { listOrphanVirtualCards(CachePolicy.REMOTE_ONLY) },
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideLoading()
        }
    }

    /**
     * List [Sudo]s from the [SudoProfilesClient].
     *
     * @param listOption Option of either retrieving [Sudo] data from the cache or network.
     */
    private fun listSudos(listOption: ListOption) {
        launch {
            try {
                sudoList = withContext(Dispatchers.IO) {
                    app.sudoProfilesClient.listSudos(listOption)
                }.toMutableList()
            } catch (e: SudoProfileException) {
                showAlertDialog(
                    titleResId = R.string.list_sudos_failure,
                    message = e.localizedMessage ?: e.toString(),
                    positiveButtonResId = R.string.try_again,
                    onPositive = { listSudos(ListOption.REMOTE_ONLY) },
                    negativeButtonResId = android.R.string.cancel
                )
            }
        }
    }

    /**
     * Configures the [RecyclerView] used to display the listed orphan [VirtualCard] items and listens
     * to item select events to navigate to the [VirtualCardDetailFragment].
     */
    private fun configureRecyclerView() {
        adapter =
            VirtualCardAdapter(orphanCardList) { virtualCard ->
                navController.navigate(
                    OrphanVirtualCardsFragmentDirections
                        .actionOrphanVirtualCardsFragmentToVirtualCardDetailFragment(
                            virtualCard
                        )
                )
            }
        binding.orphanVirtualCardRecyclerView.adapter = adapter
        binding.orphanVirtualCardRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    /** Set the visibility of the [emptyVirtualCardsLabel] in the view. */
    private fun setEmptyOrphanVirtualCardsLabel() {
        if (bindingDelegate.isAttached()) {
            if (orphanCardList.isEmpty()) {
                binding.emptyVirtualCardsLabel.visibility = View.VISIBLE
            } else {
                binding.emptyVirtualCardsLabel.visibility = View.GONE
            }
        }
    }

    /**
     * Sets recycler view to enabled/disabled.
     *
     * @param isEnabled If true, recycler view will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        binding.orphanVirtualCardRecyclerView.isEnabled = isEnabled
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int = 0) {
        if (textResId != 0) {
            binding.progressText.text = getString(textResId)
        }
        binding.progressBar.visibility = View.VISIBLE
        binding.progressText.visibility = View.VISIBLE
        setItemsEnabled(false)
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        if (bindingDelegate.isAttached()) {
            binding.progressBar.visibility = View.GONE
            binding.progressText.visibility = View.GONE
            setItemsEnabled(true)
        }
    }
}
