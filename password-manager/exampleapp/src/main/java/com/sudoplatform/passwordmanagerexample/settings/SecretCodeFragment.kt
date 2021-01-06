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
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.sudoplatform.passwordmanagerexample.App
import com.sudoplatform.passwordmanagerexample.R
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_secret_code.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_secret_code, container, false)
        val toolbar = (view.toolbar as Toolbar)
        toolbar.title = getString(R.string.secret_code)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as App
        val secretCode = app.sudoPasswordManager.getSecretCode()
        view.textView_secretCode.text = secretCode

        view.button_copy.setOnClickListener {
            saveSecretCodeToClipboard(secretCode, requireContext())
            Toast.makeText(
                requireContext(),
                getString(R.string.secret_code_copied),
                Toast.LENGTH_LONG
            ).show()
        }

        view.button_download.setOnClickListener {
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
