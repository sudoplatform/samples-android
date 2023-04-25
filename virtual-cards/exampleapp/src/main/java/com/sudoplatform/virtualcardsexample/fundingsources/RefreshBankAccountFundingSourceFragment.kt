/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.fundingsources

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
import androidx.navigation.fragment.navArgs
import com.plaid.link.OpenPlaidLink
import com.plaid.link.configuration.LinkTokenConfiguration
import com.plaid.link.result.LinkExit
import com.plaid.link.result.LinkSuccess
import com.sudoplatform.sudovirtualcards.SudoVirtualCardsClient
import com.sudoplatform.sudovirtualcards.types.AuthorizationText
import com.sudoplatform.sudovirtualcards.types.CheckoutBankAccountProviderRefreshData
import com.sudoplatform.sudovirtualcards.types.ClientApplicationData
import com.sudoplatform.sudovirtualcards.types.FundingSource
import com.sudoplatform.sudovirtualcards.types.inputs.RefreshFundingSourceInput
import com.sudoplatform.virtualcardsexample.App
import com.sudoplatform.virtualcardsexample.R
import com.sudoplatform.virtualcardsexample.createLoadingAlertDialog
import com.sudoplatform.virtualcardsexample.databinding.FragmentRefreshBankAccountFundingSourceBinding
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
 * This [RefreshBankAccountFundingSourceFragment] presents a form so that a user can refresh a Checkout
 * bank account based [FundingSource] that has expired or has been reset.
 *
 * - Links From:
 *  - [FundingSourcesFragment]: A user taps the "Refresh" button on a funding source that is in a
 *   REFRESH state.
 *
 * - Links To:
 *  - [FundingSourcesFragment]: If a user successfully refreshes a funding source, they will be returned
 *   to this form.
 */
class RefreshBankAccountFundingSourceFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentRefreshBankAccountFundingSourceBinding>()
    private val binding by bindingDelegate

    /** Toolbar [Menu] displaying title and create button. */
    private lateinit var toolbarMenu: Menu

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: RefreshBankAccountFundingSourceFragmentArgs by navArgs()

    /** The identifier of the selected [FundingSource] used for display. */
    private lateinit var fundingSourceId: String

    /** The [AuthorizationText] to display to the user. */
    private lateinit var authorizationText: List<AuthorizationText>

    /** The link token required to refresh the bank account funding source. */
    private lateinit var linkToken: String

    /** The [LinkSuccess] result from the Plaid Link Flow. */
    private lateinit var linkSuccess: LinkSuccess

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingDelegate.attach(FragmentRefreshBankAccountFundingSourceBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.refresh_bank_account)
            inflateMenu(R.menu.nav_menu_with_refresh_button)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.refresh -> {
                        refreshFundingSource()
                    }
                }
                true
            }
            toolbarMenu = menu
        }
        app = requireActivity().application as App
        fundingSourceId = args.fundingSourceId!!
        authorizationText = args.authorizationText!!.toList()
        linkToken = args.linkToken!!
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        setToolbarItemsEnabled(false)
        hideAuthorizationScrollView()
        binding.launchPlaidLinkButton.setOnClickListener {
            startRefreshFundingSource()
        }
        binding.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                setToolbarItemsEnabled(true)
            } else {
                setToolbarItemsEnabled(false)
            }
        }
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    /**
     * Starts the bank account funding source refresh flow from the [SudoVirtualCardsClient] by
     * launching the Plaid Link flow.
     */
    private fun startRefreshFundingSource() {
        launch {
            try {
                showLoading(R.string.launching_plaid_link)
                launchPlaidLink(linkToken)
                activity?.runOnUiThread {
                    configureAuthorizationWebView(authorizationText)
                }
            } catch (e: Exception) {
                showAlertDialog(
                    titleResId = R.string.refresh_funding_source_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { startRefreshFundingSource() },
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideLoading()
        }
    }

    /** Refreshes the bank account funding source from the [SudoVirtualCardsClient]. */
    private fun refreshFundingSource() {
        launch {
            try {
                showLoading(R.string.refreshing_funding_source)
                app.sudoVirtualCardsClient.createKeysIfAbsent()
                withContext(Dispatchers.IO) {
                    val refreshData = CheckoutBankAccountProviderRefreshData(
                        accountId = linkSuccess.metadata.accounts[0].id,
                        authorizationText = authorizationText[0],
                    )
                    val input = RefreshFundingSourceInput(
                        fundingSourceId,
                        refreshData,
                        ClientApplicationData("androidApplication"),
                        authorizationText[0].language
                    )
                    app.sudoVirtualCardsClient.refreshFundingSource(input)
                }
                showAlertDialog(
                    titleResId = R.string.success,
                    positiveButtonResId = android.R.string.ok,
                    onPositive = {
                        navController.navigate(
                            RefreshBankAccountFundingSourceFragmentDirections
                                .actionRefreshBankAccountFundingSourceFragmentToFundingSourcesFragment()
                        )
                    }
                )
            } catch (e: SudoVirtualCardsClient.FundingSourceException) {
                showAlertDialog(
                    titleResId = R.string.refresh_funding_source_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { refreshFundingSource() },
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideLoading()
        }
    }

    /**
     * Builds the Plaid Link configuration based on the [linkToken] and redirects the user to the
     * Plaid Link flow in order to select a financial institution and bank account to fund the
     * funding source.
     *
     * @param linkToken [String] Link token required to launch Plaid Link.
     */
    private fun launchPlaidLink(linkToken: String) {
        val tokenConfiguration = LinkTokenConfiguration.Builder()
            .token(linkToken)
            .build()
        linkAccountToPlaid.launch(tokenConfiguration)
    }

    /** Callback used to register and launch the activity result from Plaid Link. */
    private val linkAccountToPlaid = registerForActivityResult(OpenPlaidLink()) {
        when (it) {
            is LinkSuccess -> {
                linkSuccess = it
                setItemsEnabled(false)
                configureBankAccountInformationTextView(it)
                showAuthorizationScrollView()
            }
            is LinkExit -> {
                showAlertDialog(
                    titleResId = R.string.create_funding_source_failure,
                    message = it.error?.displayMessage ?: getString(R.string.plaid_link_error),
                    negativeButtonResId = android.R.string.cancel
                )
            }
        }
    }

    /**
     * Configures the web view used to hold the authorization text.
     *
     * @param authorizationText [List<AuthorizationText>] The authorization text to display.
     */
    private fun configureAuthorizationWebView(authorizationText: List<AuthorizationText>) {
        var authorizationTextHtml = authorizationText.find {
                a ->
            a.contentType == "text/html"
        }?.content
        if (authorizationTextHtml == null) {
            val authorizationTextPlain = authorizationText.find {
                    a ->
                a.contentType == "text/plain"
            }?.content ?: ""
            authorizationTextHtml = "<p>$authorizationTextPlain</p>"
        }
        binding.agreementText.loadData(authorizationTextHtml, null, null)
    }

    /**
     * Configures the text views used to hold the bank account information returned from the
     * Plaid Link [LinkSuccess] result.
     *
     * @param linkSuccess [LinkSuccess] Success result from the Plaid Link flow.
     */
    private fun configureBankAccountInformationTextView(linkSuccess: LinkSuccess) {
        binding.institutionLabel.text = linkSuccess.metadata.institution?.name
        binding.accountTypeLabel.text = linkSuccess.metadata.accounts[0].subtype.json
        binding.accountNameLabel.text = linkSuccess.metadata.accounts[0].name
        binding.accountNumberEndingLabel.text = getString(
            R.string.account_number_ending_label, linkSuccess.metadata.accounts[0].mask
        )
    }

    /**
     * Sets toolbar items to enabled/disabled.
     *
     * @param isEnabled [Boolean] If true, toolbar items will be enabled.
     */
    private fun setToolbarItemsEnabled(isEnabled: Boolean) {
        toolbarMenu.getItem(0)?.isEnabled = isEnabled
    }

    /**
     * Sets buttons to enabled/disabled.
     *
     * @param isEnabled [Boolean] If true, buttons will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        binding.launchPlaidLinkButton.isEnabled = isEnabled
        if (isEnabled) {
            binding.launchPlaidLinkButton.setBackgroundColor(
                resources.getColor(R.color.colorPrimary, null)
            )
        } else {
            binding.launchPlaidLinkButton.setBackgroundColor(
                resources.getColor(R.color.colorGrey, null)
            )
        }
    }

    /** Displays the scroll view containing the authorization text information. */
    private fun showAuthorizationScrollView() {
        binding.scrollView.visibility = View.VISIBLE
    }

    /** Hides the scroll view containing the authorization text information. */
    private fun hideAuthorizationScrollView() {
        binding.scrollView.visibility = View.GONE
    }

    /** Displays the loading [AlertDialog] indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int) {
        loading = createLoadingAlertDialog(textResId)
        loading?.show()
        setToolbarItemsEnabled(false)
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
