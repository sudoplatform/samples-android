/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import com.sudoplatform.emailexample.util.SimplifiedEmailMessage
import com.sudoplatform.sudoemail.types.EmailAddressPublicInfo
import com.sudoplatform.sudoemail.types.EmailAlias
import com.sudoplatform.sudoemail.types.EmailAliasStatus
import com.sudoplatform.sudoemail.types.EmailAttachment
import com.sudoplatform.sudoemail.types.EmailMessage
import com.sudoplatform.sudoemail.types.InternetMessageFormatHeader
import com.sudoplatform.sudoemail.types.inputs.SendEmailMessageInput
import com.sudoplatform.sudoemail.types.inputs.SendEmailMessageViaAliasInput
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/** How long a recipient field must be left unchanged before its encryption is looked up. */
const val ENCRYPTION_CHECK_DELAY_MS = 1000L

private val ADDRESS_SPEC_PATTERN = Regex("^.+@[^.].*\\.[a-z]{2,}$")
private val RFC822_ADDRESS_PATTERN = Regex("^.*<.+@[^.].*\\.[a-z]{2,}>$")

/** What the user has entered on the compose screen. Recipient fields are comma separated. */
data class MessageContent(
    val to: String = "",
    val cc: String = "",
    val bcc: String = "",
    val subject: String = "",
    val body: String = "",
)

/** Splits a comma separated recipient field into addresses, trimming each one. */
fun addressesToArray(addresses: String): List<EmailMessage.EmailAddress> = addresses.split(",").map { EmailMessage.EmailAddress(it.trim()) }

/** The addresses in a recipient field, or none if it is blank. */
fun recipients(field: String): List<EmailMessage.EmailAddress> = if (field.isBlank()) emptyList() else addressesToArray(field)

/** True if [address] is a bare address, or a display name followed by an address in angle brackets. */
fun isValidEmailAddress(address: String): Boolean =
    if (address.contains(" ")) {
        RFC822_ADDRESS_PATTERN.containsMatchIn(address)
    } else {
        ADDRESS_SPEC_PATTERN.containsMatchIn(address)
    }

/** True if every address in a comma separated recipient field is valid. */
fun isValidEmailAddressList(addresses: String): Boolean = addressesToArray(addresses).all { isValidEmailAddress(it.emailAddress) }

/** True if every one of [addresses] has a public key with encryption enabled. */
fun allAddressesEncrypted(
    addresses: List<String>,
    publicInfo: List<EmailAddressPublicInfo>,
): Boolean = addresses.all { address -> publicInfo.any { it.emailAddress == address && it.enableEncryption } }

/** Whether a recipient field's addresses can all receive end-to-end encrypted messages. */
enum class RecipientEncryption {
    EMPTY,

    /** The field has changed and hasn't been looked up yet. */
    PENDING,
    ENCRYPTED,
    NOT_ENCRYPTED,
}

/**
 * A recipient field's encryption: that of the last lookup if it was for the field's current
 * text, so that a lookup for text since changed is never shown.
 *
 * @param lookup the text last looked up and whether its addresses are all encrypted.
 */
fun recipientEncryption(
    text: String,
    lookup: Pair<String, Boolean>?,
): RecipientEncryption =
    when {
        text.isBlank() -> RecipientEncryption.EMPTY
        lookup == null || lookup.first != text -> RecipientEncryption.PENDING
        lookup.second -> RecipientEncryption.ENCRYPTED
        else -> RecipientEncryption.NOT_ENCRYPTED
    }

/** The encryption indicator shows once every non-empty recipient field is encrypted. */
fun showEncryptedIndicator(fields: List<RecipientEncryption>): Boolean =
    fields.any { it == RecipientEncryption.ENCRYPTED } &&
        fields.all { it == RecipientEncryption.ENCRYPTED || it == RecipientEncryption.EMPTY }

/**
 * [subject] formatted with [template], such as "Re: %1$s", unless it already starts with the
 * template's prefix.
 */
fun prefixedSubject(
    subject: String?,
    template: String,
): String {
    val prefix = template.substringBefore("%1\$s").trim()
    val original = subject.orEmpty()
    return if (prefix.isNotEmpty() && original.startsWith(prefix)) original else template.format(original)
}

/**
 * The fields for replying to or forwarding [message]: To is its sender, Cc its Cc, and the
 * subject and body are formatted with the given templates.
 *
 * @param body the message's body as plain text.
 */
fun replyOrForwardContent(
    message: EmailMessage,
    body: String,
    subjectTemplate: String,
    bodyTemplate: String,
): MessageContent =
    MessageContent(
        to = message.from.joinToString { it.emailAddress },
        cc = message.cc.joinToString { it.emailAddress },
        subject = prefixedSubject(message.subject, subjectTemplate),
        body = bodyTemplate.format(body),
    )

/** The fields restored from a draft, with each recipient list joined so it splits back the same. */
fun draftContent(draft: SimplifiedEmailMessage): MessageContent =
    MessageContent(
        to = draft.to.joinToString(", "),
        cc = draft.cc.joinToString(", "),
        bcc = draft.bcc.joinToString(", "),
        subject = draft.subject,
        body = draft.body,
    )

