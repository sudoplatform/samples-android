/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaddressblocklist

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.emailexample.App
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.databinding.FragmentEmailAddressBlocklistBinding
import com.sudoplatform.emailexample.emailmessages.EmailMessagesFragment
import com.sudoplatform.emailexample.showAlertDialog
import com.sudoplatform.emailexample.util.ObjectDelegate
import com.sudoplatform.sudoapiclient.sudoApiClientLogger
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudoemail.types.BatchOperationStatus
import com.sudoplatform.sudoemail.types.UnsealedBlockedAddressStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * This [EmailAddressBlocklistFragment] presents a list of blocked email addresses.
 *
 * - Links From:
 *  - [EmailMessagesFragment]: A user selects the "Blocklist" option in the drop down menu.
 */
class EmailAddressBlocklistFragment :
    Fragment(),
    CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentEmailAddressBlocklistBinding>()
    private val binding by bindingDelegate

    /** Toolbar [Menu] displaying title and delete button. */
    private lateinit var toolbarMenu: Menu

    /** A reference to the [RecyclerView.Adapter] handling blocked email address data. */
    private lateinit var adaptor: EmailAddressBlocklistAdaptor

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** A mutable list of blocked email addresses. */
    private var blockedAddressesList = mutableListOf<String>()

    /** A mutable list of the selected blocked email addresses. */
    private var selectedBlockedAddresses = mutableListOf<String>()

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: EmailAddressBlocklistFragmentArgs by navArgs()

    /** Email address identifier belonging to the blocked email address. */
    private lateinit var emailAddressId: String

    /** Reference to a blocked email address. */
    private lateinit var emailAddress: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingDelegate.attach(FragmentEmailAddressBlocklistBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.blocked_addresses)
            inflateMenu(R.menu.nav_menu_with_delete_button)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.delete -> {
                        // Unblock selected addresses
                        unblockEmailAddresses(selectedBlockedAddresses)
                    }
                }
                true
            }
            toolbarMenu = menu
        }
        app = requireActivity().application as App
        emailAddressId = args.emailAddressId
        emailAddress = args.emailAddress
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView()
        navController = Navigation.findNavController(view)

        listBlockedEmailAddresses()
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    /** List blocked email addresses from the [SudoEmailClient]. */
    private fun listBlockedEmailAddresses() {
        launch {
            try {
                showLoading()
                val blockedAddresses =
                    withContext(Dispatchers.IO) {
                        app.sudoEmailClient.getEmailAddressBlocklist()
                    }
                blockedAddressesList.clear()
                val cleartextAddresses = mutableListOf<String>()
                for (blockedAddress in blockedAddresses) {
                    if (blockedAddress.status === UnsealedBlockedAddressStatus.Completed) {
                        cleartextAddresses.add(blockedAddress.address)
                    } else {
                        // Handle error. Likely a missing key in which case the address could be unblocked using the hashed value
                    }
                }
                blockedAddressesList.addAll(cleartextAddresses)
                adaptor.notifyDataSetChanged()
            } catch (e: SudoEmailClient.EmailBlocklistException) {
                sudoApiClientLogger.error(e.localizedMessage ?: "$e")
                showAlertDialog(
                    titleResId = R.string.list_blocked_addresses_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { listBlockedEmailAddresses() },
                    negativeButtonResId = android.R.string.cancel,
                )
            }
            hideLoading()
        }
    }

    /**
     * Unblocks the given email addresses from the [SudoEmailClient].
     *
     * @param addresses [List<String>] The list of email addresses to unblock.
     */
    private fun unblockEmailAddresses(addresses: List<String>) {
        launch {
            showLoading(R.string.unblocking_addresses)
            try {
                val response = app.sudoEmailClient.unblockEmailAddresses(addresses)
                when (response.status) {
                    BatchOperationStatus.SUCCESS -> blockedAddressesList.removeAll(addresses)
                    BatchOperationStatus.PARTIAL -> response.successValues?.let { blockedAddressesList.removeAll(it) }
                    else -> {
                        showAlertDialog(
                            titleResId = R.string.unblock_addresses_failure,
                            message = getString(R.string.something_wrong),
                            positiveButtonResId = R.string.try_again,
                            onPositive = { unblockEmailAddresses(addresses) },
                            negativeButtonResId = android.R.string.cancel,
                        )
                    }
                }
                adaptor.notifyDataSetChanged()
            } catch (e: SudoEmailClient.EmailBlocklistException) {
                showAlertDialog(
                    titleResId = R.string.unblock_addresses_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { unblockEmailAddresses(addresses) },
                    negativeButtonResId = android.R.string.cancel,
                )
            } finally {
                hideLoading()
            }
        }
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading(
        @StringRes textResId: Int = 0,
    ) {
        if (textResId != 0) {
            binding.progressText.text = getString(textResId)
        }
        binding.progressBar.visibility = View.VISIBLE
        binding.progressText.visibility = View.VISIBLE
        binding.blockedAddressesRecyclerView.visibility = View.GONE
        setItemsEnabled(false)
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        if (bindingDelegate.isAttached()) {
            binding.progressBar.visibility = View.GONE
            binding.progressText.visibility = View.GONE
            binding.blockedAddressesRecyclerView.visibility = View.VISIBLE
            setItemsEnabled(true)
        }
    }

    /**
     * Sets toolbar items and recycler view to enabled/disabled.
     *
     * @param isEnabled [Boolean] If true, toolbar items and recycler view will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        toolbarMenu.getItem(0)?.isEnabled = isEnabled
        binding.blockedAddressesRecyclerView.isEnabled = isEnabled
    }

    /**
     * Configures the [RecyclerView] used to display the listed blocked email address items and
     * listens to item select events.
     */
    private fun configureRecyclerView() {
        adaptor =
            EmailAddressBlocklistAdaptor(blockedAddressesList) { selected, address ->
                if (selected) {
                    selectedBlockedAddresses.add(address)
                } else {
                    selectedBlockedAddresses.remove(address)
                }
            }
        binding.blockedAddressesRecyclerView.adapter = adaptor
        binding.blockedAddressesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }
}
