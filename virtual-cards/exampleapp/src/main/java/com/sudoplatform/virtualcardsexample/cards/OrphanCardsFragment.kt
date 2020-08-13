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
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
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
import com.sudoplatform.virtualcardsexample.mainmenu.MainMenuFragment
import com.sudoplatform.virtualcardsexample.showAlertDialog
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_orphan_cards.*
import kotlinx.android.synthetic.main.fragment_orphan_cards.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    ): View? {
        val view = inflater.inflate(R.layout.fragment_orphan_cards, container, false)
        val toolbar = (view.toolbar as Toolbar)
        toolbar.title = getString(R.string.orphan_cards)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView(view)
        navController = Navigation.findNavController(view)

        listSudos(ListOption.REMOTE_ONLY)
        listOrphanCards(CachePolicy.REMOTE_ONLY)
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        super.onDestroy()
    }

    /**
     * List orphan [Card]s from the [SudoVirtualCardsClient].
     *
     * @param cachePolicy Option of either retrieving [Card] data from the cache or network.
     */
    private fun listOrphanCards(cachePolicy: CachePolicy) {
        val app = requireActivity().application as App
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
        val app = requireActivity().application as App
        launch {
            try {
                sudoList = withContext(Dispatchers.IO) {
                    app.sudoProfilesClient.listSudos(listOption)
                }.toMutableList()
            } catch (e: SudoProfileException) {
                showAlertDialog(
                    titleResId = R.string.list_sudos_failure,
                    message = e.localizedMessage,
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
    private fun configureRecyclerView(view: View) {
        adapter =
            CardAdapter(orphanCardList) { card ->
                val bundle = bundleOf(
                    getString(R.string.card) to card
                )
                navController.navigate(R.id.action_orphanCardsFragment_to_cardDetailFragment, bundle)
            }

        view.orphanCardRecyclerView.adapter = adapter
        view.orphanCardRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    /** Set the visibility of the [emptyCardsLabel] in the view. */
    private fun setEmptyOrphanCardsLabel() {
        if (orphanCardList.isEmpty()) {
            emptyCardsLabel?.visibility = View.VISIBLE
        } else {
            emptyCardsLabel?.visibility = View.GONE
        }
    }

    /**
     * Sets recycler view to enabled/disabled.
     *
     * @param isEnabled If true, recycler view will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        orphanCardRecyclerView?.isEnabled = isEnabled
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int = 0) {
        if (textResId != 0) {
            progressText.text = getString(textResId)
        }
        progressBar.visibility = View.VISIBLE
        progressText.visibility = View.VISIBLE
        setItemsEnabled(false)
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        progressBar?.visibility = View.GONE
        progressText?.visibility = View.GONE
        setItemsEnabled(true)
    }
}
