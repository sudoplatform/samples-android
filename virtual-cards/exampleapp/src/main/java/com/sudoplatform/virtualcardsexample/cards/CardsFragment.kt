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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudovirtualcards.SudoVirtualCardsClient
import com.sudoplatform.sudovirtualcards.types.CachePolicy
import com.sudoplatform.sudovirtualcards.types.Card
import com.sudoplatform.virtualcardsexample.App
import com.sudoplatform.virtualcardsexample.MissingFragmentArgumentException
import com.sudoplatform.virtualcardsexample.R
import com.sudoplatform.virtualcardsexample.createLoadingAlertDialog
import com.sudoplatform.virtualcardsexample.showAlertDialog
import com.sudoplatform.virtualcardsexample.sudos.CreateSudoFragment
import com.sudoplatform.virtualcardsexample.sudos.SudosFragment
import com.sudoplatform.virtualcardsexample.swipe.SwipeLeftActionHelper
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_cards.*
import kotlinx.android.synthetic.main.fragment_cards.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This [CardsFragment] presents a list of [Card]s.
 *
 * - Links From:
 *  - [CreateSudoFragment]: A user chooses the "Create" option from the top right corner of the toolbar.
 *  - [SudosFragment]: A user selects a [Sudo] from the list which will show this view with the list of
 *   [Card]s created against this [Sudo]. The card's alias property is used as the text for each card.
 *
 * - Links To:
 *  - [CreateCardFragment]: If a user taps the "Create Virtual Card" button, the [CreateCardFragment]
 *   will be presented so that the user can add a new [Card] to their [Sudo].
 *  - [CardDetailFragment]: If a user selects a [Card] from the list, the [CardDetailFragment] will
 *   be presented so that the user can view card details and transactions.
 */
class CardsFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** A reference to the [RecyclerView.Adapter] handling [Card] data. */
    private lateinit var adapter: CardAdapter

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private lateinit var loading: AlertDialog

    /** A mutable list of [Card]s. */
    private var cardList = mutableListOf<Card>()

    /** A [Sudo] identifier used to filter [Card]s. */
    private lateinit var sudoId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_cards, container, false)
        val toolbar = (view.toolbar as Toolbar)
        toolbar.title = getString(R.string.virtual_cards)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView(view)
        navController = Navigation.findNavController(view)
        sudoId = requireArguments().getString(getString(R.string.sudo_id))
            ?: throw MissingFragmentArgumentException("Sudo identifier missing")
        val sudoLabel = requireArguments().getString(getString(R.string.sudo_label))
            ?: throw MissingFragmentArgumentException("Sudo label missing")

        view.createCardButton.setOnClickListener {
            val bundle = bundleOf(
                getString(R.string.sudo_id) to sudoId,
                getString(R.string.sudo_label) to sudoLabel
            )
            navController.navigate(R.id.action_cardsFragment_to_createCardFragment, bundle)
        }

        listCards(CachePolicy.REMOTE_ONLY)
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        super.onDestroy()
    }

    /**
     * List [Card]s from the [SudoVirtualCardsClient].
     *
     * @param cachePolicy Option of either retrieving [Card] data from the cache or network.
     */
    private fun listCards(cachePolicy: CachePolicy) {
        val app = requireActivity().application as App
        launch {
            try {
                showLoading()
                val cards = withContext(Dispatchers.IO) {
                    app.sudoVirtualCardsClient.listCards(cachePolicy = cachePolicy)
                }
                cardList.clear()
                for (card in cards.items) {
                    if (card.owners.all { it.id == sudoId }) {
                        cardList.add(card)
                    }
                }
                adapter.notifyDataSetChanged()
            } catch (e: SudoVirtualCardsClient.CardException) {
                showAlertDialog(
                    titleResId = R.string.list_cards_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { listCards(CachePolicy.REMOTE_ONLY) },
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideLoading()
        }
    }

    /**
     * Cancel a [Card] from the [SudoVirtualCardsClient] based on the input [id].
     *
     * @param id The identifier of the [Card] to cancel.
     * @param completion Callback which executes when the operation is completed.
     */
    private fun cancelCard(id: String, completion: (Card) -> Unit) {
        val app = requireActivity().application as App
        launch {
            try {
                showCancelAlert(R.string.cancelling_card)
                val card = withContext(Dispatchers.IO) {
                    app.sudoVirtualCardsClient.cancelCard(id)
                }
                completion(card)
                showAlertDialog(
                    titleResId = R.string.success,
                    positiveButtonResId = android.R.string.ok
                )
            } catch (e: SudoVirtualCardsClient.CardException) {
                showAlertDialog(
                    titleResId = R.string.cancel_card_failure,
                    message = e.localizedMessage ?: "$e",
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideCancelAlert()
        }
    }

    /**
     * Configures the [RecyclerView] used to display the listed [Card] items and listens to item
     * select events to navigate to the [CardDetailFragment].
     */
    private fun configureRecyclerView(view: View) {
        adapter =
            CardAdapter(cardList) { card ->
                val bundle = bundleOf(
                    getString(R.string.card) to card
                )
                navController.navigate(R.id.action_cardsFragment_to_cardDetailFragment, bundle)
            }

        view.cardRecyclerView.adapter = adapter
        view.cardRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        configureSwipeToCancel()
    }

    /**
     * Sets buttons and recycler view to enabled/disabled.
     *
     * @param isEnabled If true, buttons and recycler view will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        createCardButton?.isEnabled = isEnabled
        cardRecyclerView?.isEnabled = isEnabled
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int = 0) {
        if (textResId != 0) {
            progressText.text = getString(textResId)
        }
        progressBar.visibility = View.VISIBLE
        progressText.visibility = View.VISIBLE
        cardRecyclerView?.visibility = View.GONE
        setItemsEnabled(false)
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        progressBar?.visibility = View.GONE
        progressText?.visibility = View.GONE
        cardRecyclerView?.visibility = View.VISIBLE
        setItemsEnabled(true)
    }

    /** Displays the loading [AlertDialog] indicating that a cancel operation is occurring. */
    private fun showCancelAlert(@StringRes textResId: Int) {
        loading = createLoadingAlertDialog(textResId)
        loading.show()
    }

    /** Dismisses the loading [AlertDialog] indicating that a cancel operation has finished. */
    private fun hideCancelAlert() {
        loading.dismiss()
    }

    /**
     * Configures the swipe to cancel action by listening to [RecyclerView.ViewHolder] swipe events
     * and drawing the swipe view and cancel icon.
     *
     * Swiping in from the left will perform a cancel operation but will not remove the item from
     * the view. Instead the item is amended with a "Cancelled" label.
     */
    private fun configureSwipeToCancel() {
        val itemTouchCallback = SwipeLeftActionHelper(requireContext(), onSwipedAction = ::onSwiped)
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(cardRecyclerView)
    }

    private fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val card = cardList[viewHolder.adapterPosition]
        cancelCard(card.id) { cancelledCard ->
            val position = viewHolder.adapterPosition
            cardList.removeAt(position)
            adapter.notifyItemRemoved(position)
            cardList.add(position, cancelledCard)
            adapter.notifyItemInserted(position)
        }
    }
}
