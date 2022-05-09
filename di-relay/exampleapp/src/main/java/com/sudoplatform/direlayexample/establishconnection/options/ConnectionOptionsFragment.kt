/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.establishconnection.options

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
import com.sudoplatform.direlayexample.databinding.FragmentConnectionOptionsBinding
import com.sudoplatform.direlayexample.establishconnection.invite.InviteFragment
import com.sudoplatform.direlayexample.establishconnection.scaninivitation.ScanInvitationFragment
import com.sudoplatform.direlayexample.postboxes.PostboxesFragment
import com.sudoplatform.direlayexample.util.ObjectDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlin.coroutines.CoroutineContext

/**
 * This [ConnectionOptionsFragment] presents the user with options for establishing a connection with
 *  a peer for the selected postbox. The options are to either create an invitation or scan an invitation.
 *
 * Links from:
 *  - [PostboxesFragment]: A user chooses a postbox from their available postboxes and that chosen
 *   postbox does not have a peer connection established yet.
 *
 * Links To:
 *  - [InviteFragment]: When the "Create Invitation" button is pressed, the [InviteFragment] will be
 *   displayed so that a user can produce an invitation qr code for a peer to scan from their sample
 *   app instance.
 *  - [ScanInvitationFragment]: When the "Scan Invitation" button is pressed, the [ScanInvitationFragment]
 *   will be displayed so that a user can scan the invitation qr code of a peer using the sample app.
 */
class ConnectionOptionsFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [App] that holds references to the APIs this fragment needs. */
    lateinit var app: App

    /** View binding to the views defined in the layout */
    private val bindingDelegate = ObjectDelegate<FragmentConnectionOptionsBinding>()
    private val binding by bindingDelegate

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: ConnectionOptionsFragmentArgs by navArgs()

    /** The selected postbox/connection Identifier. */
    private lateinit var connectionId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        connectionId = args.connectionId
        bindingDelegate.attach(FragmentConnectionOptionsBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.peer_connect_toolbar_title)
            inflateMenu(R.menu.general_nav_menu)
        }
        app = requireActivity().application as App
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        binding.inviteButton.setOnClickListener {
            navController.navigate(
                ConnectionOptionsFragmentDirections.actionConnectionOptionsFragmentToInviteFragment(
                    connectionId = connectionId
                )
            )
        }

        binding.scanInvitationButton.setOnClickListener {
            navController.navigate(
                ConnectionOptionsFragmentDirections.actionConnectionOptionsFragmentToScanInvitationFragment(
                    connectionId = connectionId
                )
            )
        }
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }
}
