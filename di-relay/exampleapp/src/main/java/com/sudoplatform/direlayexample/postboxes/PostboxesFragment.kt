/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.postboxes

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.direlayexample.App
import com.sudoplatform.direlayexample.R
import com.sudoplatform.direlayexample.databinding.FragmentPostboxesBinding
import com.sudoplatform.direlayexample.register.RegisterFragment
import com.sudoplatform.direlayexample.showAlertDialog
import com.sudoplatform.direlayexample.swipe.SwipeLeftActionHelper
import com.sudoplatform.direlayexample.util.ObjectDelegate
import com.sudoplatform.sudodirelay.SudoDIRelayClient
import com.sudoplatform.sudodirelay.types.Postbox
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudoprofiles.exceptions.SudoProfileException
import com.sudoplatform.sudouser.SudoUserClient
import com.sudoplatform.sudouser.exceptions.RegisterException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.util.UUID
import kotlin.coroutines.CoroutineContext

/**
 * Wrapper class to allow us to pass the postbox through to the PostboxFragment rather than
 * passing it by id and requiring it to be fetched again from the server.
 */
@Parcelize
data class PostboxWrapper(
    val postbox: @RawValue Postbox
) : Parcelable

/**
 * This [PostboxesFragment] presents a view showing the [Postbox.id]s of the users active postboxes,
 * which they can navigate to, and an option to create a new relay postbox.
 *
 * Links From:
 *  - [RegisterFragment]: A user clicked start.
 *
 * Links To:
 *  - [PostboxFragment]: Details about a postbox, including messages.
 */
class PostboxesFragment : Fragment(), CoroutineScope {

    companion object {
        const val POSTBOX_CREATE_AUDIENCE = "sudoplatform.relay.postbox"
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentPostboxesBinding>()
    private val binding by bindingDelegate

    /** Toolbar [Menu] displaying title and toolbar items. */
    private lateinit var toolbarMenu: Menu

    /** A reference to the [RecyclerView.Adapter] handling Postbox data. */
    private lateinit var adapter: PostboxAdapter

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** A mutable list of Postboxes. */
    private var postboxList = mutableListOf<Postbox>()

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: PostboxesFragmentArgs by navArgs()

    /** A [Sudo] used to retrieve the ownership proof. */
    private lateinit var sudo: Sudo

    /** The ownership proof used to tie a [Sudo] to a Postbox. */
    private lateinit var ownershipProof: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingDelegate.attach(FragmentPostboxesBinding.inflate(inflater, container, false))
        app = requireActivity().application as App
        sudo = args.sudo
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView()
        navController = Navigation.findNavController(view)
        configureToolbar(binding.toolbar.root)

        binding.createPostboxButton.setOnClickListener {
            createPostbox()
        }
    }

    override fun onResume() {
        super.onResume()
        launch {
            setItemsEnabled(false)
            showLoading(R.string.loading_postboxes)

            updatePostboxesForSudo()
            getOwnershipProof()

            setItemsEnabled(true)
            hideLoading()
        }
    }

    /**
     * Retrieve the ownership proof used to bind the [Sudo] and Postbox together.
     */
    private suspend fun getOwnershipProof() {
        try {
            ownershipProof = withContext(Dispatchers.IO) {
                app.sudoProfilesClient.getOwnershipProof(sudo, POSTBOX_CREATE_AUDIENCE)
            }
        } catch (e: SudoProfileException) {
            showAlertDialog(
                titleResId = R.string.ownership_proof_error,
                message = e.localizedMessage ?: "$e",
                negativeButtonResId = android.R.string.cancel
            )
        }
    }

    /**
     * Configures the [RecyclerView] used to display the listed Postbox items and listens to
     * item select events to navigate to the [PostboxFragment] for the selected postbox.
     */
    private fun configureRecyclerView() {
        adapter =
            PostboxAdapter(postboxList) { postbox ->
                launch {
                    navigateToMessages(postbox)
                }
            }

        binding.postboxRecyclerView.adapter = adapter
        binding.postboxRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        configureSwipeToDelete()
    }

