package com.sudoplatform.passwordmanagerexample.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.sudoplatform.passwordmanagerexample.App
import com.sudoplatform.passwordmanagerexample.R
import com.sudoplatform.passwordmanagerexample.databinding.FragmentSecretCodeBinding
import com.sudoplatform.passwordmanagerexample.util.ObjectDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * This [SecretCodeFragment] presents the secret code and buttons that allow the user to download a
 * rescue kit PDF or copy the secret code.
 *
 * - Links From:
 *  - [SettingsFragment]: If a user taps on the "Show Secret Code" button they will be shown this view
 */
class SecretCodeFragment : Fragment(), CoroutineScope {

    companion object {
        const val SECRET_CODE_CLIPBOARD_LABEL = "secret_code"
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** View binding to the views defined in the layout */
    private val bindingDelegate = ObjectDelegate<FragmentSecretCodeBinding>()
    private val binding by bindingDelegate

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bindingDelegate.attach(FragmentSecretCodeBinding.inflate(inflater, container, false))
        binding.toolbar.root.title = getString(R.string.secret_code)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as App
        val secretCode = app.sudoPasswordManager.getSecretCode()
        binding.textViewSecretCode.text = secretCode

        binding.buttonCopy.setOnClickListener {
            saveSecretCodeToClipboard(secretCode, requireContext())
            Toast.makeText(
                requireContext(),
                getString(R.string.secret_code_copied),
                Toast.LENGTH_LONG
            ).show()
        }

        binding.buttonDownload.setOnClickListener {
            launch {
                try {
                    val rescueKitFile = renderRescueKitToFile(requireContext(), app.sudoPasswordManager)
                    shareRescueKit(requireContext(), rescueKitFile)
                } catch (e: Exception) {
                    app.logger.error(getString(R.string.registration_keys_failure))
                    app.logger.outputError(Error(e))
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.save_pdf_failure, e.localizedMessage),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        bindingDelegate.detach()
        super.onDestroy()
    }
}

fun saveSecretCodeToClipboard(secretCode: String?, context: Context) {
    if (secretCode.isNullOrBlank()) {
        return
    }
    val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(SecretCodeFragment.SECRET_CODE_CLIPBOARD_LABEL, secretCode))
}
