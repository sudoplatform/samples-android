/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
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
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.emailexample.App
import com.sudoplatform.emailexample.MissingFragmentArgumentException
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.createLoadingAlertDialog
import com.sudoplatform.emailexample.emailmessages.EmailMessagesFragment
import com.sudoplatform.emailexample.showAlertDialog
import com.sudoplatform.emailexample.sudos.CreateSudoFragment
import com.sudoplatform.emailexample.sudos.SudosFragment
import com.sudoplatform.emailexample.swipe.SwipeLeftActionHelper
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudoemail.types.CachePolicy
import com.sudoplatform.sudoemail.types.EmailAddress
import com.sudoplatform.sudoprofiles.Sudo
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_email_addresses.*
import kotlinx.android.synthetic.main.fragment_email_addresses.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    /** A reference to the [RecyclerView.Adapter] handling [EmailAddress] data. */
    private lateinit var adapter: EmailAddressAdapter

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private lateinit var loading: AlertDialog

    /** A mutable list of [EmailAddress]es. */
    private var emailAddressList = mutableListOf<EmailAddress>()

    /** A [Sudo] identifier used to filter [EmailAddress]es. */
    private lateinit var sudoId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_email_addresses, container, false)
        val toolbar = (view.toolbar as Toolbar)
        toolbar.title = getString(R.string.email_addresses)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView(view)
        navController = Navigation.findNavController(view)
        sudoId = requireArguments().getString(getString(R.string.sudo_id))
            ?: throw MissingFragmentArgumentException("Sudo identifier missing")
        val sudoLabel = requireArguments().getString(getString(R.string.sudo_label))
            ?: throw MissingFragmentArgumentException("Sudo label missing")

        view.createEmailAddressButton.setOnClickListener {
            val bundle = bundleOf(
                getString(R.string.sudo_id) to sudoId,
                getString(R.string.sudo_label) to sudoLabel
            )
            navController.navigate(R.id.action_emailAddressesFragment_to_provisionEmailAddressFragment, bundle)
        }

        listEmailAddresses(CachePolicy.REMOTE_ONLY)
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        super.onDestroy()
    }

    /**
     * List [EmailAddress]es from the [SudoEmailClient].
     *
     * @param cachePolicy Option of either retrieving [EmailAddress] data from the cache or network.
     */
    private fun listEmailAddresses(cachePolicy: CachePolicy) {
        val app = requireActivity().application as App
        launch {
            try {
                showLoading()
                val emailAddresses = withContext(Dispatchers.IO) {
                    app.sudoEmailClient.listEmailAddresses(cachePolicy = cachePolicy)
                }
                emailAddressList.clear()
                for (emailAddress in emailAddresses.items) {
                    if (emailAddress.owners.all { it.id == sudoId }) {
                        emailAddressList.add(emailAddress)
                    }
                }
                adapter.notifyDataSetChanged()
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
     * De-provision a selected [EmailAddress] from the [SudoEmailClient].
     *
     * @param emailAddressId The identifier of the [EmailAddress] to de-provision.
     */
    private fun deprovisionEmailAddress(emailAddressId: String) {
        val app = requireActivity().application as App
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
    private fun configureRecyclerView(view: View) {
        adapter =
            EmailAddressAdapter(emailAddressList) { emailAddress ->
                val bundle = bundleOf(
                    getString(R.string.email_address) to emailAddress.emailAddress,
                    getString(R.string.email_address_id) to emailAddress.id
                )
                navController.navigate(R.id.action_emailAddressesFragment_to_emailMessagesFragment, bundle)
            }

        view.emailAddressRecyclerView.adapter = adapter
        view.emailAddressRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        configureSwipeToDelete()
    }

    /**
     * Sets buttons and recycler view to enabled/disabled.
     *
     * @param isEnabled If true, buttons and recycler view will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        createEmailAddressButton?.isEnabled = isEnabled
        emailAddressRecyclerView?.isEnabled = isEnabled
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int = 0) {
        if (textResId != 0) {
            progressText.text = getString(textResId)
        }
        progressBar.visibility = View.VISIBLE
        progressText.visibility = View.VISIBLE
        emailAddressRecyclerView?.visibility = View.GONE
        setItemsEnabled(false)
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        progressBar?.visibility = View.GONE
        progressText?.visibility = View.GONE
        emailAddressRecyclerView?.visibility = View.VISIBLE
        setItemsEnabled(true)
    }

    /** Displays the loading [AlertDialog] indicating that a deletion operation is occurring. */
    private fun showDeleteAlert(@StringRes textResId: Int) {
        loading = createLoadingAlertDialog(textResId)
        loading.show()
    }

    /** Dismisses the loading [AlertDialog] indicating that a deletion operation has finished. */
    private fun hideDeleteAlert() {
        loading.dismiss()
    }

    /**
     * Configures the swipe to delete action by listening to [RecyclerView.ViewHolder] swipe events
     * and drawing the swipe view and delete icon.
     *
     * Swiping in from the left will perform a delete operation and remove the item from the view.
     */
    private fun configureSwipeToDelete() {
        val itemTouchCallback = SwipeLeftActionHelper(requireContext(), onSwipedAction = ::onSwiped)
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(emailAddressRecyclerView)
    }

    private fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val emailAddress = emailAddressList[viewHolder.adapterPosition]
        deprovisionEmailAddress(emailAddress.id)
        emailAddressList.removeAt(viewHolder.adapterPosition)
        adapter.notifyItemRemoved(viewHolder.adapterPosition)
    }
}
