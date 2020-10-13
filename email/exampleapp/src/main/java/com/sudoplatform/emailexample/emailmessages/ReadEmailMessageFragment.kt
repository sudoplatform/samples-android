package com.sudoplatform.emailexample.emailmessages

import android.os.Bundle
import android.text.format.DateFormat
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
import com.sudoplatform.emailexample.MissingFragmentArgumentException
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.createLoadingAlertDialog
import com.sudoplatform.emailexample.showAlertDialog
import com.sudoplatform.emailexample.util.Rfc822MessageParser
import com.sudoplatform.emailexample.util.SimplifiedEmailMessage
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudoemail.types.CachePolicy
import com.sudoplatform.sudoemail.types.EmailMessage
import java.util.Date
import kotlin.coroutines.CoroutineContext
import kotlinx.android.synthetic.main.fragment_read_email_message.*
import kotlinx.android.synthetic.main.fragment_read_email_message.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This [ReadEmailMessageFragment] presents a view to allow a user to view the contents of an an
 * email message.
 *
 * - Links From:
 *  - [EmailMessagesFragment]: A user selects an [EmailMessage] from the list which will present this
 *   view with the contents of the selected [EmailMessage].
 *
 * - Links To:
 *  - [SendEmailMessageFragment]: If a user taps the "Reply" button on the top right of the toolbar,
 *   the [SendEmailMessageFragment] will be presented so that a user can compose a reply email message.
 */
class ReadEmailMessageFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /** Navigation controller used to manage app navigation. */
    private lateinit var navController: NavController

    /** Toolbar [Menu] displaying title and reply button. */
    private lateinit var toolbarMenu: Menu

    /** An [AlertDialog] used to indicate that an operation is occurring. */
    private lateinit var loading: AlertDialog

    /** Email Address used to compose a reply email message. */
    private lateinit var emailAddress: String

    /** Email Address Identifier used to compose a reply email message. */
    private lateinit var emailAddressId: String

    /** The selected email message. */
    private lateinit var emailMessage: EmailMessage

    /** The simplified email message containing the body. */
    private lateinit var emailMessageWithBody: SimplifiedEmailMessage

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_read_email_message, container, false)
        val toolbar = (view.toolbar as Toolbar)
        toolbar.title = getString(R.string.read_email_message)
        emailAddress = requireArguments().getString(getString(R.string.email_address))
            ?: throw MissingFragmentArgumentException("Email address missing")
        emailAddressId = requireArguments().getString(getString(R.string.email_address_id))
            ?: throw MissingFragmentArgumentException("Email address id missing")

        toolbar.inflateMenu(R.menu.nav_menu_with_reply_button)
        toolbar.setOnMenuItemClickListener {
            when (it?.itemId) {
                R.id.reply -> {
                    val bundle = bundleOf(
                        getString(R.string.email_address) to emailAddress,
                        getString(R.string.email_address_id) to emailAddressId,
                        getString(R.string.email_message) to emailMessage,
                        getString(R.string.simplified_email_message) to emailMessageWithBody
                    )
                    navController.navigate(R.id.action_readEmailMessageFragment_to_sendEmailMessageFragment, bundle)
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

        readEmailMessage()
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        coroutineContext.cancel()
        super.onDestroy()
    }

    /** Reads an email message from the [SudoEmailClient]. */
    private fun readEmailMessage() {
        val app = requireActivity().application as App
        emailMessage = requireArguments().getParcelable(getString(R.string.email_message))
            ?: throw MissingFragmentArgumentException("Email message missing")
        launch {
            try {
                showLoading(R.string.reading)
                val rfc822Data = withContext(Dispatchers.IO) {
                    app.sudoEmailClient.getEmailMessageRfc822Data(
                        emailMessage.messageId,
                        cachePolicy = CachePolicy.REMOTE_ONLY
                    )
                }
                if (rfc822Data != null) {
                    emailMessageWithBody = Rfc822MessageParser.parseRfc822Data(rfc822Data)
                    configureEmailMessageContents(emailMessage)
                }
            } catch (e: SudoEmailClient.EmailMessageException) {
                showAlertDialog(
                    titleResId = R.string.read_email_message_failure,
                    message = e.localizedMessage ?: "$e",
                    positiveButtonResId = R.string.try_again,
                    onPositive = { readEmailMessage() },
                    negativeButtonResId = android.R.string.cancel
                )
            }
            hideLoading()
        }
    }

    /**
     * Configures the view with the appropriate values from the email message contents.
     *
     * @param emailMessage Email message to configure the view with.
     */
    private fun configureEmailMessageContents(emailMessage: EmailMessage) {
        dateValue.text = formatDate(emailMessage.createdAt)
        fromValue.text = if (emailMessage.from.isNotEmpty()) emailMessage.from.first().toString() else ""
        toValue.text = if (emailMessage.to.isNotEmpty()) emailMessage.to.joinToString("\n") else ""
        ccValue.text = if (emailMessage.cc.isNotEmpty()) emailMessage.cc.joinToString("\n") else ""
        subject.text = emailMessage.subject
        contentBody.text = emailMessageWithBody.body
    }

    /**
     * Formats a [Date] to a presentable String.
     *
     * @param date The [Date] to be formatted.
     * @return A presentable [String] containing the date.
     */
    private fun formatDate(date: Date): String {
        return DateFormat.format("MM/dd/yyyy", date).toString()
    }

    /**
     * Sets toolbar items to enabled/disabled.
     *
     * @param isEnabled If true, toolbar items will be enabled.
     */
    private fun setItemsEnabled(isEnabled: Boolean) {
        toolbarMenu.getItem(0)?.isEnabled = isEnabled
    }

    /** Displays the loading [AlertDialog] indicating that an operation is occurring. */
    private fun showLoading(@StringRes textResId: Int) {
        loading = createLoadingAlertDialog(textResId)
        loading.show()
        contents?.visibility = View.GONE
        setItemsEnabled(false)
    }

    /** Dismisses the loading [AlertDialog] indicating that an operation has finished. */
    private fun hideLoading() {
        loading.dismiss()
        contents?.visibility = View.VISIBLE
        setItemsEnabled(true)
    }
}
