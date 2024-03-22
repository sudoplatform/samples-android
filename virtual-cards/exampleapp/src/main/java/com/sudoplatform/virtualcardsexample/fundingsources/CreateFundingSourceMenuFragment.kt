/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.virtualcardsexample.fundingsources

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.sudoplatform.virtualcardsexample.App
import com.sudoplatform.virtualcardsexample.R
import com.sudoplatform.virtualcardsexample.databinding.FragmentCreateFundingSourceMenuBinding
import com.sudoplatform.virtualcardsexample.util.ObjectDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlin.coroutines.CoroutineContext

/**
 * This [CreateFundingSourceMenuFragment] presents a menu type screen so that the user can navigate
 * through each of the funding source creation menu items.
 *
 * Links From:
 *  - [FundingSourcesFragment]: A user taps the "Create Funding Source" button.
 *
 * Links To:
 *  - [CreateCardFundingSourceFragment]: If a user taps the "Add Stripe Credit Card" button, the
 *   [CreateCardFundingSourceFragment] will be presented so the user can create a Stripe credit
 *   card based funding source.
 *  - [CreateBankAccountFundingSourceFragment]: If a user taps the "Add Checkout Bank Account"
 *   button, the [CreateBankAccountFundingSourceFragment] will be presented so the user can create
 *   a Checkout bank account based funding source.
 */
class CreateFundingSourceMenuFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentCreateFundingSourceMenuBinding>()
    private val binding by bindingDelegate

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingDelegate.attach(FragmentCreateFundingSourceMenuBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.create_funding_source)
        }
        app = requireActivity().application as App
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        binding.createStripeCardButton.setOnClickListener {
            navController.navigate(
                CreateFundingSourceMenuFragmentDirections
                    .actionCreateFundingSourceMenuFragmentToCreateCardFundingSourceFragment(),
            )
        }
        binding.createCheckoutBankAccountButton.setOnClickListener {
            navController.navigate(
                CreateFundingSourceMenuFragmentDirections
                    .actionCreateFundingSourceMenuFragmentToCreateCheckoutBankAccountFundingSourceFragment(),
            )
        }
        binding.learnMoreButton.setOnClickListener {
            learnMore()
        }
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    /** Navigates to a Sudo Platform web page when the "Learn More" button is pressed. */
    private fun learnMore() {
        val openUrl = Intent(Intent.ACTION_VIEW)
        openUrl.data = Uri.parse(getString(R.string.create_funding_source_doc_url))
        startActivity(openUrl)
    }
}
