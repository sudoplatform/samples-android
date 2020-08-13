/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.fundingsources

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
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudovirtualcards.SudoVirtualCardsClient
import com.sudoplatform.sudovirtualcards.types.FundingSource
import com.sudoplatform.sudovirtualcards.types.inputs.CreditCardFundingSourceInput
import com.sudoplatform.virtualcardsexample.App
import com.sudoplatform.virtualcardsexample.R
import com.sudoplatform.virtualcardsexample.createLoadingAlertDialog
import com.sudoplatform.virtualcardsexample.shared.InputFormAdapter
import com.sudoplatform.virtualcardsexample.shared.InputFormCell
import com.sudoplatform.virtualcardsexample.showAlertDialog
import java.util.Calendar
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_create_funding_source.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This [CreateFundingSourceFragment] presents a form so that a user can create a [FundingSource].
 *
 * - Links From:
 *  - [FundingSourcesFragment]: A user taps the "Create Funding Source" button at the bottom of the
 *    list.
 *
 * - Links To:
 *  - [FundingSourcesFragment]: If a user successfully creates a funding source, they will be returned
 *   to this form.
 */
class CreateFundingSourceFragment : Fragment(), CoroutineScope {

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
    private val enteredInput = arrayOf("4242424242424242", "10", expirationYear(),
        "123", "222333 Peachtree Place", null, "Atlanta", "GA", "30318", "US")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_funding_source, container, false)
        val toolbar = (view.toolbar as Toolbar)
        toolbar.title = getString(R.string.create_funding_source)

        toolbar.inflateMenu(R.menu.nav_menu_with_create_button)
        toolbar.setOnMenuItemClickListener {
            when (it?.itemId) {
                R.id.create -> {
                    createFundingSource()
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
        navController = Navigation.findNavController(view)

        view.learnMoreButton.setOnClickListener {
            learnMore()
        }
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        super.onDestroy()
    }

    /**
     * Validates and creates a [FundingSource] from the
     * [SudoVirtualCardsClient] based on the submitted form inputs.
     */
    private fun createFundingSource() {
        if (validateFormData()) {
            showAlertDialog(
                titleResId = R.string.validate_fields,
                positiveButtonResId = android.R.string.ok
            )
            return
        }
        val app = requireActivity().application as App
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
            country = enteredInput[9] ?: ""
        )
        launch {
            try {
                showLoading(R.string.creating_funding_source)
                withContext(Dispatchers.IO) {
                    app.sudoVirtualCardsClient.createFundingSource(input)
                }
                showAlertDialog(
                    titleResId = R.string.success,
                    positiveButtonResId = android.R.string.ok,
                    onPositive = { navController.navigate(R.id.action_createFundingSourceFragment_to_fundingSourcesFragment) }
                )
            } catch (e: SudoVirtualCardsClient.FundingSourceException) {
                showAlertDialog(
                    titleResId = R.string.create_funding_source_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { createFundingSource() },
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideLoading()
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
        labels = resources.getStringArray(R.array.create_funding_source_labels)
        for (i in labels.indices) {
            val hint = if (labels[i].contains(getString(R.string.address_line_2))) getString(R.string.enter_optional_input, labels[i]) else getString(R.string.enter_non_optional_input, labels[i])
            inputFormCells.add(InputFormCell(labels[i], enteredInput[i] ?: "", hint))
        }
    }

    /** Navigates to a Sudo Platform web page when the "Learn More" button is pressed. */
    private fun learnMore() {
        val openUrl = Intent(Intent.ACTION_VIEW)
        openUrl.data = Uri.parse(getString(R.string.create_funding_source_doc_url))
        startActivity(openUrl)
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
