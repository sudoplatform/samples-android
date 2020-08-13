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
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudovirtualcards.SudoVirtualCardsClient
import com.sudoplatform.sudovirtualcards.subscription.TransactionSubscriber
import com.sudoplatform.sudovirtualcards.types.CachePolicy
import com.sudoplatform.sudovirtualcards.types.Card
import com.sudoplatform.sudovirtualcards.types.Transaction
import com.sudoplatform.sudovirtualcards.types.inputs.filters.filterTransactionsBy
import com.sudoplatform.virtualcardsexample.App
import com.sudoplatform.virtualcardsexample.MissingFragmentArgumentException
import com.sudoplatform.virtualcardsexample.R
import com.sudoplatform.virtualcardsexample.showAlertDialog
import com.sudoplatform.virtualcardsexample.transactions.TransactionAdapter
import com.sudoplatform.virtualcardsexample.transactions.transactiondetail.TransactionDetailFragment
import java.util.UUID
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_card_detail.*
import kotlinx.android.synthetic.main.fragment_card_detail.view.*
import kotlinx.android.synthetic.main.fragment_card_detail.view.toolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    /** A reference to the [RecyclerView.Adapter] handling [Transaction] data. */
    private lateinit var adapter: TransactionAdapter

    /** A mutable list of [Transaction]s. */
    private var transactionList = mutableListOf<Transaction>()

    /** The selected [Card] used for display. */
    private lateinit var card: Card

    /** Subscription ID for transactions */
    private val subscriptionId = UUID.randomUUID().toString()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_card_detail, container, false)
        val toolbar = (view.toolbar as Toolbar)
        toolbar.title = getString(R.string.card_detail)
        card = requireArguments().getParcelable(getString(R.string.card))
            ?: throw MissingFragmentArgumentException("Card to display missing")
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureCardView()
        configureRecyclerView(view)
        navController = Navigation.findNavController(view)

        listTransactions(CachePolicy.REMOTE_ONLY)
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
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
        val app = requireActivity().application as App
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
        val app = requireActivity().application as App
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
        val app = requireActivity().application as App
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
            stateView.visibility = View.VISIBLE
        } else {
            stateView.visibility = View.GONE
        }
        cardNameField.text = card.alias
        accountNumberField.text = formatAccountNumber(card.cardNumber)
        securityCodeField.text = card.securityCode
        expiryDateField.text = getString(R.string.expiry_date_field, expirationMonthStr, expirationYearStr)
        cardHolderNameField.text = card.cardHolder
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
    private fun configureRecyclerView(view: View) {
        adapter =
            TransactionAdapter(transactionList) { transaction ->
                val bundle = bundleOf(
                    getString(R.string.transaction) to transaction,
                    getString(R.string.card) to card
                )
                navController.navigate(R.id.action_cardDetailFragment_to_transactionDetailFragment, bundle)
            }

        view.transactionRecyclerView.adapter = adapter
        view.transactionRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    /** Set the visibility of the [emptyTxnsLabel] in the view. */
    private fun setEmptyTransactionsLabel() {
        if (transactionList.isEmpty()) {
            emptyTxnsLabel?.visibility = View.VISIBLE
        } else {
            emptyTxnsLabel?.visibility = View.GONE
        }
    }

    /**
     * Sets recycler view to enabled/disabled.
     *
     * @param isEnabled If true, recycler view will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        transactionRecyclerView?.isEnabled = isEnabled
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int = 0) {
        if (textResId != 0) {
            progressText.text = getString(textResId)
        }
        progressBar.visibility = View.VISIBLE
        progressText.visibility = View.VISIBLE
        transactionRecyclerView?.visibility = View.GONE
        setItemsEnabled(false)
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        progressBar?.visibility = View.GONE
        progressText?.visibility = View.GONE
        transactionRecyclerView?.visibility = View.VISIBLE
        setItemsEnabled(true)
    }
}