    /**
     * Navigate to the [PostboxFragment] with the
     * [postbox] as navigation argument.
     *
     * @param postbox the selected postbox.
     */
    private fun navigateToMessages(postbox: Postbox) {
        navController.navigate(
            PostboxesFragmentDirections.actionPostboxesFragmentToPostboxFragment(
                postbox = PostboxWrapper(postbox)
            )
        )
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    /** Perform de-registration from the [SudoUserClient] and clear all local data. */
    private fun deregister() {
        launch {
            try {
                showLoading(R.string.deregistering)
                withContext(Dispatchers.IO) {
                    // wipe postboxes
                    app.sudoUserClient.deregister()
                }
                hideLoading()
                navController.navigate(
                    PostboxesFragmentDirections.actionPostboxesFragmentToRegisterFragment()
                )
            } catch (e: RegisterException) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.deregister_failure, e.localizedMessage),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /** Configures the toolbar menu. */
    private fun configureToolbar(toolbar: Toolbar) {
        toolbar.inflateMenu(R.menu.general_nav_menu)
        val sharedPreferences = context?.getSharedPreferences("SignIn", Context.MODE_PRIVATE)
        val usedFSSO = sharedPreferences?.getBoolean("usedFSSO", false)
        if (usedFSSO == true) {
            toolbar.menu.getItem(0)?.title = getString(R.string.sign_out)
        }
        with(toolbar) {
            title = getString(R.string.postboxes_title)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.deregister -> {
                        if (usedFSSO == true) {
                            launch {
                                app.doFSSOSignout()
                            }
                        } else {
                            showAlertDialog(
                                titleResId = R.string.deregister,
                                messageResId = R.string.deregister_confirmation,
                                positiveButtonResId = R.string.deregister,
                                onPositive = { deregister() },
                                negativeButtonResId = android.R.string.cancel
                            )
                        }
                    }
                }
                true
            }
            toolbarMenu = menu
        }
    }

    /**
     * Use the relay client to create a new postbox with a randomly generated UUIDv4 as the
     * connectionId. On successful creation, the postbox is added to the recycler view list, and
     * added to internal storage for later use.
     */
    private fun createPostbox() {
        launch {
            showLoading(R.string.creating_postbox)
            try {
                val connectionId = UUID.randomUUID().toString()
                app.logger.info("creating new postbox with ID: $connectionId")
                val newPostbox = app.diRelayClient.createPostbox(connectionId, ownershipProof)
                postboxList.add(newPostbox)
                adapter.notifyDataSetChanged()
            } catch (e: SudoDIRelayClient.DIRelayException) {
                showAlertDialog(
                    titleResId = R.string.postbox_creation_failed,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { createPostbox() },
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideLoading()
        }
    }

    /**
     * Gets all stored postboxes from the service
     */
    private suspend fun updatePostboxesForSudo() {
        postboxList.clear()
        try {
            postboxList.addAll(
                app.diRelayClient.listPostboxes().items
                    .filter { it.sudoId == sudo.id }
            )
            adapter.notifyDataSetChanged()
        } catch (e: Exception) {
            showAlertDialog(
                titleResId = R.string.fetch_postboxes_failure,
                message = e.localizedMessage ?: "$e",
                negativeButtonResId = android.R.string.ok
            )
        }
    }

    /**
     * Configures the swipe to delete action by listening to [RecyclerView.ViewHolder] swipe events
     * and drawing the swipe view and delete icon.
     *
     * Swiping in from the left will perform a delete operation and remove the item from the view.
     */
    private fun configureSwipeToDelete() {
        val itemTouchCallback = SwipeLeftActionHelper(requireContext(), onSwipedAction = ::onSwiped)
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.postboxRecyclerView)
    }

    private fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        launch {
            val postbox = postboxList[viewHolder.adapterPosition]
            deletePostbox(postbox.id)
            postboxList.removeAt(viewHolder.adapterPosition)
            adapter.notifyItemRemoved(viewHolder.adapterPosition)
        }
    }

    /**
     * Calls the relay client to delete the postbox with id of [postboxId].
     * The internal storage and keys related to the [postboxId] are cleaned up as well.
     *
     * @param postboxId The identifier of the postbox to be deleted
     */
    private suspend fun deletePostbox(postboxId: String) {
        try {
            showLoading(R.string.delete_postbox_pending)
            app.diRelayClient.deletePostbox(postboxId)
        } catch (e: SudoDIRelayClient.DIRelayException) {
            showAlertDialog(
                titleResId = R.string.postbox_delete_failed,
                message = e.localizedMessage ?: "$e",
                negativeButtonResId = android.R.string.ok
            )
        }
        hideLoading()
    }

    /**
     * Sets create postbox button to enabled/disabled
     *
     * @param isEnabled If true, create postbox button will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        binding.createPostboxButton.isEnabled = isEnabled
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int = 0) {
        binding.progressText2.setText(textResId)
        binding.progressBar3.visibility = View.VISIBLE
        binding.progressText2.visibility = View.VISIBLE
        setItemsEnabled(false)
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        if (bindingDelegate.isAttached()) {
            binding.progressBar3.visibility = View.GONE
            binding.progressText2.visibility = View.GONE
            setItemsEnabled(true)
        }
    }
}
