/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailmessages

import com.sudoplatform.emailexample.EmailMessageEvent
import com.sudoplatform.emailexample.util.SimplifiedEmailMessage
import com.sudoplatform.sudoemail.subscription.EmailMessageSubscriber
import com.sudoplatform.sudoemail.types.Direction
import com.sudoplatform.sudoemail.types.DraftEmailMessageWithContent
import com.sudoplatform.sudoemail.types.EmailFolder
import com.sudoplatform.sudoemail.types.EmailMessage
import com.sudoplatform.sudoemail.types.EncryptionStatus
import com.sudoplatform.sudoemail.types.ScheduledDraftMessage
import com.sudoplatform.sudoemail.types.ScheduledDraftMessageState
import java.util.Date

/** The most email message IDs the service deletes in one request. */
const val MAX_DELETE_BATCH_SIZE = 100

/** The names the service gives an email address's standard folders. */
object StandardFolderNames {
    const val INBOX = "INBOX"
    const val SENT = "SENT"
    const val TRASH = "TRASH"
}

/** An entry in the folder dropdown: one of the address's folders, or its drafts. */
sealed interface FolderOption {
    data class Folder(
        val folder: EmailFolder,
    ) : FolderOption

    /** Drafts aren't stored in a folder, so are listed separately. */
    data object Drafts : FolderOption
}

/** The name shown for a folder: the user's name for a custom folder, otherwise the service's. */
fun EmailFolder.displayName(): String = customFolderName?.takeIf { it.isNotEmpty() } ?: folderName

/** True for a folder the user created, which can be renamed and deleted. */
val EmailFolder.isCustom: Boolean get() = !customFolderName.isNullOrEmpty()

val EmailFolder.isTrash: Boolean get() = folderName == StandardFolderNames.TRASH

/** The folder selected when the screen opens, or when the selected folder is deleted. */
fun defaultFolder(folders: List<EmailFolder>): EmailFolder? =
    folders.firstOrNull { it.folderName == StandardFolderNames.INBOX } ?: folders.firstOrNull()

/** A row in the message list, for an email message or a draft. */
data class MessageListItem(
    val id: String,
    /** True for a received message, which shows who it is from rather than who it is to. */
    val isInbound: Boolean,
    val from: String,
    val to: List<String>,
    val subject: String?,
    val date: Date,
    val isEncrypted: Boolean,
    val hasAttachments: Boolean,
    /** When a scheduled draft will be sent. */
    val sendAt: Date? = null,
)

/** An address as shown in the list: the bare address when it has no display name. */
fun formatAddressForDisplay(address: EmailMessage.EmailAddress): String =
    if (address.displayName.isNullOrBlank() || address.displayName == "null") address.emailAddress else address.toString()

fun EmailMessage.toListItem(): MessageListItem =
    MessageListItem(
        id = id,
        isInbound = direction == Direction.INBOUND,
        from = from.firstOrNull()?.let(::formatAddressForDisplay).orEmpty(),
        to = to.map(::formatAddressForDisplay),
        subject = subject,
        date = createdAt,
        isEncrypted = encryptionStatus == EncryptionStatus.ENCRYPTED,
        hasAttachments = hasAttachments,
    )

/**
 * Whether a message in a folder belongs in this address's list: it was sent from or to the
 * address, or the address is one of its recipients.
 *
 * @param emailAddress the address itself, or null if it couldn't be loaded.
 */
fun isMessageForAddress(
    message: EmailMessage,
    emailAddressId: String,
    emailAddress: String?,
): Boolean {
    if (message.emailAddressId == emailAddressId) return true
    if (emailAddress == null) return false
    return (message.to + message.cc + message.bcc).any { it.emailAddress.equals(emailAddress, ignoreCase = true) }
}

fun sortNewestFirst(items: List<MessageListItem>): List<MessageListItem> = items.sortedByDescending { it.date }

/** The rows for a folder's messages: those for this address, newest first. */
fun folderMessageItems(
    messages: List<EmailMessage>,
    emailAddressId: String,
    emailAddress: String?,
): List<MessageListItem> =
    sortNewestFirst(
        messages
            .filter {
                isMessageForAddress(it, emailAddressId, emailAddress)
            }.map { it.toListItem() },
    )

/** A row for a draft, from its parsed RFC 822 content. Drafts are outbound and unencrypted. */
fun draftToListItem(
    draft: DraftEmailMessageWithContent,
    content: SimplifiedEmailMessage,
    sendAt: Date?,
): MessageListItem =
    MessageListItem(
        id = draft.id,
        isInbound = false,
        from = content.from.firstOrNull().orEmpty(),
        to = content.to,
        subject = content.subject,
        date = draft.updatedAt,
        isEncrypted = false,
        hasAttachments = false,
        sendAt = sendAt,
    )

/**
 * The rows for the drafts, newest first, each showing when it will be sent if it is scheduled.
 * Cancelled schedules are ignored.
 */
fun draftItems(
    drafts: List<Pair<DraftEmailMessageWithContent, SimplifiedEmailMessage>>,
    scheduledDrafts: List<ScheduledDraftMessage>,
): List<MessageListItem> {
    val sendAtById =
        scheduledDrafts
            .filter { it.state != ScheduledDraftMessageState.CANCELLED }
            .associate { it.id to it.sendAt }
    return sortNewestFirst(drafts.map { (draft, content) -> draftToListItem(draft, content, sendAtById[draft.id]) })
}

/**
 * The list after a change from the email message subscription, newest first.
 *
 * - CREATED: a received message is added to the inbox and a sent one to the sent folder. A sent
 *   message that was a scheduled draft removes the draft from the drafts list.
 * - UPDATED: the message replaces the one in the list, is added if it has moved into the shown
 *   folder, or is removed if it has moved out of it.
 * - DELETED: the message is removed.
 *
 * @param selected the folder being shown.
 */
fun applyMessageEvent(
    items: List<MessageListItem>,
    event: EmailMessageEvent,
    selected: FolderOption?,
    emailAddressId: String,
): List<MessageListItem> {
    val message = event.emailMessage
    val folder = (selected as? FolderOption.Folder)?.folder
    val index = items.indexOfFirst { it.id == message.id }
    val updated =
        when (event.type) {
            EmailMessageSubscriber.ChangeType.CREATED ->
                when {
                    selected == FolderOption.Drafts -> items.filterNot { it.id == message.id && it.sendAt != null }
                    folder != null && index < 0 && message.emailAddressId == emailAddressId && receivesNewMessage(folder, message) ->
                        items + message.toListItem()
                    else -> items
                }
            EmailMessageSubscriber.ChangeType.UPDATED ->
                when {
                    // Drafts aren't email messages, so updates don't apply to them.
                    folder == null -> items
                    message.folderId != folder.id -> if (index >= 0) items.filterNot { it.id == message.id } else items
                    index >= 0 -> items.toMutableList().also { it[index] = message.toListItem() }
                    else -> items + message.toListItem()
                }
            EmailMessageSubscriber.ChangeType.DELETED -> items.filterNot { it.id == message.id }
        }
    return sortNewestFirst(updated)
}

private fun receivesNewMessage(
    folder: EmailFolder,
    message: EmailMessage,
): Boolean =
    (message.direction == Direction.INBOUND && folder.folderName == StandardFolderNames.INBOX) ||
        (message.direction == Direction.OUTBOUND && folder.folderName == StandardFolderNames.SENT)

/** Message IDs split into requests the service accepts. */
fun deletionBatches(ids: List<String>): List<List<String>> = ids.chunked(MAX_DELETE_BATCH_SIZE)
