/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.fundingsources

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudovirtualcards.SudoVirtualCardsClient
import com.sudoplatform.sudovirtualcards.extensions.isUnfunded
import com.sudoplatform.sudovirtualcards.types.CheckoutBankAccountProviderRefreshData
import com.sudoplatform.sudovirtualcards.types.CheckoutBankAccountRefreshUserInteractionData
import com.sudoplatform.sudovirtualcards.types.ClientApplicationData
import com.sudoplatform.sudovirtualcards.types.FundingSource
import com.sudoplatform.sudovirtualcards.types.inputs.RefreshFundingSourceInput
import com.sudoplatform.virtualcardsexample.App
import com.sudoplatform.virtualcardsexample.R
import com.sudoplatform.virtualcardsexample.createLoadingAlertDialog
import com.sudoplatform.virtualcardsexample.databinding.FragmentFundingSourcesBinding
import com.sudoplatform.virtualcardsexample.mainmenu.MainMenuFragment
import com.sudoplatform.virtualcardsexample.showAlertDialog
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
 * This [FundingSourcesFragment] presents a list of [FundingSource]s.
 *
 * - Links From:
 *  - [MainMenuFragment]: A user chooses the "Funding Sources" option from the main menu which will
 *   show this view with the list of funding sources created. The last four digits of the funding
 *   source's card number and credit card network is used as the text for each funding source.
 *
 * - Links To:
 *  - [CreateFundingSourceMenuFragment]: If a user taps the "Create Funding Source" button, the
 *   [CreateFundingSourceMenuFragment] will be presented so the user can select a funding source to
 *   create.
 */
class FundingSourcesFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentFundingSourcesBinding>()
    private val binding by bindingDelegate

    /** A reference to the [RecyclerView.Adapter] handling [FundingSource] data. */
    private lateinit var adapter: FundingSourceAdapter

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** A mutable list of [FundingSource]s. */
    private var fundingSourceList = mutableListOf<FundingSource>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingDelegate.attach(FragmentFundingSourcesBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.funding_sources)
        }
        app = requireActivity().application as App
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView()
        navController = Navigation.findNavController(view)

        binding.createFundingSourceButton.setOnClickListener {
            navController.navigate(
                FundingSourcesFragmentDirections
                    .actionFundingSourcesFragmentToCreateFundingSourceMenuFragment(),
            )
        }

        listFundingSources()
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    /**
     * List [FundingSource]s from the [SudoVirtualCardsClient].
     */
    private fun listFundingSources() {
        launch {
            try {
                showLoading()
                val fundingSources = withContext(Dispatchers.IO) {
                    app.sudoVirtualCardsClient.listFundingSources()
                }
                fundingSourceList.clear()
                for (fundingSource in fundingSources.items) {
                    fundingSourceList.add(fundingSource)
                }
                adapter.notifyDataSetChanged()
            } catch (e: SudoVirtualCardsClient.FundingSourceException) {
                showAlertDialog(
                    titleResId = R.string.list_funding_sources_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { listFundingSources() },
                    negativeButtonResId = android.R.string.cancel,
                )
            }
            hideLoading()
        }
    }

    /**
     * Cancel a [FundingSource] from the [SudoVirtualCardsClient] based on the input [id].
     *
     * @param id [String] The identifier of the [FundingSource] to cancel.
     * @param completion Callback which executes when the operation is completed.
     */
    private fun cancelFundingSource(id: String, completion: (FundingSource) -> Unit) {
        launch {
            try {
                showWorkingAlert(R.string.cancelling_funding_source)
                val fundingSource = withContext(Dispatchers.IO) {
                    app.sudoVirtualCardsClient.cancelFundingSource(id)
                }
                hideWorkingAlert()
                completion(fundingSource)
                showAlertDialog(
                    titleResId = R.string.success,
                    positiveButtonResId = android.R.string.ok,
                )
            } catch (e: SudoVirtualCardsClient.FundingSourceException) {
                hideWorkingAlert()
                showAlertDialog(
                    titleResId = R.string.cancel_funding_source_failure,
                    message = e.localizedMessage ?: "$e",
                    negativeButtonResId = android.R.string.cancel,
                )
            }
        }
    }

    /**
     * Request review of a [FundingSource] from the [SudoVirtualCardsClient] based on the input [id].
     *
     * @param id [String] The identifier of the [FundingSource] to cancel.
     * @param completion Callback which executes when the operation is completed.
     */
    private fun reviewFundingSource(id: String, completion: (FundingSource) -> Unit) {
        launch {
            try {
                showWorkingAlert(R.string.reviewing_funding_source)
                val fundingSource = withContext(Dispatchers.IO) {
                    app.sudoVirtualCardsClient.reviewUnfundedFundingSource(id)
                }
                hideWorkingAlert()
                completion(fundingSource)
                showAlertDialog(
                    titleResId = R.string.success,
                    positiveButtonResId = android.R.string.ok,
                )
            } catch (e: SudoVirtualCardsClient.FundingSourceException) {
                hideWorkingAlert()
                showAlertDialog(
                    titleResId = R.string.review_funding_source_failure,
                    message = e.localizedMessage ?: "$e",
                    negativeButtonResId = android.R.string.cancel,
                )
            }
        }
    }

    /**
     * Refreshes the bank account funding source from the [SudoVirtualCardsClient].
     *
     * @param id [String] The identifier of the funding source to refresh.
     */
    private fun refreshFundingSource(id: String) {
        launch {
            try {
                showLoading(R.string.refreshing_funding_source)
                withContext(Dispatchers.IO) {
                    val refreshData = CheckoutBankAccountProviderRefreshData(
                        accountId = null,
                        authorizationText = null,
                    )
                    val input = RefreshFundingSourceInput(
                        id,
                        refreshData,
                        ClientApplicationData("androidApplication"),
                        language = "en-US",
                    )
                    app.sudoVirtualCardsClient.refreshFundingSource(input)
                }
            } catch (e: SudoVirtualCardsClient.FundingSourceException) {
                when (e) {
                    is SudoVirtualCardsClient.FundingSourceException.FundingSourceRequiresUserInteractionException -> {
                        when (e.interactionData) {
                            is CheckoutBankAccountRefreshUserInteractionData -> {
                                val userInteractionData = e.interactionData as CheckoutBankAccountRefreshUserInteractionData
                                val linkToken = userInteractionData.linkToken.linkToken
                                val authorizationText = userInteractionData.authorizationText
                                navController.navigate(
                                    FundingSourcesFragmentDirections
                                        .actionFundingSourcesFragmentToRefreshBankAccountFundingSourceFragment(
                                            id,
                                            authorizationText.toTypedArray(),
                                            linkToken,
                                        ),
                                )
                            }
                            else -> {
                                showAlertDialog(
                                    titleResId = R.string.refresh_funding_source_failure,
                                    message = e.localizedMessage ?: "$e",
                                    positiveButtonResId = R.string.try_again,
                                    onPositive = { refreshFundingSource(id) },
                                    negativeButtonResId = android.R.string.cancel,
                                )
                            }
                        }
                    }
                    else -> {
                        showAlertDialog(
                            titleResId = R.string.refresh_funding_source_failure,
                            message = e.localizedMessage ?: "$e",
                            positiveButtonResId = R.string.try_again,
                            onPositive = { refreshFundingSource(id) },
                            negativeButtonResId = android.R.string.cancel,
                        )
                    }
                }
            }
            hideLoading()
        }
    }

    /**
     * Configures the [RecyclerView] used to display the listed [FundingSource] items.
     */
    private fun configureRecyclerView() {
        adapter = FundingSourceAdapter(fundingSourceList) { fundingSource ->
            refreshFundingSource(fundingSource.id)
        }
        binding.fundingSourceRecyclerView.adapter = adapter
        binding.fundingSourceRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        configureSwipeToCancel()
    }

    /**
     * Sets buttons and recycler view to enabled/disabled.
     *
     * @param isEnabled [Boolean] If true, buttons and recycler view will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        binding.createFundingSourceButton.isEnabled = isEnabled
        binding.fundingSourceRecyclerView.isEnabled = isEnabled
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int = 0) {
        if (textResId != 0) {
            binding.progressText.text = getString(textResId)
        }
        binding.progressBar.visibility = View.VISIBLE
        binding.progressText.visibility = View.VISIBLE
        binding.fundingSourceRecyclerView.visibility = View.GONE
        setItemsEnabled(false)
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        if (bindingDelegate.isAttached()) {
            binding.progressBar.visibility = View.GONE
            binding.progressText.visibility = View.GONE
            binding.fundingSourceRecyclerView.visibility = View.VISIBLE
            setItemsEnabled(true)
        }
    }

    /** Displays the loading [AlertDialog] indicating that a cancel operation is occurring. */
    private fun showWorkingAlert(@StringRes textResId: Int) {
        loading = createLoadingAlertDialog(textResId)
        loading?.show()
    }

    /** Dismisses the loading [AlertDialog] indicating that a cancel operation has finished. */
    private fun hideWorkingAlert() {
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
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.fundingSourceRecyclerView)
    }

    private fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val fundingSource = fundingSourceList[viewHolder.adapterPosition]
        if (fundingSource.isUnfunded()) {
            reviewFundingSource(fundingSource.id) { reviewedFundingSource ->
                val position = viewHolder.adapterPosition
                fundingSourceList.removeAt(position)
                adapter.notifyItemRemoved(position)
                fundingSourceList.add(position, reviewedFundingSource)
                adapter.notifyItemInserted(position)
            }
            return
        }
        cancelFundingSource(fundingSource.id) { cancelledFundingSource ->
            val position = viewHolder.adapterPosition
            fundingSourceList.removeAt(position)
            adapter.notifyItemRemoved(position)
            fundingSourceList.add(position, cancelledFundingSource)
            adapter.notifyItemInserted(position)
        }
    }
}
