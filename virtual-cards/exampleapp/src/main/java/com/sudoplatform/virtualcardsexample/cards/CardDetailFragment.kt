/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.cards

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudovirtualcards.SudoVirtualCardsClient
import com.sudoplatform.sudovirtualcards.subscription.TransactionSubscriber
import com.sudoplatform.sudovirtualcards.types.CachePolicy
import com.sudoplatform.sudovirtualcards.types.Card
import com.sudoplatform.sudovirtualcards.types.Transaction
import com.sudoplatform.sudovirtualcards.types.inputs.filters.filterTransactionsBy
import com.sudoplatform.virtualcardsexample.App
import com.sudoplatform.virtualcardsexample.R
import com.sudoplatform.virtualcardsexample.databinding.FragmentCardDetailBinding
import com.sudoplatform.virtualcardsexample.showAlertDialog
import com.sudoplatform.virtualcardsexample.transactions.TransactionAdapter
import com.sudoplatform.virtualcardsexample.transactions.transactiondetail.TransactionDetailFragment
import com.sudoplatform.virtualcardsexample.util.ObjectDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.CoroutineContext

/**
 * The [CardDetailFragment] presents a view containing [Card] details and a list of [Transaction]
 * created against the selected [Card].
 *
 * - Links From:
 *  - [CreateCardFragment]: A user chooses the "Create" option from the top right corner of the
 *   tool bar.
 *  - [CardsFragment]: A user chooses a [Card] from the list.
 *  - [OrphanCardsFragment]: A user chooses an orphan [Card] from the list.
 *
 * - Links To:
 *  - [TransactionDetailFragment]: If a user selects a [Transaction] from the list, the
 *   [TransactionDetailFragment] will be presented so that the user can view transaction details.
 */
class CardDetailFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentCardDetailBinding>()
    private val binding by bindingDelegate

    /** A reference to the [RecyclerView.Adapter] handling [Transaction] data. */
    private lateinit var adapter: TransactionAdapter

    /** A mutable list of [Transaction]s. */
    private var transactionList = mutableListOf<Transaction>()

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: CardDetailFragmentArgs by navArgs()

    /** The selected [Card] used for display. */
    private lateinit var card: Card

    /** Subscription ID for transactions */
    private val subscriptionId = UUID.randomUUID().toString()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingDelegate.attach(FragmentCardDetailBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.card_detail)
        }
        app = requireActivity().application as App
        card = args.card!!
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureCardView()
        configureRecyclerView()
        navController = Navigation.findNavController(view)

        listTransactions(CachePolicy.REMOTE_ONLY)
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        subscribeToTransactions()
    }

    override fun onPause() {
        unsubscribeFromTransactions()
        super.onPause()
    }

    /**
     * List [Transaction]s from the [SudoVirtualCardsClient].
     *
     * @param cachePolicy Option of either retrieving [Transaction] data from the cache or network.
     */
    private fun listTransactions(cachePolicy: CachePolicy) {
        launch {
            try {
                showLoading()
                val transactions = withContext(Dispatchers.IO) {
                    app.sudoVirtualCardsClient.listTransactions(
                        cachePolicy = cachePolicy,
                        filter = { filterTransactionsBy { cardId equalTo card.id } }
                    )
                }
                transactionList.clear()
                for (transaction in transactions.items) {
                    transactionList.add(transaction)
                }
                adapter.notifyDataSetChanged()
                setEmptyTransactionsLabel()
            } catch (e: SudoVirtualCardsClient.TransactionException) {
                showAlertDialog(
                    titleResId = R.string.list_transactions_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { listTransactions(CachePolicy.REMOTE_ONLY) },
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideLoading()
        }
    }

    /** Subscribe to receive live updates as [Transaction]s are created and updated. */
    private fun subscribeToTransactions() = launch {
        try {
            withContext(Dispatchers.IO) {
                app.sudoVirtualCardsClient.subscribeToTransactions(
                    id = subscriptionId,
                    subscriber = transactionSubscriber
                )
            }
        } catch (e: SudoVirtualCardsClient.TransactionException) {
            showAlertDialog(
                titleResId = R.string.subscribe_transactions_failure,
                message = e.localizedMessage ?: "$e",
                negativeButtonResId = android.R.string.ok,
                onNegative = {}
            )
        }
    }

    private val transactionSubscriber = object : TransactionSubscriber {
        override fun connectionStatusChanged(state: TransactionSubscriber.ConnectionState) {
            if (state == TransactionSubscriber.ConnectionState.DISCONNECTED) {
                launch(Dispatchers.Main) {
                    showAlertDialog(
                        titleResId = R.string.subscribe_transactions_failure,
                        messageResId = R.string.subscribe_lost_connection,
                        positiveButtonResId = android.R.string.ok,
                        onPositive = {}
                    )
                }
            }
        }

        override fun transactionChanged(transaction: Transaction) {
            launch(Dispatchers.Main) {
                addOrUpdateTransaction(transaction)
                adapter.notifyDataSetChanged()
                setEmptyTransactionsLabel()
            }
        }
    }

    /** Add to the list of transactions or replace an existing [Transaction]. */
    private fun addOrUpdateTransaction(newTransaction: Transaction) {
        val replaceAtIndex = transactionList.indexOfFirst { it.id == newTransaction.id }
        if (replaceAtIndex == -1) {
            transactionList.add(newTransaction)
        } else {
            transactionList[replaceAtIndex] = newTransaction
        }
    }

    /** Unsubscribe from live [Transaction] updates. */
    private fun unsubscribeFromTransactions() = launch {
        try {
            withContext(Dispatchers.IO) {
                app.sudoVirtualCardsClient.unsubscribeFromTransactions(subscriptionId)
            }
        } catch (e: SudoVirtualCardsClient.TransactionException) {
            app.logger.error("Failed to unsubscribe: $e")
        }
    }

    /**
     * Configures each field with the various card details on the graphical representation of a
     * virtual card.
     */
    private fun configureCardView() {
        val expirationMonthStr = card.expirationMonth.toString().padStart(2, '0')
        val expirationYearStr = (card.expirationYear % 100).toString()

        if (card.state == Card.State.CLOSED) {
            binding.stateView.visibility = View.VISIBLE
        } else {
            binding.stateView.visibility = View.GONE
        }
        binding.cardNameField.text = card.alias
        binding.accountNumberField.text = formatAccountNumber(card.cardNumber)
        binding.securityCodeField.text = card.securityCode
        binding.expiryDateField.text = getString(R.string.expiry_date_field, expirationMonthStr, expirationYearStr)
        binding.cardHolderNameField.text = card.cardHolder
    }

    /**
     * Formats the account number for display on the graphical representation of a virtual card.
     *
     * @param cardNumber The raw card number.
     * @return Formatted account number.
     */
    private fun formatAccountNumber(cardNumber: String): String {
        val chunkedCardNumber = cardNumber.chunked(4)
        return TextUtils.join(" ", chunkedCardNumber)
    }

    /**
     * Configures the [RecyclerView] used to display the listed [Transaction] items and listens
     * to item select events to navigate to the [TransactionDetailFragment].
     */
    private fun configureRecyclerView() {
        adapter =
            TransactionAdapter(transactionList) { transaction ->
                navController.navigate(
                    CardDetailFragmentDirections.actionCardDetailFragmentToTransactionDetailFragment(
                        card,
                        transaction
                    )
                )
            }
        binding.transactionRecyclerView.adapter = adapter
        binding.transactionRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    /** Set the visibility of the [emptyTxnsLabel] in the view. */
    private fun setEmptyTransactionsLabel() {
        if (bindingDelegate.isAttached()) {
            if (transactionList.isEmpty()) {
                binding.emptyTxnsLabel.visibility = View.VISIBLE
            } else {
                binding.emptyTxnsLabel.visibility = View.GONE
            }
        }
    }

    /**
     * Sets recycler view to enabled/disabled.
     *
     * @param isEnabled If true, recycler view will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        binding.transactionRecyclerView.isEnabled = isEnabled
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int = 0) {
        if (textResId != 0) {
            binding.progressText.text = getString(textResId)
        }
        binding.progressBar.visibility = View.VISIBLE
        binding.progressText.visibility = View.VISIBLE
        binding.transactionRecyclerView.visibility = View.GONE
        setItemsEnabled(false)
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        if (bindingDelegate.isAttached()) {
            binding.progressBar.visibility = View.GONE
            binding.progressText.visibility = View.GONE
            binding.transactionRecyclerView.visibility = View.VISIBLE
            setItemsEnabled(true)
        }
    }
}
