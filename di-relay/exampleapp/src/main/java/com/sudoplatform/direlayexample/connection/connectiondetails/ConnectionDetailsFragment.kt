/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.connection.connectiondetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.sudoplatform.direlayexample.App
import com.sudoplatform.direlayexample.R
import com.sudoplatform.direlayexample.databinding.FragmentConnectionDetailsBinding
import com.sudoplatform.direlayexample.showAlertDialog
import com.sudoplatform.direlayexample.util.ObjectDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * This [ConnectionDetailsFragment] presents metadata details about the selected postbox's connection.
 *
 * Links From:
 *  - [ConnectionFragment]: when a user taps "details" from a [ConnectionFragment], this
 *   [ConnectionDetailsFragment] displays details about connection that the [ConnectionFragment]
 *   refers to.
 */
class ConnectionDetailsFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [App] that holds references to the APIs this fragment needs. */
    lateinit var app: App

    /** View binding to the views defined in the layout */
    private val bindingDelegate = ObjectDelegate<FragmentConnectionDetailsBinding>()
    private val binding by bindingDelegate

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: ConnectionDetailsFragmentArgs by navArgs()

    /** The selected postbox/connection Identifier. */
    private lateinit var connectionId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        connectionId = args.connectionId
        bindingDelegate.attach(
            FragmentConnectionDetailsBinding.inflate(
                inflater,
                container,
                false
            )
        )
        with(binding.toolbar.root) {
            title = getString(R.string.details_toolbar_title)
            inflateMenu(R.menu.general_nav_menu)
        }
        app = requireActivity().application as App
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        loadAndSetDetails()
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    /**
     * Loads and sets all the metadata around the connection [connectionId] and the established peer.
     */
    private fun loadAndSetDetails() {
        launch {
            try {
                val myConnectionId = connectionId
                val myEndpoint = "${app.basePostboxEndpoint}$myConnectionId"
                val myPublicKey = withContext(Dispatchers.IO) {
                    app.keyManagement.getPublicKeyForConnection(myConnectionId)
                }

                binding.connectionId.text = myConnectionId
                binding.postboxEndpoint.text = myEndpoint
                binding.publicKey.text = myPublicKey

                if (app.connectionsStorage.isPeerConnected(connectionId)) {
                    // this should be true at this point in flow
                    val peerConnectionId = app.connectionsStorage.getPeerConnectionIdForConnection(connectionId)!!
                    val peerEndpoint = "${app.basePostboxEndpoint}$peerConnectionId"
                    val peerPublicKey = withContext(Dispatchers.IO) {
                        app.keyManagement.getPublicKeyForConnection(peerConnectionId)
                    }

                    binding.peerConnectionId.text = peerConnectionId
                    binding.peerPostboxEndpoint.text = peerEndpoint
                    binding.peerPublicKey.text = peerPublicKey
                }
            } catch (e: Exception) {
                app.logger.error("error getting connection details: \n\t${e.localizedMessage}")
                showAlertDialog(
                    titleResId = R.string.error_retrieving_details,
                    message = e.localizedMessage ?: "$e",
                    negativeButtonResId = android.R.string.ok,
                )
            }
        }
    }
}
