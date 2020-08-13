/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.cards

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudovirtualcards.SudoVirtualCardsClient
import com.sudoplatform.sudovirtualcards.types.BillingAddress
import com.sudoplatform.sudovirtualcards.types.CachePolicy
import com.sudoplatform.sudovirtualcards.types.Card
import com.sudoplatform.sudovirtualcards.types.FundingSource
import com.sudoplatform.sudovirtualcards.types.ProvisionalCard
import com.sudoplatform.sudovirtualcards.types.inputs.ProvisionCardInput
import com.sudoplatform.virtualcardsexample.App
import com.sudoplatform.virtualcardsexample.MissingFragmentArgumentException
import com.sudoplatform.virtualcardsexample.R
import com.sudoplatform.virtualcardsexample.createLoadingAlertDialog
import com.sudoplatform.virtualcardsexample.shared.InputFormAdapter
import com.sudoplatform.virtualcardsexample.shared.InputFormCell
import com.sudoplatform.virtualcardsexample.showAlertDialog
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_create_card.*
import kotlinx.android.synthetic.main.fragment_create_card.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This [CreateCardFragment] presents a form so that a user can create a [Card].
 *
 * - Links From:
 *  - [CardsFragment]: A user taps the "Create Virtual Card" button at the bottom of the list.
 *
 * - Links To:
 *  - [CardDetailFragment]: If a user successfully creates a card, the [CardDetailFragment] will be
 *   presented so the user can view card details and transactions.
 */
class CreateCardFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** Toolbar [Menu] displaying title and create button. */
    private lateinit var toolbarMenu: Menu

    /** A reference to the input form [RecyclerView.Adapter] handling input form data. */
    private lateinit var adapter: InputFormAdapter

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private lateinit var loading: AlertDialog

    /** A list of [InputFormCell]s that corresponds to the cells of the input form. */
    private val inputFormCells = mutableListOf<InputFormCell>()

    /** An array of labels used for each [InputFormCell]. */
    private var labels = emptyArray<String>()

    /** An array of the default text populated for each [InputFormCell]. */
    private val enteredInput = arrayOf(
        "Unlimited Cards", "Shopping", "123 Street Rd", null,
        "Salt Lake City", "UT", "84044", "US"
    )

    /** A [FundingSource] used to create a [Card]. */
    private lateinit var fundingSource: FundingSource

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_card, container, false)
        val toolbar = (view.toolbar as Toolbar)
        toolbar.title = getString(R.string.create_virtual_card)

        toolbar.inflateMenu(R.menu.nav_menu_with_create_button)
        toolbar.setOnMenuItemClickListener {
            when (it?.itemId) {
                R.id.create -> {
                    createCard()
                }
            }
            true
        }
        toolbarMenu = toolbar.menu
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView(view)
        configureFormCells()
        setSudoLabelText()
        setErrorLabelHidden(true)
        setItemsEnabled(false)
        navController = Navigation.findNavController(view)

        view.learnMoreButton.setOnClickListener {
            learnMore()
        }

        loadFirstActiveFundingSource()
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        super.onDestroy()
    }

    /**
     * Validates and creates a [Card] from the [SudoVirtualCardsClient] based on the submitted
     * form inputs.
     */
    private fun createCard() {
        if (validateFormData()) {
            showAlertDialog(
                titleResId = R.string.validate_fields,
                positiveButtonResId = android.R.string.ok
            )
            return
        }
        val app = requireActivity().application as App
        val sudoId = requireArguments().getString(getString(R.string.sudo_id))
            ?: throw MissingFragmentArgumentException("Sudo identifier missing")
        val billingAddress = BillingAddress(
            addressLine1 = enteredInput[2] ?: "",
            addressLine2 = enteredInput[3],
            city = enteredInput[4] ?: "",
            state = enteredInput[5] ?: "",
            postalCode = enteredInput[6] ?: "",
            country = enteredInput[7] ?: ""
        )
        val input = ProvisionCardInput(
            sudoId = sudoId,
            fundingSourceId = fundingSource.id,
            cardHolder = enteredInput[0] ?: "",
            alias = enteredInput[1] ?: "",
            billingAddress = billingAddress,
            currency = "USD"
        )
        launch {
            try {
                showLoading(R.string.creating_card)
                val initialProvisionalCard = withContext(Dispatchers.IO) {
                    app.sudoVirtualCardsClient.provisionCard(input)
                }
                var state = initialProvisionalCard.state
                while (state == ProvisionalCard.State.PROVISIONING) {
                    val provisionalCard = withContext(Dispatchers.IO) {
                        app.sudoVirtualCardsClient.getProvisionalCard(initialProvisionalCard.id)
                    }
                    if (provisionalCard?.state == ProvisionalCard.State.COMPLETED) {
                        showAlertDialog(
                            titleResId = R.string.success,
                            positiveButtonResId = android.R.string.ok,
                            onPositive = {
                                val bundle = bundleOf(
                                    getString(R.string.card) to provisionalCard.card
                                )
                                navController.navigate(R.id.action_createCardFragment_to_cardDetailFragment, bundle)
                            }
                        )
                        break
                    }
                    state = provisionalCard?.state ?: ProvisionalCard.State.PROVISIONING
                }
            } catch (e: SudoVirtualCardsClient.CardException) {
                showAlertDialog(
                    titleResId = R.string.create_card_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { createCard() },
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideLoading()
        }
    }

    /** Load the first active [FundingSource] from the [SudoVirtualCardsClient] associated with the user's account. */
    private fun loadFirstActiveFundingSource() {
        val app = requireActivity().application as App
        launch {
            try {
                val fundingSources = withContext(Dispatchers.IO) {
                    app.sudoVirtualCardsClient.listFundingSources(cachePolicy = CachePolicy.REMOTE_ONLY)
                }
                fundingSource = fundingSources.items.first { it.state == FundingSource.State.ACTIVE }
                fundingSourceLabel?.text = getString(R.string.funding_source_label, fundingSource.network, fundingSource.last4)
                setItemsEnabled(true)
            } catch (e: Exception) {
                when (e) {
                    is NoSuchElementException -> setErrorLabelHidden(false)
                    else -> {
                        setItemsEnabled(false)
                        showAlertDialog(
                            titleResId = R.string.list_funding_sources_failure,
                            message = e.localizedMessage ?: "$e",
                            negativeButtonResId = android.R.string.cancel
                        )
                    }
                }
            }
        }
    }

    /**
     * Configures the [RecyclerView] used to display the [InputFormCell]s and listens to input
     * change events to capture user input.
     */
    private fun configureRecyclerView(view: View) {
        adapter = InputFormAdapter(inputFormCells) { position, charSeq ->
            enteredInput[position] = charSeq
        }

        view.formRecyclerView.setHasFixedSize(true)
        view.formRecyclerView.adapter = adapter
        view.formRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    /** Configures [InputFormCell] labels, text field hints and placeholder text. */
    private fun configureFormCells() {
        labels = resources.getStringArray(R.array.create_card_labels)
        for (i in labels.indices) {
            val hint = if (labels[i].contains(getString(R.string.address_line_2))) getString(R.string.enter_optional_input, labels[i]) else getString(R.string.enter_non_optional_input, labels[i])
            inputFormCells.add(InputFormCell(labels[i], enteredInput[i] ?: "", hint))
        }
    }

    /** Navigates to a Sudo Platform web page when the "Learn More" button is pressed. */
    private fun learnMore() {
        val openUrl = Intent(Intent.ACTION_VIEW)
        openUrl.data = Uri.parse(getString(R.string.create_card_doc_url))
        startActivity(openUrl)
    }

    /** Validates submitted input form data. */
    private fun validateFormData(): Boolean {
        val requiredInput = enteredInput.toMutableList()
        requiredInput.removeAt(3)
        return requiredInput.contains("")
    }

    /** Set the [Sudo] label text containing the [Sudo] alias. */
    private fun setSudoLabelText() {
        val sudoLabelText = requireArguments().getString(getString(R.string.sudo_label))
        sudoLabel?.text = sudoLabelText
    }

    /** Hide the [errorLabel] from the view. */
    private fun setErrorLabelHidden(isHidden: Boolean) {
        if (isHidden) {
            errorLabel?.visibility = View.GONE
        } else {
            errorLabel?.visibility = View.VISIBLE
        }
    }

    /**
     * Sets toolbar items to enabled/disabled.
     *
     * @param isEnabled If true, toolbar items will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        toolbarMenu.getItem(0)?.isEnabled = isEnabled
    }

    /** Displays the loading [AlertDialog] indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int) {
        loading = createLoadingAlertDialog(textResId)
        loading.show()
        setItemsEnabled(false)
    }

    /** Dismisses the loading [AlertDialog] indicating that an operation has finished. */
    private fun hideLoading() {
        loading.dismiss()
        setItemsEnabled(true)
    }
}
