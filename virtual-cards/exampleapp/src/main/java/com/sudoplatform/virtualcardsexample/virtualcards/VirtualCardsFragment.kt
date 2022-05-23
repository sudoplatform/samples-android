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
import com.sudoplatform.sudovirtualcards.types.ListAPIResult
import com.sudoplatform.sudovirtualcards.types.SingleAPIResult
import com.sudoplatform.sudovirtualcards.types.VirtualCard
import com.sudoplatform.virtualcardsexample.App
import com.sudoplatform.virtualcardsexample.R
import com.sudoplatform.virtualcardsexample.createLoadingAlertDialog
import com.sudoplatform.virtualcardsexample.databinding.FragmentVirtualCardsBinding
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
 * This [VirtualCardsFragment] presents a list of [VirtualCard]s.
 *
 * - Links From:
 *  - [CreateSudoFragment]: A user chooses the "Create" option from the top right corner of the toolbar.
 *  - [SudosFragment]: A user selects a [Sudo] from the list which will show this view with the list of
 *   [VirtualCard]s created against this [Sudo]. The [VirtualCard.metadata] property is used as the text
 *   for each virtual card.
 *
 * - Links To:
 *  - [CreateVirtualCardFragment]: If a user taps the "Create Virtual Card" button, the [CreateVirtualCardFragment]
 *   will be presented so that the user can add a new [VirtualCard] to their [Sudo].
 *  - [VirtualCardDetailFragment]: If a user selects a [VirtualCard] from the list, the [VirtualCardDetailFragment]
 *   will be presented so that the user can view virtual card details and transactions.
 */
class VirtualCardsFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentVirtualCardsBinding>()
    private val binding by bindingDelegate

    /** A reference to the [RecyclerView.Adapter] handling [VirtualCard] data. */
    private lateinit var adapter: VirtualCardAdapter

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** A mutable list of [VirtualCard]s. */
    private var virtualCardList = mutableListOf<VirtualCard>()

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: VirtualCardsFragmentArgs by navArgs()

    /** A [Sudo] used to filter [VirtualCard]s. */
    private lateinit var sudo: Sudo

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingDelegate.attach(FragmentVirtualCardsBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.virtual_cards)
        }
        app = requireActivity().application as App
        sudo = args.sudo!!
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView()
        navController = Navigation.findNavController(view)

        binding.createVirtualCardButton.setOnClickListener {
            navController.navigate(
                VirtualCardsFragmentDirections.actionVirtualCardsFragmentToCreateVirtualCardFragment(sudo)
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
     * List [VirtualCard]s from the [SudoVirtualCardsClient].
     *
     * @param cachePolicy Option of either retrieving [VirtualCard] data from the cache or network.
     */
    private fun listCards(cachePolicy: CachePolicy) {
        launch {
            try {
                showLoading()
                val virtualCards = withContext(Dispatchers.IO) {
                    app.sudoVirtualCardsClient.listVirtualCards(cachePolicy = cachePolicy)
                }
                when (virtualCards) {
                    is ListAPIResult.Success -> {
                        virtualCardList.clear()
                        for (card in virtualCards.result.items) {
                            if (card.owners.all { it.id == sudo.id }) {
                                virtualCardList.add(card)
                            }
                        }
                        adapter.notifyDataSetChanged()
                    }
                    is ListAPIResult.Partial -> {
                        val cause = virtualCards.result.failed.first().cause
                        showAlertDialog(
                            titleResId = R.string.list_virtual_cards_failure,
                            message = cause.localizedMessage ?: "$cause",
                            positiveButtonResId = R.string.try_again,
                            onPositive = { listCards(CachePolicy.REMOTE_ONLY) },
                            negativeButtonResId = android.R.string.cancel
                        )
                    }
                }
            } catch (e: SudoVirtualCardsClient.VirtualCardException) {
                showAlertDialog(
                    titleResId = R.string.list_virtual_cards_failure,
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
     * Cancel a [VirtualCard] from the [SudoVirtualCardsClient] based on the input [id].
     *
     * @param id The identifier of the [VirtualCard] to cancel.
     * @param completion Callback which executes when the operation is completed.
     */
    private fun cancelVirtualCard(id: String, completion: (VirtualCard) -> Unit) {
        launch {
            try {
                showCancelAlert(R.string.cancelling_virtual_card)
                val virtualCard = withContext(Dispatchers.IO) {
                    app.sudoVirtualCardsClient.cancelVirtualCard(id)
                }
                when (virtualCard) {
                    is SingleAPIResult.Success -> {
                        completion(virtualCard.result)
                        hideCancelAlert()
                        showAlertDialog(
                            titleResId = R.string.success,
                            positiveButtonResId = android.R.string.ok
                        )
                    }
                    is SingleAPIResult.Partial -> {
                        val cause = virtualCard.result.cause
                        hideCancelAlert()
                        showAlertDialog(
                            titleResId = R.string.cancel_virtual_card_failure,
                            message = cause.localizedMessage ?: "$cause",
                            negativeButtonResId = android.R.string.cancel
                        )
                    }
                }
            } catch (e: SudoVirtualCardsClient.VirtualCardException) {
                hideCancelAlert()
                showAlertDialog(
                    titleResId = R.string.cancel_virtual_card_failure,
                    message = e.localizedMessage ?: "$e",
                    negativeButtonResId = android.R.string.cancel
                )
            }
        }
    }

    /**
     * Configures the [RecyclerView] used to display the listed [VirtualCard] items and listens to
     * item select events to navigate to the [VirtualCardDetailFragment].
     */
    private fun configureRecyclerView() {
        adapter =
            VirtualCardAdapter(virtualCardList) { virtualCard ->
                navController.navigate(
                    VirtualCardsFragmentDirections
                        .actionVirtualCardsFragmentToVirtualCardDetailFragment(virtualCard)
                )
            }
        binding.virtualCardRecyclerView.adapter = adapter
        binding.virtualCardRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        configureSwipeToCancel()
    }

    /**
     * Sets buttons and recycler view to enabled/disabled.
     *
     * @param isEnabled If true, buttons and recycler view will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        binding.createVirtualCardButton.isEnabled = isEnabled
        binding.virtualCardRecyclerView.isEnabled = isEnabled
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int = 0) {
        if (textResId != 0) {
            binding.progressText.text = getString(textResId)
        }
        binding.progressBar.visibility = View.VISIBLE
        binding.progressText.visibility = View.VISIBLE
        binding.virtualCardRecyclerView.visibility = View.GONE
        setItemsEnabled(false)
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        if (bindingDelegate.isAttached()) {
            binding.progressBar.visibility = View.GONE
            binding.progressText.visibility = View.GONE
            binding.virtualCardRecyclerView.visibility = View.VISIBLE
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
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.virtualCardRecyclerView)
    }

    private fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val card = virtualCardList[viewHolder.adapterPosition]
        cancelVirtualCard(card.id) { cancelledCard ->
            val position = viewHolder.adapterPosition
            virtualCardList.removeAt(position)
            adapter.notifyItemRemoved(position)
            virtualCardList.add(position, cancelledCard)
            adapter.notifyItemInserted(position)
        }
    }
}
