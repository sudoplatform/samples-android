/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.transactions.transactiondetail

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudoprofiles.ListOption
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import com.sudoplatform.sudoprofiles.exceptions.SudoProfileException
import com.sudoplatform.sudovirtualcards.SudoVirtualCardsClient
import com.sudoplatform.sudovirtualcards.types.CachePolicy
import com.sudoplatform.sudovirtualcards.types.Card
import com.sudoplatform.sudovirtualcards.types.CurrencyAmount
import com.sudoplatform.sudovirtualcards.types.FundingSource
import com.sudoplatform.sudovirtualcards.types.Transaction
import com.sudoplatform.virtualcardsexample.App
import com.sudoplatform.virtualcardsexample.R
import com.sudoplatform.virtualcardsexample.cards.CardDetailFragment
import com.sudoplatform.virtualcardsexample.showAlertDialog
import java.util.Date
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_transaction_detail.*
import kotlinx.android.synthetic.main.fragment_transaction_detail.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This [TransactionDetailFragment] presents a list of transaction details.
 *
 * - Links From:
 *  - [CardDetailFragment]: A user selects a [Transaction] from the list which will show this view
 *    with the list of transaction details.
 */
class TransactionDetailFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** A reference to the [RecyclerView.Adapter] handling transaction detail data. */
    private lateinit var adapter: TransactionDetailAdapter

    /** A mutable list of [TransactionDetailCell]s which hold [Transaction] detail information. */
    private val transactionDetailCells = mutableListOf<TransactionDetailCell>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_transaction_detail, container, false)
        val toolbar = (view.toolbar as Toolbar)
        toolbar.title = getString(R.string.transaction_detail)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val transaction: Transaction = requireArguments().getParcelable(getString(R.string.transaction))!!
        val card: Card = requireArguments().getParcelable(getString(R.string.card))!!
        configureRecyclerView(view)
        configureTransactionDetailCells(transaction)
        configureAccountDetails(card)
    }

    /**
     * Retrieve the [Sudo] from the [SudoProfilesClient] associated with the selected [Card].
     *
     * @param card The selected [Card].
     */
    private fun retrieveAssociatedSudo(card: Card) {
        val app = requireActivity().application as App
        launch {
            try {
                val sudoList = withContext(Dispatchers.IO) {
                    app.sudoProfilesClient.listSudos(ListOption.REMOTE_ONLY)
                }.toMutableList()
                val sudo = sudoList.firstOrNull { sudo -> card.owners.all { it.id == sudo.id } }
                if (sudo != null) {
                    sudoLabel?.text = sudo.label
                }
            } catch (e: SudoProfileException) {
                showAlertDialog(
                    titleResId = R.string.list_sudos_failure,
                    message = e.localizedMessage ?: e.toString(),
                    negativeButtonResId = android.R.string.cancel
                )
            }
        }
    }

    /**
     * Retrieve the [FundingSource] from the [SudoVirtualCardsClient] used to fund the selected [Card].
     *
     * @param card The selected [Card].
     */
    private fun retrieveAssociatedFundingSource(card: Card) {
        val app = requireActivity().application as App
        launch {
            try {
                val fundingSource = withContext(Dispatchers.IO) {
                    app.sudoVirtualCardsClient.getFundingSource(card.fundingSourceId, cachePolicy = CachePolicy.REMOTE_ONLY)
                }
                if (fundingSource != null) {
                    fundingSourceLabel?.text = getString(R.string.funding_source_label, fundingSource.network, fundingSource.last4)
                }
            } catch (e: SudoVirtualCardsClient.FundingSourceException) {
                showAlertDialog(
                    titleResId = R.string.get_funding_source_failure,
                    message = e.localizedMessage ?: "$e",
                    negativeButtonResId = android.R.string.cancel
                )
            }
        }
    }

    /** Configures the [RecyclerView] used to display the [TransactionDetailCell]s. */
    private fun configureRecyclerView(view: View) {
        adapter = TransactionDetailAdapter(transactionDetailCells)
        view.transactionDetail_recyclerView.adapter = adapter
        view.transactionDetail_recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    /**
     * Configures the [TransactionDetailCell] titles, subtitles and value text.
     *
     * @param transaction The selected [Transaction] to display its details.
     */
    private fun configureTransactionDetailCells(transaction: Transaction) {
        val merchantCell = TransactionDetailCell(getString(R.string.merchant), "", transaction.description)
        val statusCell = TransactionDetailCell(getString(R.string.status), "", transaction.type.name)
        if (transaction.type == Transaction.Type.DECLINE) {
            val declineReasonCell = TransactionDetailCell(getString(R.string.decline_reason), "", transaction.declineReason?.description(requireContext()) ?: getString(R.string.dr_declined))
            transactionDetailCells.add(declineReasonCell)
        }
        transactionDetailCells.addAll(arrayListOf(merchantCell, statusCell))
        configureTransactionDateCells(transaction)
        val transactionDetail = transaction.details.first()
        val amountCell = TransactionDetailCell(getString(R.string.amount), "", formatCurrencyAmount(transactionDetail.virtualCardAmount))
        transactionDetailCells.add(amountCell)
        when (transaction.type) {
            Transaction.Type.PENDING, Transaction.Type.COMPLETE -> {
                val feePercentStr = "%.2f%%".format((transactionDetail.markup.percent / 1000.0))
                val feeFlatStr = "$%.2f".format((transactionDetail.markup.flat / 100.0))
                val serviceFeeSubtitle = "$feePercentStr + $feeFlatStr"
                val serviceFeeCell = TransactionDetailCell(getString(R.string.service_fee), serviceFeeSubtitle, formatCurrencyAmount(transactionDetail.markupAmount))
                val totalFeeCell = TransactionDetailCell(getString(R.string.total), "", formatCurrencyAmount(transactionDetail.fundingSourceAmount))
                transactionDetailCells.addAll(arrayListOf(serviceFeeCell, totalFeeCell))
            }
            else -> {}
        }
    }

    /**
     * Configures the transaction detail date labels and value text.
     *
     * @param transaction The selected [Transaction] to display its details.
     */
    private fun configureTransactionDateCells(transaction: Transaction) {
        when (transaction.type) {
            Transaction.Type.PENDING -> {
                val dateChargedCell = TransactionDetailCell(getString(R.string.date_charged), "", formatDate(transaction.transactedAt))
                transactionDetailCells.add(dateChargedCell)
            }
            Transaction.Type.COMPLETE -> {
                val dateSettledCell = TransactionDetailCell(getString(R.string.date_settled), "", formatDate(transaction.transactedAt))
                transactionDetailCells.add(dateSettledCell)
            }
            Transaction.Type.REFUND -> {
                val dateRefundedCell = TransactionDetailCell(getString(R.string.date_refunded), "", formatDate(transaction.transactedAt))
                transactionDetailCells.add(dateRefundedCell)
            }
            Transaction.Type.DECLINE -> {
                val dateDeclinedCell = TransactionDetailCell(getString(R.string.date_declined), "", formatDate(transaction.transactedAt))
                transactionDetailCells.add(dateDeclinedCell)
            }
            else -> {}
        }
    }

    /**
     * Configures the labels and value text of [Sudo], [FundingSource] and [Card] details.
     *
     * @param card The selected [Card] to display its associated account details.
     */
    private fun configureAccountDetails(card: Card) {
        cardLabel?.text = card.alias
        retrieveAssociatedFundingSource(card)
        retrieveAssociatedSudo(card)
    }

    /**
     * Formats a [CurrencyAmount] value to a presentable [String].
     *
     * @param value The [CurrencyAmount] to be formatted.
     * @return A presentable [String] containing the currency amount value.
     */
    private fun formatCurrencyAmount(value: CurrencyAmount): String {
        val doubleVal = value.amount.toDouble() / 100.0
        return "$%.2f".format(doubleVal)
    }

    /**
     * Formats a [Date] to a presentable String.
     *
     * @param date The [Date] to be formatted.
     * @return A presentable [String] containing the date.
     */
    private fun formatDate(date: Date): String {
        return DateFormat.format("MMM, dd yyyy", date).toString()
    }
}
