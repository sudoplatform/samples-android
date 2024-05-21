/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.transactions.transactiondetail

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudoprofiles.ListOption
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import com.sudoplatform.sudoprofiles.exceptions.SudoProfileException
import com.sudoplatform.sudovirtualcards.SudoVirtualCardsClient
import com.sudoplatform.sudovirtualcards.extensions.isUnfunded
import com.sudoplatform.sudovirtualcards.types.BankAccountFundingSource
import com.sudoplatform.sudovirtualcards.types.CachePolicy
import com.sudoplatform.sudovirtualcards.types.CreditCardFundingSource
import com.sudoplatform.sudovirtualcards.types.CurrencyAmount
import com.sudoplatform.sudovirtualcards.types.FundingSource
import com.sudoplatform.sudovirtualcards.types.Transaction
import com.sudoplatform.sudovirtualcards.types.TransactionType
import com.sudoplatform.sudovirtualcards.types.VirtualCard
import com.sudoplatform.virtualcardsexample.App
import com.sudoplatform.virtualcardsexample.R
import com.sudoplatform.virtualcardsexample.databinding.FragmentTransactionDetailBinding
import com.sudoplatform.virtualcardsexample.showAlertDialog
import com.sudoplatform.virtualcardsexample.util.ObjectDelegate
import com.sudoplatform.virtualcardsexample.virtualcards.VirtualCardDetailFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import kotlin.coroutines.CoroutineContext

/**
 * This [TransactionDetailFragment] presents a list of transaction details.
 *
 * - Links From:
 *  - [VirtualCardDetailFragment]: A user selects a [Transaction] from the list which will show this
 *    view with the list of transaction details.
 */
class TransactionDetailFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentTransactionDetailBinding>()
    private val binding by bindingDelegate

    /** A reference to the [RecyclerView.Adapter] handling transaction detail data. */
    private lateinit var adapter: TransactionDetailAdapter

    /** A mutable list of [TransactionDetailCell]s which hold [Transaction] detail information. */
    private val transactionDetailCells = mutableListOf<TransactionDetailCell>()

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: TransactionDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingDelegate.attach(FragmentTransactionDetailBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.transaction_detail)
        }
        app = requireActivity().application as App
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val transaction = args.transaction!!
        val card = args.virtualCard!!
        configureRecyclerView()
        configureTransactionDetailCells(transaction)
        configureAccountDetails(card)
    }

    /**
     * Retrieve the [Sudo] from the [SudoProfilesClient] associated with the selected [VirtualCard].
     *
     * @param virtualCard The selected [VirtualCard].
     */
    private fun retrieveAssociatedSudo(virtualCard: VirtualCard) {
        launch {
            try {
                val sudoList = withContext(Dispatchers.IO) {
                    app.sudoProfilesClient.listSudos(ListOption.REMOTE_ONLY)
                }.toMutableList()
                val sudo = sudoList.firstOrNull { sudo -> virtualCard.owners.all { it.id == sudo.id } }
                if (sudo != null) {
                    binding.sudoLabel.text = sudo.label
                }
            } catch (e: SudoProfileException) {
                showAlertDialog(
                    titleResId = R.string.list_sudos_failure,
                    message = e.localizedMessage ?: e.toString(),
                    negativeButtonResId = android.R.string.cancel,
                )
            }
        }
    }

    /**
     * Retrieve the [FundingSource] from the [SudoVirtualCardsClient] used to fund the selected
     * [VirtualCard].
     *
     * @param virtualCard The selected [VirtualCard].
     */
    private fun retrieveAssociatedFundingSource(virtualCard: VirtualCard) {
        launch {
            try {
                val fundingSource = withContext(Dispatchers.IO) {
                    app.sudoVirtualCardsClient.getFundingSource(virtualCard.fundingSourceId, cachePolicy = CachePolicy.REMOTE_ONLY)
                }
                when (fundingSource) {
                    is CreditCardFundingSource -> {
                        binding.fundingSourceLabel.text = getString(R.string.funding_source_credit_card_description, fundingSource.last4, fundingSource.network)
                    }
                    is BankAccountFundingSource -> {
                        val unfundedSuffix = if (fundingSource.isUnfunded()) " ***UNFUNDED***" else ""
                        binding.fundingSourceLabel.text = getString(R.string.funding_source_bank_account_description, fundingSource.last4, fundingSource.bankAccountType, unfundedSuffix)
                    }
                    else -> { /* Nothing to do here */ }
                }
            } catch (e: SudoVirtualCardsClient.FundingSourceException) {
                showAlertDialog(
                    titleResId = R.string.get_funding_source_failure,
                    message = e.localizedMessage ?: "$e",
                    negativeButtonResId = android.R.string.cancel,
                )
            }
        }
    }

    /** Configures the [RecyclerView] used to display the [TransactionDetailCell]s. */
    private fun configureRecyclerView() {
        adapter = TransactionDetailAdapter(transactionDetailCells)
        binding.transactionDetailRecyclerView.adapter = adapter
        binding.transactionDetailRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    /**
     * Configures the [TransactionDetailCell] titles, subtitles and value text.
     *
     * @param transaction The selected [Transaction] to display its details.
     */
    private fun configureTransactionDetailCells(transaction: Transaction) {
        val merchantCell = TransactionDetailCell(getString(R.string.merchant), "", transaction.description)
        val statusCell = TransactionDetailCell(getString(R.string.status), "", transaction.type.name)
        if (transaction.type == TransactionType.DECLINE) {
            val declineReasonCell = TransactionDetailCell(getString(R.string.decline_reason), "", transaction.declineReason?.description(requireContext()) ?: getString(R.string.dr_declined))
            transactionDetailCells.add(declineReasonCell)
        }
        transactionDetailCells.addAll(arrayListOf(merchantCell, statusCell))
        configureTransactionDateCells(transaction)
        for (transactionDetail in transaction.details) {
            val amountCell = TransactionDetailCell(getString(R.string.merchant_amount), "", formatCurrencyAmount(transactionDetail.virtualCardAmount))
            transactionDetailCells.add(amountCell)
            when (transaction.type) {
                TransactionType.PENDING, TransactionType.COMPLETE -> {
                    val feePercentStr = "%.2f%%".format((transactionDetail.markup.percent / 1000.0))
                    val feeFlatStr = "$%.2f".format((transactionDetail.markup.flat / 100.0))
                    val serviceFeeSubtitle = "$feePercentStr + $feeFlatStr"
                    val serviceFeeCell = TransactionDetailCell(getString(R.string.service_fee), serviceFeeSubtitle, formatCurrencyAmount(transactionDetail.markupAmount))
                    val totalFeeCell = TransactionDetailCell(getString(R.string.funding_source_charge_amount), "", formatCurrencyAmount(transactionDetail.fundingSourceAmount))
                    transactionDetailCells.addAll(arrayListOf(serviceFeeCell, totalFeeCell))
                }
                else -> {}
            }
            val chargeStatus = transactionDetail.state
            val chargeStatusCell = TransactionDetailCell(getString(R.string.charge_status), "", chargeStatus.name)
            transactionDetailCells.add(chargeStatusCell)
        }
    }

    /**
     * Configures the transaction detail date labels and value text.
     *
     * @param transaction The selected [Transaction] to display its details.
     */
    private fun configureTransactionDateCells(transaction: Transaction) {
        when (transaction.type) {
            TransactionType.PENDING -> {
                val dateChargedCell = TransactionDetailCell(getString(R.string.date_charged), "", formatDate(transaction.transactedAt))
                transactionDetailCells.add(dateChargedCell)
            }
            TransactionType.COMPLETE -> {
                val dateSettledCell = TransactionDetailCell(getString(R.string.date_settled), "", formatDate(transaction.transactedAt))
                transactionDetailCells.add(dateSettledCell)
            }
            TransactionType.REFUND -> {
                val dateRefundedCell = TransactionDetailCell(getString(R.string.date_refunded), "", formatDate(transaction.transactedAt))
                transactionDetailCells.add(dateRefundedCell)
            }
            TransactionType.DECLINE -> {
                val dateDeclinedCell = TransactionDetailCell(getString(R.string.date_declined), "", formatDate(transaction.transactedAt))
                transactionDetailCells.add(dateDeclinedCell)
            }
            else -> {}
        }
    }

    /**
     * Configures the labels and value text of [Sudo], [FundingSource] and [VirtualCard] details.
     *
     * @param virtualCard The selected [VirtualCard] to display its associated account details.
     */
    private fun configureAccountDetails(virtualCard: VirtualCard) {
        binding.virtualCardLabel.text = virtualCard.metadata?.unwrap().toString()
        retrieveAssociatedFundingSource(virtualCard)
        retrieveAssociatedSudo(virtualCard)
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
        return DateFormat.format("MMM dd, yyyy", date).toString()
    }
}
