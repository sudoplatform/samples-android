/*
 * Copyright © 2024 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.addressblocklist

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
import com.sudoplatform.emailexample.App
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.databinding.FragmentAddressBlocklistBinding
import com.sudoplatform.emailexample.showAlertDialog
import com.sudoplatform.emailexample.util.ObjectDelegate
import com.sudoplatform.sudoapiclient.sudoApiClientLogger
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudoemail.types.BatchOperationResult
import com.sudoplatform.sudoemail.types.BatchOperationStatus
import com.sudoplatform.sudoemail.types.UnsealedBlockedAddressStatus
import com.sudoplatform.sudoprofiles.Sudo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class AddressBlocklistFragment : Fragment(), CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentAddressBlocklistBinding>()
    private val binding by bindingDelegate

    /** Toolbar [Menu] displaying title and block and reply buttons. */
    private lateinit var toolbarMenu: Menu

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    private lateinit var adaptor: AddressBlocklistAdaptor

    private var blockedAddressesList = mutableListOf<String>()

    private var selectedBlockedAddresses = mutableListOf<String>()

    private val args: AddressBlocklistFragmentArgs by navArgs()

    private lateinit var sudo: Sudo

    private lateinit var emailAddressId: String

    private lateinit var emailAddress: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        bindingDelegate.attach(FragmentAddressBlocklistBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.blocked_addresses)
            inflateMenu(R.menu.nav_menu_with_back_delete_buttons)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.delete -> {
                        // Unblock selected addresses
                        unblockEmailAddresses(selectedBlockedAddresses)
                    }
                    R.id.back -> {
                        navController.navigate(
                            AddressBlocklistFragmentDirections
                                .actionAddressBlocklistFragmentToEmailMessagesFragment(
                                    emailAddress,
                                    emailAddressId,
                                    sudo,
                                ),
                        )
                    }
                }
                true
            }
            toolbarMenu = menu
        }
        app = requireActivity().application as App
        sudo = args.sudo
        emailAddressId = args.emailAddressId
        emailAddress = args.emailAddress
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView()
        navController = Navigation.findNavController(view)

        listBlockedEmails()
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    /**
     * List blocked email addresses from the [SudoEmailClient]
     */
    private fun listBlockedEmails() {
        sudoApiClientLogger.debug("listBlockedEmails init. List $blockedAddressesList has ${blockedAddressesList.size} items")
        launch {
            showLoading()

            try {
                val blockedAddresses = withContext(Dispatchers.IO) {
                    app.sudoEmailClient.getEmailAddressBlocklist()
                }
                sudoApiClientLogger.debug("Returned ${blockedAddresses.size} items")
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
                    onPositive = { listBlockedEmails() },
                    negativeButtonResId = android.R.string.cancel,
                )
            }
            binding.filter.visibility = View.VISIBLE
            hideLoading()
            sudoApiClientLogger.debug("listBlockedEmails finish. List $blockedAddressesList has ${blockedAddressesList.size} items")
        }
    }

    /**
     * Unblocks the given email addresses
     */
    private fun unblockEmailAddresses(addresses: List<String>) {
        launch {
            showLoading(R.string.unblocking_addresses)
            try {
                when (val response = app.sudoEmailClient.unblockEmailAddresses(addresses)) {
                    is BatchOperationResult.SuccessOrFailureResult -> {
                        if (response.status === BatchOperationStatus.SUCCESS) {
                            blockedAddressesList.removeAll(addresses)
                        } else {
                            showAlertDialog(
                                titleResId = R.string.unblock_addresses_failure,
                                message = "Something went wrong.",
                                positiveButtonResId = R.string.try_again,
                                onPositive = { unblockEmailAddresses(addresses) },
                                negativeButtonResId = android.R.string.cancel,
                            )
                        }
                    } is BatchOperationResult.PartialResult -> {
                        blockedAddressesList.removeAll(response.successValues)
                    }
                }
                adaptor.notifyDataSetChanged()
            } catch (e: SudoEmailClient.EmailBlocklistException) {
                showAlertDialog(
                    titleResId = R.string.unblock_addresses_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { listBlockedEmails() },
                    negativeButtonResId = android.R.string.cancel,
                )
            } finally {
                hideLoading()
            }
            binding.filter.visibility = View.VISIBLE
        }
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int = 0) {
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

    private fun configureRecyclerView() {
        adaptor = AddressBlocklistAdaptor(blockedAddressesList) { selected, address ->
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
