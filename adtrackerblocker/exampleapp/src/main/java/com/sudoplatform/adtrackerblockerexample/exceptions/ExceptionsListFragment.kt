/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.adtrackerblockerexample.exceptions

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.adtrackerblockerexample.App
import com.sudoplatform.adtrackerblockerexample.R
import com.sudoplatform.adtrackerblockerexample.createLoadingAlertDialog
import com.sudoplatform.adtrackerblockerexample.databinding.FragmentExceptionsListBinding
import com.sudoplatform.adtrackerblockerexample.databinding.LayoutExceptionUrlDialogBinding
import com.sudoplatform.adtrackerblockerexample.showAlertDialog
import com.sudoplatform.adtrackerblockerexample.swipe.SwipeLeftActionHelper
import com.sudoplatform.adtrackerblockerexample.util.ObjectDelegate
import com.sudoplatform.sudoadtrackerblocker.SudoAdTrackerBlockerException
import com.sudoplatform.sudoadtrackerblocker.types.BlockingException
import com.sudoplatform.sudoadtrackerblocker.types.toHostException
import com.sudoplatform.sudoadtrackerblocker.types.toPageException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * This [ExceptionsListFragment] presents a list of exceptions to the blocking rules.
 *
 * - Links From:
 *  - [RulesetsFragment]: From the menu on the [RulesetsFragment]
 */
class ExceptionsListFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** View binding to the views defined in the layout */
    private val bindingDelegate = ObjectDelegate<FragmentExceptionsListBinding>()
    private val binding by bindingDelegate

    /** A reference to the [RecyclerView.Adapter] handling exceptions. */
    private lateinit var listAdapter: ExceptionsListAdapter

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private lateinit var loading: AlertDialog

    /** A mutable list of exceptions. */
    private val exceptionList = mutableListOf<BlockingException>()

    /** The [Application] that holds references to the APIs this fragment needs */
    private lateinit var app: App

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bindingDelegate.attach(FragmentExceptionsListBinding.inflate(inflater, container, false))

        app = requireActivity().application as App

        with(binding.toolbar.root) {
            title = getString(R.string.exceptions_list)

            inflateMenu(R.menu.nav_menu_exceptions_list_menu)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.removeAllExceptions -> {
                        removeAllExceptions()
                    }
                }
                true
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView()

        binding.floatingActionButton.setOnClickListener {
            promptForException()
        }
    }

    override fun onResume() {
        super.onResume()
        loadExceptionsList()
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    /**
     * Get exceptions from the [SudoAdTrackerBlockerClient].
     */
    private fun loadExceptionsList() {
        launch {
            try {
                showLoading(R.string.loading_exceptions)
                val exceptions = withContext(Dispatchers.IO) {
                    app.adTrackerBlockerClient.getExceptions()
                }
                exceptionList.clear()
                if (exceptions.isNotEmpty()) {
                    exceptionList.addAll(exceptions)
                }
                listAdapter.notifyDataSetChanged()
            } catch (e: SudoAdTrackerBlockerException) {
                showAlertDialog(
                    titleResId = R.string.list_exceptions_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { loadExceptionsList() },
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideLoading()
        }
    }

    /**
     * Remove a selected exception from the [SudoAdTrackerBlockerClient].
     *
     * @param exception The selected exception to remove.
     */
    private fun removeException(exception: BlockingException) {
        launch {
            try {
                showRemoveAlert(R.string.removing_exception)
                withContext(Dispatchers.IO) {
                    app.adTrackerBlockerClient.removeExceptions(exception)
                }
            } catch (e: SudoAdTrackerBlockerException) {
                showAlertDialog(
                    titleResId = R.string.remove_exceptions_failure,
                    message = e.localizedMessage ?: "$e",
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideRemoveAlert()
        }
    }

    /**
     * Configures the [RecyclerView] used to display the listed [BlockingException] items.
     */
    private fun configureRecyclerView() {
        listAdapter = ExceptionsListAdapter(exceptionList)
        binding.exceptionsRecyclerView.adapter = listAdapter
        binding.exceptionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.exceptionsRecyclerView.requestFocus()
        configureSwipeToDelete()
    }

    /**
     * Prompt for the URL of the exception with a simple alert dialog.
     */
    private fun promptForException() {
        val exceptionUrlView = LayoutExceptionUrlDialogBinding.inflate(layoutInflater)
        AlertDialog.Builder(requireContext())
            .setView(exceptionUrlView.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (!exceptionUrlView.exceptionUrlInput.text.isNullOrBlank()) {
                    addException(exceptionUrlView.exceptionUrlInput.text.toString())
                }
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .show()
    }

    /**
     * Create a [BlockingException] from the URL entered and add it to [SudoAdTrackerBlockerClient]
     */
    private fun addException(exceptionUrl: String) {
        try {
            var uri = Uri.parse(exceptionUrl)
            if (uri.scheme == null) {
                // There's no http:// or https:// on the front. Put something there to make
                // the Uri.parse method work properly.
                uri = Uri.parse("scheme://$exceptionUrl")
            }
            val exception = if (uri.path.isNullOrEmpty())
                toHostException(exceptionUrl)
            else
                toPageException(exceptionUrl)
            launch {
                withContext(Dispatchers.IO) {
                    app.adTrackerBlockerClient.addExceptions(exception)
                }
                loadExceptionsList()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            app.logger.outputError(Error(e))
        }
    }

    /**
     * Remove all the exceptions from the [SudoAdTrackerBlockerClient].
     */
    private fun removeAllExceptions() {
        launch {
            try {
                showRemoveAlert(R.string.removing_exception)
                withContext(Dispatchers.IO) {
                    app.adTrackerBlockerClient.removeAllExceptions()
                }
                exceptionList.clear()
                listAdapter.notifyDataSetChanged()
            } catch (e: SudoAdTrackerBlockerException) {
                showAlertDialog(
                    titleResId = R.string.remove_exceptions_failure,
                    message = e.localizedMessage ?: "$e",
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideRemoveAlert()
        }
    }

    /**
     * Sets buttons and recycler view to enabled/disabled.
     *
     * @param isEnabled If true, buttons and recycler view will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        binding.exceptionsRecyclerView.isEnabled = isEnabled
        binding.floatingActionButton.isEnabled = isEnabled
    }

    /** Displays the progress bar spinner indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int = 0) {
        if (textResId != 0) {
            binding.progressText.text = getString(textResId)
        }
        binding.progressBar.visibility = View.VISIBLE
        binding.progressText.visibility = View.VISIBLE
        binding.exceptionsRecyclerView.visibility = View.GONE
        binding.floatingActionButton.visibility = View.GONE
        setItemsEnabled(false)
    }

    /** Hides the progress bar spinner indicating that an operation has finished. */
    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.progressText.visibility = View.GONE
        binding.exceptionsRecyclerView.visibility = View.VISIBLE
        binding.floatingActionButton.visibility = View.VISIBLE
        setItemsEnabled(true)
    }

    /** Displays the loading [AlertDialog] indicating that a remove operation is occurring. */
    private fun showRemoveAlert(@StringRes textResId: Int) {
        loading = createLoadingAlertDialog(textResId)
        loading.show()
    }

    /** Dismisses the loading [AlertDialog] indicating that a remove operation has finished. */
    private fun hideRemoveAlert() {
        loading.dismiss()
    }

    /**
     * Configures the swipe to remove action by listening to [RecyclerView.ViewHolder] swipe events
     * and drawing the swipe view and remove icon.
     *
     * Swiping in from the left will perform a remove operation and remove the item from the view.
     */
    private fun configureSwipeToDelete() {
        val itemTouchCallback = SwipeLeftActionHelper(requireContext(), onSwipedAction = ::onSwiped)
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.exceptionsRecyclerView)
    }

    private fun onSwiped(viewHolder: RecyclerView.ViewHolder) {
        val url = exceptionList[viewHolder.adapterPosition]
        removeException(url)
        exceptionList.removeAt(viewHolder.adapterPosition)
        listAdapter.notifyItemRemoved(viewHolder.adapterPosition)
    }
}
