/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.postbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.sudoplatform.direlayexample.App
import com.sudoplatform.direlayexample.R
import com.sudoplatform.direlayexample.databinding.FragmentMessageDetailsBinding
import com.sudoplatform.direlayexample.showAlertDialog
import com.sudoplatform.direlayexample.util.ObjectDelegate
import com.sudoplatform.sudodirelay.types.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.lang.Exception
import kotlin.coroutines.CoroutineContext

/**
 * This [MessageFragment] presents a single [Message].
 *
 * Links From:
 *  - [PostboxFragment]: A user chooses a message from the list which will show this view with the
 *  [Message] details
 */
class MessageFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentMessageDetailsBinding>()
    private val binding by bindingDelegate

    /** Toolbar [Menu] displaying title and compose button. */
    private lateinit var toolbarMenu: Menu

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** The message of interest. */
    private lateinit var message: Message

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: MessageFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        message = args.message.message
        bindingDelegate.attach(FragmentMessageDetailsBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = message.id
            toolbarMenu = menu
        }
        app = requireActivity().application as App
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        displayMessageDetails()
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    private fun displayMessageDetails() {
        launch {
            try {
                binding.postboxId.text = message.postboxId
                binding.messageId.text = message.id
                binding.messageContents.text = message.message
                binding.messageCreatedAt.text = message.createdAt.toString()
                binding.messageOwner.text = message.ownerId
                binding.sudoOwner.text = message.sudoId
            } catch (e: Exception) {
                app.logger.error("error displaying message details: \n\t${e.localizedMessage}")
                showAlertDialog(
                    titleResId = R.string.message_details_failed,
                    message = e.localizedMessage ?: "$e",
                    negativeButtonResId = android.R.string.ok,
                )
            }
        }
    }
}