/** The aliases a message can be sent from: the enabled aliases that forward to [emailAddress]. */
fun sendFromAliases(
    aliases: List<EmailAlias>,
    emailAddress: String,
): List<EmailAlias> = aliases.filter { it.realAddress == emailAddress && it.status == EmailAliasStatus.ENABLED }

/** An entry in the "send from" dropdown. */
sealed interface SendFromOption {
    /** Send from the email address itself. */
    data object EmailAddress : SendFromOption

    data class Alias(
        val alias: EmailAlias,
    ) : SendFromOption
}

/** The "send from" options: the email address itself, then [aliases] in order. */
fun sendFromOptions(aliases: List<EmailAlias>): List<SendFromOption> =
    listOf(SendFromOption.EmailAddress) + aliases.map { SendFromOption.Alias(it) }

/** The text shown for a "send from" option. */
fun sendFromOptionLabel(
    option: SendFromOption,
    emailAddressLabel: String,
): String =
    when (option) {
        SendFromOption.EmailAddress -> emailAddressLabel
        is SendFromOption.Alias -> option.alias.aliasAddress
    }

fun buildMessageHeader(
    from: EmailMessage.EmailAddress,
    content: MessageContent,
): InternetMessageFormatHeader =
    InternetMessageFormatHeader(
        from = from,
        to = recipients(content.to),
        cc = recipients(content.cc),
        bcc = recipients(content.bcc),
        replyTo = emptyList(),
        subject = content.subject,
    )

/** IDs of the message being replied to or forwarded, if any. */
data class MessageReference(
    val replyingMessageId: String? = null,
    val forwardingMessageId: String? = null,
)

/** Input to send a message from an email address, with [sender] as its From. */
fun buildSendEmailMessageInput(
    emailAddressId: String,
    sender: EmailMessage.EmailAddress,
    content: MessageContent,
    attachments: List<EmailAttachment>,
    reference: MessageReference = MessageReference(),
): SendEmailMessageInput =
    SendEmailMessageInput(
        senderEmailAddressId = emailAddressId,
        emailMessageHeader = buildMessageHeader(sender, content),
        body = content.body,
        attachments = attachments,
        replyingMessageId = reference.replyingMessageId,
        forwardingMessageId = reference.forwardingMessageId,
    )

/** Input to send a message via [alias], with the alias's address as its From. */
fun buildSendEmailMessageViaAliasInput(
    alias: EmailAlias,
    content: MessageContent,
    attachments: List<EmailAttachment>,
    reference: MessageReference = MessageReference(),
): SendEmailMessageViaAliasInput =
    SendEmailMessageViaAliasInput(
        senderEmailAliasId = alias.id,
        emailMessageHeader = buildMessageHeader(EmailMessage.EmailAddress(alias.aliasAddress), content),
        body = content.body,
        attachments = attachments,
        replyingMessageId = reference.replyingMessageId,
        forwardingMessageId = reference.forwardingMessageId,
    )

/** A message ready to send, either from the email address or via one of its aliases. */
sealed interface SendRequest {
    data class FromEmailAddress(
        val input: SendEmailMessageInput,
    ) : SendRequest

    data class ViaAlias(
        val input: SendEmailMessageViaAliasInput,
    ) : SendRequest
}

/** The request to send a message: via [selectedAlias] if one is chosen, otherwise from the email address. */
fun buildSendRequest(
    emailAddressId: String,
    sender: EmailMessage.EmailAddress,
    selectedAlias: EmailAlias?,
    content: MessageContent,
    attachments: List<EmailAttachment>,
    reference: MessageReference = MessageReference(),
): SendRequest =
    if (selectedAlias != null) {
        SendRequest.ViaAlias(buildSendEmailMessageViaAliasInput(selectedAlias, content, attachments, reference))
    } else {
        SendRequest.FromEmailAddress(buildSendEmailMessageInput(emailAddressId, sender, content, attachments, reference))
    }

/** A top bar action on the compose screen. */
enum class ComposeAction {
    CANCEL_SCHEDULE,
    SCHEDULE,
    ATTACH,
    SAVE_DRAFT,
    SEND,
}

/** The top bar actions, in order: drafts can also be scheduled, or have their schedule cancelled. */
fun composeActions(
    mode: ComposeMode,
    isScheduled: Boolean,
): List<ComposeAction> {
    val common = listOf(ComposeAction.ATTACH, ComposeAction.SAVE_DRAFT, ComposeAction.SEND)
    return when {
        mode != ComposeMode.DRAFT -> common
        isScheduled -> listOf(ComposeAction.CANCEL_SCHEDULE) + common
        else -> listOf(ComposeAction.SCHEDULE) + common
    }
}

/**
 * The time to send a scheduled draft, to the minute.
 *
 * @param pickerDateMillis the day picked in a Material date picker, which is midnight UTC.
 */
fun scheduledSendTime(
    pickerDateMillis: Long,
    hour: Int,
    minute: Int,
    timeZone: TimeZone = TimeZone.getDefault(),
): Date {
    val day = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = pickerDateMillis }
    return Calendar
        .getInstance(timeZone)
        .apply {
            clear()
            set(day.get(Calendar.YEAR), day.get(Calendar.MONTH), day.get(Calendar.DAY_OF_MONTH), hour, minute, 0)
        }.time
}
