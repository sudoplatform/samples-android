/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.cards

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
import com.sudoplatform.sudovirtualcards.types.Card
import com.sudoplatform.virtualcardsexample.App
import com.sudoplatform.virtualcardsexample.R
import com.sudoplatform.virtualcardsexample.databinding.FragmentOrphanCardsBinding
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
 * This [OrphanCardsFragment] presents a list of orphan [Card]s which are associated with
 * deleted Sudos.
 *
 * - Links From:
 *  - [MainMenuFragment]: A user chooses the "Orphan Cards" option from the main menu which will show
 *   this view with the list of orphan cards. The orphan card's [Card.alias] property is used as the
 *   text for each card.
 *
 * - Links To:
 *  - [CardDetailFragment]: If a user chooses an orphan [Card] from the list, the [CardDetailFragment]
 *   will be presented so the user can view card details and transactions.
 */
class OrphanCardsFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentOrphanCardsBinding>()
    private val binding by bindingDelegate

    /** A reference to the [RecyclerView.Adapter] handling orphan [Card] data. */
    private lateinit var adapter: CardAdapter

    /** A mutable list of orphan [Card]s that are associated with a deleted [Sudo]'s identifier. */
    private var orphanCardList = mutableListOf<Card>()

    /** A mutable list of [Sudo]s to check for deleted [Sudo]s. */
    private var sudoList = mutableListOf<Sudo>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingDelegate.attach(FragmentOrphanCardsBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.orphan_cards)
        }
        app = requireActivity().application as App
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView()
        navController = Navigation.findNavController(view)

        listSudos(ListOption.REMOTE_ONLY)
        listOrphanCards(CachePolicy.REMOTE_ONLY)
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    /**
     * List orphan [Card]s from the [SudoVirtualCardsClient].
     *
     * @param cachePolicy Option of either retrieving [Card] data from the cache or network.
     */
    private fun listOrphanCards(cachePolicy: CachePolicy) {
        launch {
            try {
                showLoading()
                val orphanCards = withContext(Dispatchers.IO) {
                    app.sudoVirtualCardsClient.listCards(cachePolicy = cachePolicy)
                }
                orphanCardList.clear()
                val sudoIds = sudoList.map { it.id ?: "" }
                for (card in orphanCards.items) {
                    if (card.owners.all { !sudoIds.contains(it.id) }) {
                        orphanCardList.add(card)
                    }
                }
                setEmptyOrphanCardsLabel()
                adapter.notifyDataSetChanged()
            } catch (e: SudoVirtualCardsClient.CardException) {
                showAlertDialog(
                    titleResId = R.string.list_orphan_cards_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { listOrphanCards(CachePolicy.REMOTE_ONLY) },
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
     * Configures the [RecyclerView] used to display the listed orphan [Card] items and listens to
     * item select events to navigate to the [CardDetailFragment].
     */
    private fun configureRecyclerView() {
        adapter =
            CardAdapter(orphanCardList) { card ->
                navController.navigate(
                    OrphanCardsFragmentDirections.actionOrphanCardsFragmentToCardDetailFragment(
                        card
                    )
                )
            }
        binding.orphanCardRecyclerView.adapter = adapter
        binding.orphanCardRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    /** Set the visibility of the [emptyCardsLabel] in the view. */
    private fun setEmptyOrphanCardsLabel() {
        if (bindingDelegate.isAttached()) {
            if (orphanCardList.isEmpty()) {
                binding.emptyCardsLabel.visibility = View.VISIBLE
            } else {
                binding.emptyCardsLabel.visibility = View.GONE
            }
        }
    }

    /**
     * Sets recycler view to enabled/disabled.
     *
     * @param isEnabled If true, recycler view will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        binding.orphanCardRecyclerView.isEnabled = isEnabled
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
