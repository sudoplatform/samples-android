/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailfolders

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
import com.sudoplatform.emailexample.App
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.createLoadingAlertDialog
import com.sudoplatform.emailexample.databinding.FragmentEditCustomFolderBinding
import com.sudoplatform.emailexample.showAlertDialog
import com.sudoplatform.emailexample.util.ObjectDelegate
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudoemail.types.inputs.UpdateCustomEmailFolderInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * This [EditCustomFolderFragment] presents a form to edit a custom email folder.
 *
 * - Links From:
 *  - [EmailMessagesFragment]: A user selects the "Edit Custom Folder" button.
 */
class EditCustomFolderFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentEditCustomFolderBinding>()
    private val binding by bindingDelegate

    /** Toolbar [Menu] displaying title and delete button. */
    private lateinit var toolbarMenu: Menu

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: EditCustomFolderFragmentArgs by navArgs()

    /** Email address identifier belonging to the custom folder. */
    private lateinit var emailAddressId: String

    /** Reference to mail address. */
    private lateinit var emailAddress: String

    /** Reference to the id of the folder to edit */
    private lateinit var customFolderId: String

    /** The name of the custom folder */
    private lateinit var customFolderName: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingDelegate.attach(
            FragmentEditCustomFolderBinding.inflate(
                inflater,
                container,
                false,
            ),
        )
        with(binding.toolbar.root) {
            title = getString(R.string.edit_custom_folder)
            inflateMenu(R.menu.nav_menu_with_save_button)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.save -> {
                        editCustomFolder()
                    }
                }
                true
            }
            toolbarMenu = menu
        }
        app = requireActivity().application as App
        emailAddressId = args.emailAddressId
        emailAddress = args.emailAddress
        customFolderId = args.customFolderId
        customFolderName = args.customFolderName
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.editText.setText(customFolderName)
        navController = Navigation.findNavController(view)
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    /** Edits a custom [EmailFolder] from the [SudoEmailClient] based on form inputs. */
    private fun editCustomFolder() {
        val folderName = binding.editText.text.toString().trim()
        if (folderName.isEmpty()) {
            showAlertDialog(
                titleResId = R.string.enter_custom_folder_name,
                positiveButtonResId = android.R.string.ok,
            )
            return
        }
        showLoading(R.string.saving_custom_folder)
        launch {
            try {
                withContext(Dispatchers.IO) {
                    app.sudoEmailClient.updateCustomEmailFolder(
                        UpdateCustomEmailFolderInput(
                            emailAddressId = emailAddressId,
                            customFolderName = folderName,
                            emailFolderId = customFolderId,
                        ),
                    )
                }
                navController.navigate(
                    EditCustomFolderFragmentDirections.actionEditCustomFolderFragmentToEmailMessages(
                        emailAddressId = emailAddressId,
                        emailAddress = emailAddress,
                    ),
                )
            } catch (e: SudoEmailClient.EmailFolderException) {
                showAlertDialog(
                    titleResId = R.string.something_wrong,
                    message = e.localizedMessage ?: e.toString(),
                    positiveButtonResId = R.string.try_again,
                    onPositive = { editCustomFolder() },
                    negativeButtonResId = android.R.string.cancel,
                )
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * Sets toolbar items and edit text field to enabled/disabled.
     *
     * @param isEnabled [Boolean] If true, toolbar items and edit text field will be enabled.
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
