/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.virtualcards

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudoprofiles.exceptions.SudoProfileException
import com.sudoplatform.sudovirtualcards.SudoVirtualCardsClient
import com.sudoplatform.sudovirtualcards.types.BillingAddress
import com.sudoplatform.sudovirtualcards.types.CachePolicy
import com.sudoplatform.sudovirtualcards.types.FundingSource
import com.sudoplatform.sudovirtualcards.types.FundingSourceState
import com.sudoplatform.sudovirtualcards.types.JsonValue
import com.sudoplatform.sudovirtualcards.types.ProvisionalVirtualCard
import com.sudoplatform.sudovirtualcards.types.VirtualCard
import com.sudoplatform.sudovirtualcards.types.inputs.ProvisionVirtualCardInput
import com.sudoplatform.virtualcardsexample.App
import com.sudoplatform.virtualcardsexample.R
import com.sudoplatform.virtualcardsexample.createLoadingAlertDialog
import com.sudoplatform.virtualcardsexample.databinding.FragmentCreateVirtualCardBinding
import com.sudoplatform.virtualcardsexample.fundingsources.FundingSourceSpinnerAdapter
import com.sudoplatform.virtualcardsexample.shared.InputFormAdapter
import com.sudoplatform.virtualcardsexample.shared.InputFormCell
import com.sudoplatform.virtualcardsexample.showAlertDialog
import com.sudoplatform.virtualcardsexample.util.ObjectDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * This [CreateVirtualCardFragment] presents a form so that a user can create a [VirtualCard].
 *
 * - Links From:
 *  - [VirtualCardsFragment]: A user taps the "Create Virtual Card" button at the bottom of the list.
 *
 * - Links To:
 *  - [VirtualCardDetailFragment]: If a user successfully creates a virtual card, the
 *   [VirtualCardDetailFragment] will be presented so the user can view card details and transactions.
 */
class CreateVirtualCardFragment : Fragment(), CoroutineScope, AdapterView.OnItemSelectedListener {

