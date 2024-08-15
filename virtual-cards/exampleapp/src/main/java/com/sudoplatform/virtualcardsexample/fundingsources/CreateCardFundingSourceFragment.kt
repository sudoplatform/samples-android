/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.fundingsources

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.Stripe
import com.sudoplatform.sudovirtualcards.SudoVirtualCardsClient
import com.sudoplatform.sudovirtualcards.types.ClientApplicationData
import com.sudoplatform.sudovirtualcards.types.FundingSource
import com.sudoplatform.sudovirtualcards.types.FundingSourceType
import com.sudoplatform.sudovirtualcards.types.StripeCardProvisioningData
import com.sudoplatform.sudovirtualcards.types.inputs.CompleteFundingSourceInput
import com.sudoplatform.sudovirtualcards.types.inputs.CreditCardFundingSourceInput
import com.sudoplatform.sudovirtualcards.types.inputs.SetupFundingSourceInput
import com.sudoplatform.virtualcardsexample.App
import com.sudoplatform.virtualcardsexample.R
import com.sudoplatform.virtualcardsexample.createLoadingAlertDialog
import com.sudoplatform.virtualcardsexample.databinding.FragmentCreateCardFundingSourceBinding
import com.sudoplatform.virtualcardsexample.shared.InputFormAdapter
import com.sudoplatform.virtualcardsexample.shared.InputFormCell
import com.sudoplatform.virtualcardsexample.showAlertDialog
import com.sudoplatform.virtualcardsexample.util.ActivityResultHandler
import com.sudoplatform.virtualcardsexample.util.DefaultActivityResultHandler
import com.sudoplatform.virtualcardsexample.util.ObjectDelegate
import com.sudoplatform.virtualcardsexample.util.StripeIntentWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.coroutines.CoroutineContext

/**
 * This [CreateCardFundingSourceFragment] presents a form so that a user can create a Stripe
 * credit card based [FundingSource].
 *
 * - Links From:
 *  - [CreateFundingSourceMenuFragment]: A user taps the "Create Stripe Funding Source" button.
 *
 * - Links To:
 *  - [FundingSourcesFragment]: If a user successfully creates a funding source, they will be returned
 *   to this form.
 */
class CreateCardFundingSourceFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentCreateCardFundingSourceBinding>()
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
        "4242424242424242", "10", expirationYear(),
        "123", "222333 Peachtree Place", null, "Atlanta", "GA", "30318", "US",
    )

    private val activityResultHandler: ActivityResultHandler = DefaultActivityResultHandler()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingDelegate.attach(FragmentCreateCardFundingSourceBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.add_credit_card)
            inflateMenu(R.menu.nav_menu_with_create_button)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.create -> {
                        createFundingSource()
                    }
                }
                true
            }
            toolbarMenu = menu
        }
        app = requireActivity().application as App
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView()
        configureFormCells()
        navController = Navigation.findNavController(view)
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    // Manages the callback from stripe 3DS processing and notifies our singleton handler
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        activityResultHandler.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * Validates and creates a [FundingSource] from the
     * [SudoVirtualCardsClient] based on the submitted form inputs.
     */
    private fun createFundingSource() {
        if (validateFormData()) {
            showAlertDialog(
                titleResId = R.string.validate_fields,
                positiveButtonResId = android.R.string.ok,
            )
            return
        }
        val expMonth = (enteredInput[1] ?: "0")
        val expYear = (enteredInput[2] ?: "0")
        val input = CreditCardFundingSourceInput(
            cardNumber = enteredInput[0] ?: "",
            expirationMonth = if (expMonth.matches("-?\\d+(\\.\\d+)?".toRegex())) expMonth.toInt() else 0,
            expirationYear = if (expYear.matches("-?\\d+(\\.\\d+)?".toRegex())) expYear.toInt() else 0,
            securityCode = enteredInput[3] ?: "",
            addressLine1 = enteredInput[4] ?: "",
            addressLine2 = enteredInput[5] ?: "",
            city = enteredInput[6] ?: "",
            state = enteredInput[7] ?: "",
            postalCode = enteredInput[8] ?: "",
            country = enteredInput[9] ?: "",
        )
        val fragment = this
        launch {
            try {
                showLoading(R.string.creating_funding_source)
                withContext(Dispatchers.IO) {
                    // Retrieve the funding source client configuration
                    val configuration = app.sudoVirtualCardsClient.getFundingSourceClientConfiguration()
                    // Perform the funding source setup operation
                    val setupInput = SetupFundingSourceInput("USD", FundingSourceType.CREDIT_CARD, ClientApplicationData("androidApplication"))
                    val provisionalFundingSource = app.sudoVirtualCardsClient.setupFundingSource(setupInput)
                    // Process stripe data
                    val stripeClient = Stripe(requireContext(), configuration.first().apiKey)
                    val stripeIntentWorker = StripeIntentWorker(requireContext(), stripeClient, activityResultHandler)
                    val provisioningData = provisionalFundingSource.provisioningData as StripeCardProvisioningData
                    val completionData = stripeIntentWorker.confirmSetupIntent(
                        input,
                        provisioningData.clientSecret,
                        fragment,
                    )
                    // Perform the funding source completion operation
                    val completeInput = CompleteFundingSourceInput(
                        provisionalFundingSource.id,
                        completionData,
                        null,
                    )
                    app.sudoVirtualCardsClient.completeFundingSource(completeInput)
                }
                showAlertDialog(
                    titleResId = R.string.success,
                    positiveButtonResId = android.R.string.ok,
                    onPositive = {
                        navController.navigate(
                            CreateCardFundingSourceFragmentDirections
                                .actionCreateCardFundingSourceFragmentToFundingSourcesFragment(),
                        )
                    },
                )
            } catch (e: SudoVirtualCardsClient.FundingSourceException) {
                showAlertDialog(
                    titleResId = R.string.create_funding_source_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { createFundingSource() },
                    negativeButtonResId = android.R.string.cancel,
                )
            }
            hideLoading()
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
        labels = resources.getStringArray(R.array.create_funding_source_labels)
        for (i in labels.indices) {
            val hint = if (labels[i].contains(getString(R.string.address_line_2))) getString(R.string.enter_optional_input, labels[i]) else getString(R.string.enter_non_optional_input, labels[i])
            inputFormCells.add(InputFormCell(labels[i], enteredInput[i] ?: "", hint))
        }
    }

    /** Returns the next calendar year. */
    private fun expirationYear(): String {
        val calender = Calendar.getInstance()
        calender.add(Calendar.YEAR, 1)
        return calender.get(Calendar.YEAR).toString()
    }

    /** Validates submitted input form data. */
    private fun validateFormData(): Boolean {
        val requiredInput = enteredInput.toMutableList()
        requiredInput.removeAt(4)
        return requiredInput.contains("")
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
}
