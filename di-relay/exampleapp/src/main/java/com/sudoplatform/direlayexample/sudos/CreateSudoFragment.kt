/*
 * Copyright © 2023 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.direlayexample.sudos

import android.content.Intent
import android.net.Uri
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
import com.sudoplatform.direlayexample.App
import com.sudoplatform.direlayexample.R
import com.sudoplatform.direlayexample.createLoadingAlertDialog
import com.sudoplatform.direlayexample.databinding.FragmentCreateSudoBinding
import com.sudoplatform.direlayexample.postboxes.PostboxesFragment
import com.sudoplatform.direlayexample.showAlertDialog
import com.sudoplatform.direlayexample.util.ObjectDelegate
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import com.sudoplatform.sudoprofiles.exceptions.SudoProfileException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.CoroutineContext

/**
 * This [CreateSudoFragment] presents a form so that a user can create a [Sudo].
 *
 * - Links From:
 *  - [SudosFragment]: A user chooses the "Create Sudo" option at the bottom of the list.
 *
 * - Links To:
 *  - [PostboxesFragment]: If a user successfully creates a [Sudo], the [PostboxesFragment] will be
 *   presented so the user can create postboxes with that [Sudo].
 */
class CreateSudoFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentCreateSudoBinding>()
    private val binding by bindingDelegate

    /** Toolbar [Menu] displaying title and create button. */
    private lateinit var toolbarMenu: Menu

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingDelegate.attach(FragmentCreateSudoBinding.inflate(inflater, container, false))
        with(binding.toolbar.root) {
            title = getString(R.string.create_sudo)
            inflateMenu(R.menu.nav_menu_with_create_button)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.create -> {
                        createSudo()
                    }
                }
                true
            }
            toolbarMenu = menu
        }
        app = requireActivity().application as App
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        binding.learnMoreButton.setOnClickListener {
            learnMore()
        }
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    /** Creates a [Sudo] from the [SudoProfilesClient] based on the submitted form inputs. */
    private fun createSudo() {
        val name = binding.editText.text.toString().trim()
        if (name.isEmpty()) {
            showAlertDialog(
                titleResId = R.string.enter_sudo_name,
                positiveButtonResId = android.R.string.ok
            )
            return
        }
        val sudo = Sudo(UUID.randomUUID().toString())
        sudo.label = name
        showLoading(R.string.creating_sudo)
        launch {
            try {
                val newSudo = withContext(Dispatchers.IO) {
                    app.sudoProfilesClient.createSudo(sudo)
                }

                showAlertDialog(
                    titleResId = R.string.success,
                    positiveButtonResId = android.R.string.ok,
                    onPositive = {
                        navController.navigate(
                            CreateSudoFragmentDirections.actionCreateSudoFragmentToPostboxesFragment(
                                newSudo,
                            )
                        )
                    }
                )
            } catch (e: SudoProfileException) {
                showAlertDialog(
                    titleResId = R.string.something_wrong,
                    message = e.localizedMessage ?: e.toString(),
                    positiveButtonResId = R.string.try_again,
                    onPositive = { createSudo() },
                    negativeButtonResId = android.R.string.cancel
                )
            } finally {
                hideLoading()
            }
        }
    }

    /** Navigates to a Sudo Platform web page when the "Learn More" button is pressed. */
    private fun learnMore() {
        val openUrl = Intent(Intent.ACTION_VIEW)
        openUrl.data = Uri.parse(getString(R.string.create_sudo_doc_url))
        startActivity(openUrl)
    }

    /**
     * Sets toolbar items and edit text field to enabled/disabled.
     *
     * @param isEnabled If true, toolbar items and edit text field will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        binding.editText.isEnabled = isEnabled
        toolbarMenu.getItem(0)?.isEnabled = isEnabled
    }

    /** Displays the loading [AlertDialog] indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int) {
        loading = createLoadingAlertDialog(textResId)
        loading?.show()
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