    companion object {
        const val VIRTUAL_CARD_AUDIENCE = "sudoplatform.virtual-cards.virtual-card"
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentCreateVirtualCardBinding>()
    private val binding by bindingDelegate

    /** Toolbar [Menu] displaying title and create button. */
    private lateinit var toolbarMenu: Menu

    /** A reference to the input form [RecyclerView.Adapter] handling input form data. */
    private lateinit var adapter: InputFormAdapter

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** A list of [InputFormCell]s that corresponds to the cells of the input form. */
    private val inputFormCells = mutableListOf<InputFormCell>()

    /** An array of labels used for each [InputFormCell]. */
    private var labels = emptyArray<String>()

    /** An array of the default text populated for each [InputFormCell]. */
    private val enteredInput = arrayOf(
        "Unlimited Cards", "Shopping", "123 Street Rd", null,
        "Salt Lake City", "UT", "84044", "US"
    )

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: CreateVirtualCardFragmentArgs by navArgs()

    /** A list of funding sources that can fund a virtual card. */
    private var fundingSourcesList = mutableListOf<FundingSource>()

    /** A reference to the [FundingSourceSpinnerAdapter] holding the [FundingSource] data. */
    private lateinit var fundingSourcesSpinnerAdapter: FundingSourceSpinnerAdapter

    /** The selected [FundingSource] used to create a [VirtualCard]. */
    private lateinit var selectedFundingSource: FundingSource

    /** A [Sudo] used to retrieve the ownership proof. */
    private lateinit var sudo: Sudo

    /** The ownership proof used to tie a [Sudo] to a [VirtualCard]. */
    private lateinit var ownershipProof: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingDelegate.attach(FragmentCreateVirtualCardBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.create_virtual_card)
            inflateMenu(R.menu.nav_menu_with_create_button)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.create -> {
                        createVirtualCard()
                    }
                }
                true
            }
            toolbarMenu = menu
        }
        app = requireActivity().application as App
        sudo = args.sudo!!
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView()
        configureFormCells()
        setSudoLabelText()
        setErrorLabelHidden(true)
        setItemsEnabled(false)
        navController = Navigation.findNavController(view)

        binding.fundingSourcesSpinner.onItemSelectedListener = this
        listActiveFundingSources()
        fundingSourcesSpinnerAdapter = FundingSourceSpinnerAdapter(
            requireContext(),
            fundingSourcesList,
        )
        fundingSourcesSpinnerAdapter.notifyDataSetChanged()
        binding.fundingSourcesSpinner.adapter = fundingSourcesSpinnerAdapter

        binding.learnMoreButton.setOnClickListener {
            learnMore()
        }
        getOwnershipProof()
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    /**
     * Validates and creates a [VirtualCard] from the [SudoVirtualCardsClient] based on the submitted
     * form inputs.
     */
    private fun createVirtualCard() {
        if (validateFormData()) {
            showAlertDialog(
                titleResId = R.string.validate_fields,
                positiveButtonResId = android.R.string.ok
            )
            return
        }
        val billingAddress = BillingAddress(
            addressLine1 = enteredInput[2] ?: "",
            addressLine2 = enteredInput[3],
            city = enteredInput[4] ?: "",
            state = enteredInput[5] ?: "",
            postalCode = enteredInput[6] ?: "",
            country = enteredInput[7] ?: ""
        )
        val cardLabel = JsonValue.JsonString(enteredInput[1] ?: "")
        val input = ProvisionVirtualCardInput(
            ownershipProofs = listOf(ownershipProof),
            fundingSourceId = selectedFundingSource.id,
            cardHolder = enteredInput[0] ?: "",
            metadata = cardLabel,
            billingAddress = billingAddress,
            currency = "USD"
        )
        launch {
            try {
                showLoading(R.string.creating_virtual_card)
                app.sudoVirtualCardsClient.createKeysIfAbsent()
                val initialProvisionalVirtualCard = withContext(Dispatchers.IO) {
                    app.sudoVirtualCardsClient.provisionVirtualCard(input)
                }
                var state = initialProvisionalVirtualCard.provisioningState
                while (state == ProvisionalVirtualCard.ProvisioningState.PROVISIONING) {
                    val provisionalCard = withContext(Dispatchers.IO) {
                        app.sudoVirtualCardsClient.getProvisionalCard(initialProvisionalVirtualCard.id)
                    }
                    if (provisionalCard?.provisioningState == ProvisionalVirtualCard.ProvisioningState.COMPLETED) {
                        showAlertDialog(
                            titleResId = R.string.success,
                            positiveButtonResId = android.R.string.ok,
                            onPositive = {
                                navController.navigate(
                                    CreateVirtualCardFragmentDirections
                                        .actionCreateVirtualCardFragmentToVirtualCardDetailFragment(
                                            provisionalCard.card
                                        )
                                )
                            }
                        )
                        break
                    }
                    state = provisionalCard?.provisioningState ?: ProvisionalVirtualCard.ProvisioningState.PROVISIONING
                }
            } catch (e: SudoVirtualCardsClient.VirtualCardException) {
                showAlertDialog(
                    titleResId = R.string.create_virtual_card_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { createVirtualCard() },
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideLoading()
        }
    }

    /** List the active [FundingSource]s from the [SudoVirtualCardsClient] associated with the user's account. */
    private fun listActiveFundingSources() {
        launch {
            try {
                val fundingSources = withContext(Dispatchers.IO) {
                    app.sudoVirtualCardsClient.listFundingSources(cachePolicy = CachePolicy.REMOTE_ONLY)
                }
                val filtered = fundingSources.items.filter { it.state == FundingSourceState.ACTIVE }
                fundingSourcesList.clear()
                fundingSourcesList.addAll(filtered)
                fundingSourcesSpinnerAdapter.notifyDataSetChanged()
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
     * Retrieve the ownership proof used to bind the [Sudo] and [VirtualCard] together.
     */
    private fun getOwnershipProof() {
        launch {
            try {
                ownershipProof = withContext(Dispatchers.IO) {
                    app.sudoProfilesClient.getOwnershipProof(sudo, VIRTUAL_CARD_AUDIENCE)
                }
            } catch (e: SudoProfileException) {
                showAlertDialog(
                    titleResId = R.string.ownership_proof_error,
                    message = e.localizedMessage ?: "$e",
                    negativeButtonResId = android.R.string.cancel
                )
            }
        }
    }

    /**
     * Configures the [RecyclerView] used to display the [InputFormCell]s and listens to input
     * change events to capture user input.
     */
    private fun configureRecyclerView() {
        adapter = InputFormAdapter(inputFormCells) { position, charSeq ->
            enteredInput[position] = charSeq
        }

        binding.formRecyclerView.setHasFixedSize(true)
        binding.formRecyclerView.adapter = adapter
        binding.formRecyclerView.layoutManager = LinearLayoutManager(requireContext())
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
        openUrl.data = Uri.parse(getString(R.string.create_virtual_card_doc_url))
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
        val sudoLabelText = args.sudo?.label
        binding.sudoLabel.text = sudoLabelText
    }

    /**
     * Hide the error label from the view.
     *
     * @param isHidden [Boolean] If true, the error label is hidden.
     */
    private fun setErrorLabelHidden(isHidden: Boolean) {
        if (isHidden) {
            binding.errorLabel.visibility = View.GONE
        } else {
            binding.errorLabel.visibility = View.VISIBLE
        }
    }

    /**
     * Sets toolbar items to enabled/disabled.
     *
     * @param isEnabled [Boolean] If true, toolbar items will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        toolbarMenu.getItem(0)?.isEnabled = isEnabled
    }

    /** Displays the loading [AlertDialog] indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int) {
        loading = createLoadingAlertDialog(textResId)
        loading?.show()
        setItemsEnabled(false)
    }

    /** Dismisses the loading [AlertDialog] indicating that an operation has finished. */
    private fun hideLoading() {
        loading?.dismiss()
        if (bindingDelegate.isAttached()) {
            setItemsEnabled(true)
        }
    }

    /** Sets the selected funding source. */
    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        selectedFundingSource = parent.getItemAtPosition(pos) as FundingSource
        setItemsEnabled(true)
    }
    override fun onNothingSelected(parent: AdapterView<*>) { /* no-op */ }
}
