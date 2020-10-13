/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.sudos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.sudoplatform.emailexample.App
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.createLoadingAlertDialog
import com.sudoplatform.emailexample.emailaddresses.EmailAddressesFragment
import com.sudoplatform.emailexample.showAlertDialog
import com.sudoplatform.sudoemail.types.EmailAddress
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import com.sudoplatform.sudoprofiles.exceptions.SudoProfileException
import java.util.UUID
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_create_sudo.*
import kotlinx.android.synthetic.main.fragment_create_sudo.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This [CreateSudoFragment] presents a form so that a user can create a [Sudo].
 *
 * - Links From:
 *  - [SudosFragment]: A user chooses the "Create Sudo" option at the bottom of the list.
 *
 * - Links To:
 *  - [EmailAddressesFragment]: If a user successfully creates a [Sudo], the [EmailAddressesFragment]
 *   will be presented so the user can create an [EmailAddress].
 */
class CreateSudoFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** Toolbar [Menu] displaying title and create button. */
    private lateinit var toolbarMenu: Menu

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private lateinit var loading: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_sudo, container, false)
        val toolbar = (view.toolbar as Toolbar)
        toolbar.title = getString(R.string.create_sudo)

        toolbar.inflateMenu(R.menu.nav_menu_with_create_button)
        toolbar.setOnMenuItemClickListener {
            when (it?.itemId) {
                R.id.create -> {
                    createSudo()
                }
            }
            true
        }
        toolbarMenu = toolbar.menu
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        view.learnMoreButton.setOnClickListener {
            learnMore()
        }
    }

    /** Creates a [Sudo] from the [SudoProfilesClient] based on the submitted form inputs. */
    private fun createSudo() {
        val name = editText.text.toString().trim()
        if (name.isEmpty()) {
            showAlertDialog(
                titleResId = R.string.enter_sudo_name,
                positiveButtonResId = android.R.string.ok
            )
            return
        }
        val app = requireActivity().application as App
        val sudo = Sudo(UUID.randomUUID().toString())
        sudo.label = name
        launch {
            try {
                showLoading(R.string.creating_sudo)
                val newSudo = withContext(Dispatchers.IO) {
                    app.sudoProfilesClient.createSudo(sudo)
                }

                showAlertDialog(
                    titleResId = R.string.success,
                    positiveButtonResId = android.R.string.ok,
                    onPositive = {
                        val bundle = bundleOf(
                            getString(R.string.sudo_id) to newSudo.id,
                            getString(R.string.sudo_label) to newSudo.label
                        )
                        navController.navigate(R.id.action_createSudoFragment_to_emailAddressesFragment, bundle)
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
            }
            hideLoading()
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
        editText.isEnabled = isEnabled
        toolbarMenu.getItem(0)?.isEnabled = isEnabled
    }

    /** Displays the loading [AlertDialog] indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int) {
        loading = createLoadingAlertDialog(textResId)
        loading.show()
        setItemsEnabled(false)
    }

    /** Dismisses the loading [AlertDialog] indicating that an operation has finished. */
    private fun hideLoading() {
        loading.dismiss()
        setItemsEnabled(true)
    }
}
