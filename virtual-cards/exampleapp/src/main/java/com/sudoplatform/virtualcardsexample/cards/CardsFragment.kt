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
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudovirtualcards.SudoVirtualCardsClient
import com.sudoplatform.sudovirtualcards.types.CachePolicy
import com.sudoplatform.sudovirtualcards.types.Card
import com.sudoplatform.virtualcardsexample.App
import com.sudoplatform.virtualcardsexample.R
import com.sudoplatform.virtualcardsexample.createLoadingAlertDialog
import com.sudoplatform.virtualcardsexample.databinding.FragmentCardsBinding
import com.sudoplatform.virtualcardsexample.showAlertDialog
import com.sudoplatform.virtualcardsexample.sudos.CreateSudoFragment
import com.sudoplatform.virtualcardsexample.sudos.SudosFragment
import com.sudoplatform.virtualcardsexample.swipe.SwipeLeftActionHelper
import com.sudoplatform.virtualcardsexample.util.ObjectDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

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

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentCardsBinding>()
    private val binding by bindingDelegate

    /** A reference to the [RecyclerView.Adapter] handling [Card] data. */
    private lateinit var adapter: CardAdapter

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** A mutable list of [Card]s. */
    private var cardList = mutableListOf<Card>()

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: CardsFragmentArgs by navArgs()

    /** A [Sudo] identifier used to filter [Card]s. */
    private lateinit var sudoId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingDelegate.attach(FragmentCardsBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.virtual_cards)
        }
        app = requireActivity().application as App
        sudoId = args.sudoId
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView()
        navController = Navigation.findNavController(view)
        val sudoLabel = args.sudoLabel

        binding.createCardButton.setOnClickListener {
            navController.navigate(
                CardsFragmentDirections.actionCardsFragmentToCreateCardFragment(sudoId, sudoLabel)
            )
        }

        listCards(CachePolicy.REMOTE_ONLY)
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    /**
     * List [Card]s from the [SudoVirtualCardsClient].
     *
     * @param cachePolicy Option of either retrieving [Card] data from the cache or network.
     */
    private fun listCards(cachePolicy: CachePolicy) {
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
        launch {
            try {
                showCancelAlert(R.string.cancelling_card)
                val card = withContext(Dispatchers.IO) {
                    app.sudoVirtualCardsClient.cancelCard(id)
                }
                completion(card)
                hideCancelAlert()
                showAlertDialog(
                    titleResId = R.string.success,
                    positiveButtonResId = android.R.string.ok
                )
            } catch (e: SudoVirtualCardsClient.CardException) {
                hideCancelAlert()
                showAlertDialog(
                    titleResId = R.string.cancel_card_failure,
                    message = e.localizedMessage ?: "$e",
                    negativeButtonResId = android.R.string.cancel
                )
            }
        }
    }

    /**
     * Configures the [RecyclerView] used to display the listed [Card] items and listens to item
     * select events to navigate to the [CardDetailFragment].
     */
    private fun configureRecyclerView() {
        adapter =
            CardAdapter(cardList) { card ->
                navController.navigate(
                    CardsFragmentDirections.actionCardsFragmentToCardDetailFragment(card)
                )
            }
        binding.cardRecyclerView.adapter = adapter
        binding.cardRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        configureSwipeToCancel()
    }

    /**
     * Sets buttons and recycler view to enabled/disabled.
     *
     * @param isEnabled If true, buttons and recycler view will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        binding.createCardButton.isEnabled = isEnabled
        binding.cardRecyclerView.isEnabled = isEnabled
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int = 0) {
        if (textResId != 0) {
            binding.progressText.text = getString(textResId)
        }
        binding.progressBar.visibility = View.VISIBLE
        binding.progressText.visibility = View.VISIBLE
        binding.cardRecyclerView.visibility = View.GONE
        setItemsEnabled(false)
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        if (bindingDelegate.isAttached()) {
            binding.progressBar.visibility = View.GONE
            binding.progressText.visibility = View.GONE
            binding.cardRecyclerView.visibility = View.VISIBLE
            setItemsEnabled(true)
        }
    }

    /** Displays the loading [AlertDialog] indicating that a cancel operation is occurring. */
    private fun showCancelAlert(@StringRes textResId: Int) {
        loading = createLoadingAlertDialog(textResId)
        loading?.show()
    }

    /** Dismisses the loading [AlertDialog] indicating that a cancel operation has finished. */
    private fun hideCancelAlert() {
        loading?.dismiss()
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
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.cardRecyclerView)
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
