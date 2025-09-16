/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.sudoplatform.emailexample.App
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.createLoadingAlertDialog
import com.sudoplatform.emailexample.databinding.FragmentEmailAddressSettingsBinding
import com.sudoplatform.emailexample.emailmessages.EmailMessagesFragment
import com.sudoplatform.emailexample.showAlertDialog
import com.sudoplatform.emailexample.util.ObjectDelegate
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudoemail.setEmailNotificationsForAddressId
import com.sudoplatform.sudonotification.types.NotificationConfiguration
import com.sudoplatform.sudonotification.types.NotificationSettingsInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.coroutines.CoroutineContext

/**
 * This [EmailAddressSettingsFragment] presents a view to allow a user to manage their notification
 * settings.
 *
 * - Links From:
 *  - [EmailMessagesFragment]: If a user taps the "Settings" button on the top right of the toolbar,
 *   the [EmailAddressSettingsFragment] will be presented so that a user can manage their
 *   notification settings.
 */
class EmailAddressSettingsFragment :
    Fragment(),
    CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** The [App] that holds references to the APIs this fragment needs. */
    private lateinit var app: App

    /** View binding to the views defined in the layout. */
    private val bindingDelegate = ObjectDelegate<FragmentEmailAddressSettingsBinding>()
    private val binding by bindingDelegate

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** Fragment arguments handled by Navigation Library safe args */
    private val args: EmailAddressSettingsFragmentArgs by navArgs()

    /** Email Address Identifier used to identify and set the notification settings. */
    private lateinit var emailAddressId: String

    /** Current notification configuration */
    private lateinit var notificationConfiguration: NotificationConfiguration

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingDelegate.attach(FragmentEmailAddressSettingsBinding.inflate(inflater, container, false))
        app = requireActivity().application as App
        emailAddressId = args.emailAddressId

        val emServiceRules = app.notificationConfiguration.configs.filter { it.name == "emService" }
        var isToggled = true
        if (emServiceRules.isNotEmpty()) {
            // Looking for an "==" rule where 1st arg is our emailAddressId {"==" : [ { "var" : "meta.emailAddressId" }, emailAddressId ] }
            for (rule in emServiceRules) {
                val rules = rule.rules ?: continue
                val jsonRules = Json.decodeFromString<JsonObject>(rules)
                val equality = jsonRules["=="]
                if (equality is JsonArray && equality.size == 2) {
                    val lhs = equality[0]
                    val rhs = equality[1]

                    if (lhs is JsonObject && rhs is JsonPrimitive && rhs.isString) {
                        val v = lhs["var"]
                        if (v is JsonPrimitive && v.isString && v.content == "meta.emailAddressId" && rhs.content == emailAddressId) {
                            isToggled = rule.status == NotificationConfiguration.ENABLE_STR
                            break
                        }
                    }
                }
            }
        }
        binding.notificationsEnabledSwitch.isChecked = isToggled
        binding.notificationsEnabledSwitch.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            setNotificationsEnabledForEmailAddress(isChecked)
        }
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        readNotificationConfiguration()
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }

    /** Reads the [NotificationConfiguration] from the [SudoEmailClient]. */
    private fun readNotificationConfiguration() {
        launch {
            try {
                showLoading(R.string.loading_email_address_settings)

                notificationConfiguration =
                    withContext(Dispatchers.IO) {
                        app.sudoNotificationClient.getNotificationConfiguration(app.deviceInfo)
                    }
            } catch (e: SudoEmailClient.EmailMessageException) {
                showAlertDialog(
                    titleResId = R.string.load_email_address_settings_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { readNotificationConfiguration() },
                    negativeButtonResId = android.R.string.cancel,
                )
            }
            hideLoading()
        }
    }

    /**
     * Sets whether the notifications are enabled for the selected email address.
     *
     * @param enabled [Boolean] Flag indicating whether notifications are enabled.
     */
    private fun setNotificationsEnabledForEmailAddress(enabled: Boolean) {
        val newConfiguration =
            app.notificationConfiguration
                .setEmailNotificationsForAddressId(emailAddressId, enabled)

        launch {
            withContext(Dispatchers.IO) {
                app.sudoNotificationClient.setNotificationConfiguration(
                    NotificationSettingsInput(
                        bundleId = app.deviceInfo.bundleIdentifier,
                        deviceId = app.deviceInfo.deviceIdentifier,
                        services = listOf(app.sudoEmailNotifiableClient.getSchema()),
                        filter = newConfiguration.configs,
                    ),
                )
            }
        }
        app.notificationConfiguration = newConfiguration
    }

    /** Displays the loading [AlertDialog] indicating that an operation is occurring. */
    private fun showLoading(
        @StringRes textResId: Int,
    ) {
        loading = createLoadingAlertDialog(textResId)
        loading?.show()
    }

    /** Dismisses the loading [AlertDialog] indicating that an operation has finished. */
    private fun hideLoading() {
        loading?.dismiss()
    }
}
