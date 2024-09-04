/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.virtualcards

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
import com.sudoplatform.sudovirtualcards.types.CardState
import com.sudoplatform.sudovirtualcards.types.ListAPIResult
import com.sudoplatform.sudovirtualcards.types.Transaction
import com.sudoplatform.sudovirtualcards.types.VirtualCard
import com.sudoplatform.virtualcardsexample.App
import com.sudoplatform.virtualcardsexample.R
import com.sudoplatform.virtualcardsexample.databinding.FragmentVirtualCardDetailBinding
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
 * The [VirtualCardDetailFragment] presents a view containing [VirtualCard] details and a list of
 * [Transaction]s created against the selected [VirtualCard].
 *
 * - Links From:
 *  - [CreateVirtualCardFragment]: A user chooses the "Create" option from the top right corner of
 *   the tool bar.
 *  - [VirtualCardsFragment]: A user chooses a [VirtualCard] from the list.
 *  - [OrphanVirtualCardsFragment]: A user chooses an orphan [VirtualCard] from the list.
 *
 * - Links To:
 *  - [TransactionDetailFragment]: If a user selects a [Transaction] from the list, the
 *   [TransactionDetailFragment] will be presented so that the user can view transaction details.
 */
class VirtualCardDetailFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentVirtualCardDetailBinding>()
    private val binding by bindingDelegate

    /** A reference to the [RecyclerView.Adapter] handling [Transaction] data. */
    private lateinit var adapter: TransactionAdapter

    /** A mutable list of [Transaction]s. */
    private var transactionList = mutableListOf<Transaction>()

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: VirtualCardDetailFragmentArgs by navArgs()

    /** The selected [VirtualCard] used for display. */
    private lateinit var virtualCard: VirtualCard

    /** Subscription ID for transactions */
    private val subscriptionId = UUID.randomUUID().toString()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingDelegate.attach(FragmentVirtualCardDetailBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.virtual_card_detail)
        }
        app = requireActivity().application as App
        virtualCard = args.virtualCard!!
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureVirtualCardView()
        configureRecyclerView()
        navController = Navigation.findNavController(view)

        listTransactions()
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

     */
    private fun listTransactions() {
        launch {
            try {
                showLoading()
                val transactions = withContext(Dispatchers.IO) {
                    app.sudoVirtualCardsClient.listTransactionsByCardId(
                        cardId = virtualCard.id,
                    )
                }
                when (transactions) {
                    is ListAPIResult.Success -> {
                        transactionList.clear()
                        for (transaction in transactions.result.items) {
                            transactionList.add(transaction)
                        }
                        adapter.notifyDataSetChanged()
                        setEmptyTransactionsLabel()
                    }
                    is ListAPIResult.Partial -> {
                        val cause = transactions.result.failed.first().cause
                        showAlertDialog(
                            titleResId = R.string.list_transactions_failure,
                            message = cause.localizedMessage ?: "$cause",
                            positiveButtonResId = R.string.try_again,
                            onPositive = { listTransactions() },
                            negativeButtonResId = android.R.string.cancel,
                        )
                    }
                }
            } catch (e: SudoVirtualCardsClient.TransactionException) {
                showAlertDialog(
                    titleResId = R.string.list_transactions_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { listTransactions() },
                    negativeButtonResId = android.R.string.cancel,
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
                    subscriber = transactionSubscriber,
                )
            }
        } catch (e: SudoVirtualCardsClient.TransactionException) {
            showAlertDialog(
                titleResId = R.string.subscribe_transactions_failure,
                message = e.localizedMessage ?: "$e",
                negativeButtonResId = android.R.string.ok,
                onNegative = {},
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
                        onPositive = {},
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
     * Configures each field with the various virtual card details on the graphical representation
     * of a virtual card.
     */
    private fun configureVirtualCardView() {
        val expirationMonthStr = virtualCard.expiry.mm.padStart(2, '0')
        val expirationYearStr = (virtualCard.expiry.yyyy.toInt() % 100).toString()

        if (virtualCard.state == CardState.CLOSED) {
            binding.stateView.visibility = View.VISIBLE
        } else {
            binding.stateView.visibility = View.GONE
        }
        binding.cardNameField.text = virtualCard.metadata?.unwrap().toString()
        binding.accountNumberField.text = formatAccountNumber(virtualCard.cardNumber)
        binding.securityCodeField.text = virtualCard.securityCode
        binding.expiryDateField.text = getString(R.string.expiry_date_field, expirationMonthStr, expirationYearStr)
        binding.cardHolderNameField.text = virtualCard.cardHolder
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
                    VirtualCardDetailFragmentDirections.actionVirtualCardDetailFragmentToTransactionDetailFragment(
                        virtualCard,
                        transaction,
                    ),
                )
            }
        binding.transactionRecyclerView.adapter = adapter
        binding.transactionRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    /** Set the visibility of the empty transactions label in the view. */
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
