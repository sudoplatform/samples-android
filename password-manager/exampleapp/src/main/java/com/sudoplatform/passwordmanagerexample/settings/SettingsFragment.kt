package com.sudoplatform.passwordmanagerexample.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.sudoplatform.passwordmanagerexample.App
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.createLoadingAlertDialog
import com.sudoplatform.passwordmanagerexample.showAlertDialog
import com.sudoplatform.sudouser.exceptions.RegisterException
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_settings.*
import kotlinx.android.synthetic.main.fragment_settings.view.*
import kotlinx.android.synthetic.main.fragment_settings.view.toolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This [SettingsFragment] presents a settings screen so that the user can navigate to different settings and screens.
 *
 * - Links From:
 *  - [VaultsFragment]: A user chooses the "Settings" option from the vault screen toolbar
 *
 * - Links To:
 *  - [SecretCodeFragment]: If a user taps on the "Show Secret Code" button they will be taken to
 *  the [SecretCodeFragment] so that they can view the secret code and rescue kit.
 */
class SettingsFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private var loading: AlertDialog? = null

    /** The [Application] that holds references to the APIs this fragment needs */
    private lateinit var app: App

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        app = requireActivity().application as App
        val toolbar = (view.toolbar as Toolbar)
        toolbar.title = getString(R.string.settings)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        view.changeMasterPasswordButton.setOnClickListener {
            navController.navigate(SettingsFragmentDirections.actionSettingsFragmentToChangeMasterPasswordFragment())
        }
        view.secretCodeButton.setOnClickListener {
            navController.navigate(SettingsFragmentDirections.actionSettingsFragmentToSecretCodeFragment())
        }
        view.passwordGeneratorButton.setOnClickListener {
            navController.navigate(SettingsFragmentDirections.actionSettingsFragmentToPasswordGeneratorDialogFragment())
        }
        view.lockVaultsButton.setOnClickListener {
            launch {
                withContext(Dispatchers.IO) {
                    app.sudoPasswordManager.lock()
                }
            }
            navController.navigate(SettingsFragmentDirections.actionSettingsFragmentToUnlockVaultsFragment())
        }
        view.deregisterButton.setOnClickListener {
            val sharedPreferences = context?.getSharedPreferences("SignIn", Context.MODE_PRIVATE)
            val usedFSSO = sharedPreferences?.getBoolean("usedFSSO", false)
            if (usedFSSO == true) {
                launch {
                    app.doFSSOSSignout()
                }
            } else {
                showAlertDialog(
                    titleResId = R.string.deregister_title,
                    messageResId = R.string.deregister_confirmation,
                    positiveButtonResId = R.string.deregister,
                    onPositive = { deregisterUser() },
                    negativeButtonResId = android.R.string.cancel
                )
            }
        }
        view.viewEntitlementsButton.setOnClickListener {
            navController.navigate(SettingsFragmentDirections.actionSettingsFragmentToViewEntitlementsFragment())
        }
        view.resetVaultsButton.setOnClickListener {
            showAlertDialog(
                titleResId = R.string.reset_vaults_alert_title,
                messageResId = R.string.reset_vaults_alert_message,
                positiveButtonResId = R.string.reset_vaults,
                onPositive = { deregisterPasswordManager() },
                negativeButtonResId = android.R.string.cancel
            )
        }
    }

    override fun onResume() {
        super.onResume()

        val sharedPreferences = context?.getSharedPreferences("SignIn", Context.MODE_PRIVATE)
        val usedFSSO = sharedPreferences?.getBoolean("usedFSSO", false)
        if (usedFSSO == true) {
            this.deregisterButton.setText(R.string.sign_out)
        }
    }

    override fun onDestroy() {
        loading?.dismiss()
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        super.onDestroy()
    }

    /** Perform deregistration of the [SudoUserClient] and clear local data (for TEST registration only) **/
    private fun deregisterUser() {
        launch {
            try {
                showLoading(R.string.deregistering)
                withContext(Dispatchers.IO) {
                    app.sudoUserClient.deregister()
                    app.sudoPasswordManager.reset()
                }
                hideLoading()
                navController.navigate(SettingsFragmentDirections.actionSettingsFragmentToRegisterFragment())
            } catch (error: Exception) {
                app.logger.error("Failed to deregister: $error")
                Toast.makeText(
                    requireContext(),
                    getString(R.string.deregister_failure, error.localizedMessage),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /** Perform deregistration of the [SudoPasswordManagerClient] */
    private fun deregisterPasswordManager() {
        launch {
            try {
                showLoading(R.string.deregistering)

                val secretCode = app.sudoPasswordManager.getSecretCode()
                saveSecretCodeToClipboard(secretCode, requireContext())

                withContext(Dispatchers.IO) {
                    app.sudoPasswordManager.deregister()
                    app.sudoPasswordManager.reset()
                }

                hideLoading()
                navController.navigate(SettingsFragmentDirections.actionSettingsFragmentToRegisterFragment())
            } catch (e: RegisterException) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.deregister_failure, e.localizedMessage),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Sets main menu buttons and toolbar items to enabled/disabled.
     *
     * @param isEnabled If true, buttons and toolbar items will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        secretCodeButton.isEnabled = isEnabled
        passwordGeneratorButton.isEnabled = isEnabled
        lockVaultsButton.isEnabled = isEnabled
        deregisterButton.isEnabled = isEnabled
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
        setItemsEnabled(true)
    }
}
