/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaddresses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.emailexample.App
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.createLoadingAlertDialog
import com.sudoplatform.emailexample.databinding.FragmentEmailAddressesBinding
import com.sudoplatform.emailexample.emailmessages.EmailMessagesFragment
import com.sudoplatform.emailexample.showAlertDialog
import com.sudoplatform.emailexample.sudos.CreateSudoFragment
import com.sudoplatform.emailexample.sudos.SudosFragment
import com.sudoplatform.emailexample.swipe.SwipeLeftActionHelper
import com.sudoplatform.emailexample.util.ObjectDelegate
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudoemail.types.CachePolicy
import com.sudoplatform.sudoemail.types.EmailAddress
import com.sudoplatform.sudoemail.types.ListAPIResult
import com.sudoplatform.sudoemail.types.inputs.ListEmailAddressesForSudoIdInput
import com.sudoplatform.sudoprofiles.Sudo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * This [EmailAddressesFragment] presents a list of [EmailAddress]es.
 *
 * - Links From:
 *  - [CreateSudoFragment]: A user chooses the "Create" option from the top right corner of the toolbar.
 *  - [SudosFragment]: A user selects a [Sudo] from the list which will show this view with the list
 *   of [EmailAddress]es created against this [Sudo].
 *
 * - Links To:
 *  - [ProvisionEmailAddressFragment]: If a user taps the "Create Email Address" button, the
 *   [ProvisionEmailAddressFragment] will be presented so that the user can add a new [EmailAddress]
 *   to their [Sudo].
 */
class EmailAddressesFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentEmailAddressesBinding>()
    private val binding by bindingDelegate

    /** A reference to the [RecyclerView.Adapter] handling [EmailAddress] data. */
    private lateinit var adapter: EmailAddressAdapter

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** A mutable list of [EmailAddress]es. */
    private var emailAddressList = mutableListOf<EmailAddress>()

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: EmailAddressesFragmentArgs by navArgs()

    /** A [Sudo] used to filter [EmailAddress]s. */
    private lateinit var sudo: Sudo

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingDelegate.attach(FragmentEmailAddressesBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.email_addresses)
        }
        app = requireActivity().application as App
        sudo = args.sudo!!
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView()
        navController = Navigation.findNavController(view)

        binding.createEmailAddressButton.setOnClickListener {
            navController.navigate(
                EmailAddressesFragmentDirections
                    .actionEmailAddressesFragmentToProvisionEmailAddressFragment(
                        sudo
                    )
            )
        }

        listEmailAddresses(CachePolicy.REMOTE_ONLY)
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    /**
     * List [EmailAddress]es from the [SudoEmailClient].
     *
     * @param cachePolicy [CachePolicy] Option of either retrieving [EmailAddress] data from the
     *  cache or network.
     */
    private fun listEmailAddresses(cachePolicy: CachePolicy) {
        launch {
            try {
                showLoading()
                val emailAddresses = withContext(Dispatchers.IO) {
                    val input = ListEmailAddressesForSudoIdInput(
                        sudoId = sudo.id!!,
                        cachePolicy = cachePolicy
                    )
                    app.sudoEmailClient.listEmailAddressesForSudoId(input)
                }
                when (emailAddresses) {
                    is ListAPIResult.Success -> {
                        emailAddressList.clear()
                        emailAddressList.addAll(emailAddresses.result.items)
                        adapter.notifyDataSetChanged()
                    }
                    is ListAPIResult.Partial -> {
                        val cause = emailAddresses.result.failed.first().cause
                        showAlertDialog(
                            titleResId = R.string.list_email_addresses_failure,
                            message = cause.localizedMessage ?: "$cause",
                            positiveButtonResId = R.string.try_again,
                            onPositive = { listEmailAddresses(CachePolicy.REMOTE_ONLY) },
                            negativeButtonResId = android.R.string.cancel
                        )
                    }
                }
            } catch (e: SudoEmailClient.EmailAddressException) {
                showAlertDialog(
                    titleResId = R.string.list_email_addresses_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { listEmailAddresses(CachePolicy.REMOTE_ONLY) },
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideLoading()
        }
    }

    /**
     * Deprovision a selected [EmailAddress] from the [SudoEmailClient].
     *
     * @param emailAddressId [String] The identifier of the [EmailAddress] to de-provision.
     */
    private fun deprovisionEmailAddress(emailAddressId: String) {
        launch {
            try {
                showDeleteAlert(R.string.deleting_email_address)
                withContext(Dispatchers.IO) {
                    app.sudoEmailClient.deprovisionEmailAddress(emailAddressId)
                }
                showAlertDialog(
                    titleResId = R.string.success,
                    positiveButtonResId = android.R.string.ok
                )
            } catch (e: SudoEmailClient.EmailAddressException) {
                showAlertDialog(
                    titleResId = R.string.deleting_email_address_failure,
                    message = e.localizedMessage ?: e.toString(),
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideDeleteAlert()
        }
    }

    /**
     * Configures the [RecyclerView] used to display the listed [EmailAddress] items and listens to
     * item select events to navigate to the [EmailMessagesFragment].
     */
    private fun configureRecyclerView() {
        adapter =
            EmailAddressAdapter(emailAddressList) { emailAddress ->
                navController.navigate(
                    EmailAddressesFragmentDirections
                        .actionEmailAddressesFragmentToEmailMessagesFragment(
                            emailAddress.emailAddress,
                            emailAddress.id
                        )
                )
            }

        binding.emailAddressRecyclerView.adapter = adapter
        binding.emailAddressRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        configureSwipeToDelete()
    }

    /**
     * Sets buttons and recycler view to enabled/disabled.
     *
     * @param isEnabled [Boolean] If true, buttons and recycler view will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        binding.createEmailAddressButton.isEnabled = isEnabled
        binding.emailAddressRecyclerView.isEnabled = isEnabled
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int = 0) {
        if (textResId != 0) {
            binding.progressText.text = getString(textResId)
        }
        binding.progressBar.visibility = View.VISIBLE
        binding.progressText.visibility = View.VISIBLE
        binding.emailAddressRecyclerView.visibility = View.GONE
        setItemsEnabled(false)
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        if (bindingDelegate.isAttached()) {
            binding.progressBar.visibility = View.GONE
            binding.progressText.visibility = View.GONE
            binding.emailAddressRecyclerView.visibility = View.VISIBLE
            setItemsEnabled(true)
        }
    }

    /** Displays the loading [AlertDialog] indicating that a deletion operation is occurring. */
    private fun showDeleteAlert(@StringRes textResId: Int) {
        loading = createLoadingAlertDialog(textResId)
        loading?.show()
    }

    /** Dismisses the loading [AlertDialog] indicating that a deletion operation has finished. */
    private fun hideDeleteAlert() {
        loading?.dismiss()
    }

    /**
     * Configures the swipe to delete action by listening to [RecyclerView.ViewHolder] swipe events
     * and drawing the swipe view and delete icon.
     *
     * Swiping in from the left will perform a delete operation and remove the item from the view.
     */
    private fun configureSwipeToDelete() {
        val itemTouchCallback = SwipeLeftActionHelper(requireContext(), onSwipedAction = ::onSwiped)
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.emailAddressRecyclerView)
    }

    private fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val emailAddress = emailAddressList[viewHolder.adapterPosition]
        deprovisionEmailAddress(emailAddress.id)
        emailAddressList.removeAt(viewHolder.adapterPosition)
        adapter.notifyItemRemoved(viewHolder.adapterPosition)
    }
}
