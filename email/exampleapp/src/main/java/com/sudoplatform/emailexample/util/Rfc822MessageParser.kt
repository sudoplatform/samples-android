/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.util

import android.os.Parcelable
import jakarta.mail.Message
import jakarta.mail.MessagingException
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import kotlinx.parcelize.Parcelize
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.util.Date

@Parcelize
data class SimplifiedEmailMessage(
    val id: String,
    val from: List<String>,
    val to: List<String>,
    val cc: List<String>,
    val bcc: List<String>,
    val subject: String,
    val body: String,
    val isDraft: Boolean = false,
    val sendAt: Date? = null,
) : Parcelable

/**
 * A parser of RFC 822 compatible email message content.
 */
internal object Rfc822MessageParser {
    private val session = Session.getDefaultInstance(System.getProperties(), null)

    private class LocalMimeMessage(
        session: Session,
        rfc822Ins: InputStream,
    ) : MimeMessage(session, rfc822Ins) {
        override fun toString(): String {
            val sender = from?.map { it.toString() } ?: emptyList()
            val toRecipients =
                getRecipients(Message.RecipientType.TO)?.map { it.toString() } ?: emptyList()
            val ccRecipients =
                getRecipients(Message.RecipientType.CC)?.map { it.toString() } ?: emptyList()
            val bccRecipients =
                getRecipients(Message.RecipientType.BCC)?.map { it.toString() } ?: emptyList()

            return buildString {
                if (dataHandler.dataSource.contentType.contains("text/plain")) {
                    appendLine(dataHandler.content)
                } else {
                    appendLine("MimeMessage [")
                    appendLine("  From: $sender")
                    appendLine("  To: $toRecipients")
                    appendLine("  Cc: $ccRecipients")
                    appendLine("  Bcc: $bccRecipients")
                    appendLine("  Subject: $subject")
                    appendLine("  Received: $receivedDate")
                    appendLine("  ContentType: $contentType")
                    appendLine("  Description: $description")
                    appendLine("  Encoding: $encoding")
                    appendLine("  Size: $size")
                    val multipart = MimeMultipart(dataHandler.dataSource)
                    appendLine("  Preamble: ${multipart.preamble}")
                    for (i in 0 until multipart.count) {
                        val bodyPart = multipart.getBodyPart(i)
                        appendLine("  Body Part $i [")
                        appendLine("    ContentType: ${bodyPart.contentType}")
                        appendLine("    Description: ${bodyPart.description}")
                        appendLine("    Size: ${bodyPart.size}")
                        if (bodyPart.contentType.contains("text/plain")) {
                            appendLine("    Content: ${bodyPart.content}")
                        }
                        appendLine("  ]")
                    }
                    appendLine("]")
                }
            }
        }
    }

    /**
     * Parse an RFC 822 compatible mail message.
     */
    @Throws(MessagingException::class, IOException::class)
    fun parseRfc822Data(rfc822Data: ByteArray): SimplifiedEmailMessage {
        val rfc822Ins = ByteArrayInputStream(rfc822Data)
        val javaMailMessage = LocalMimeMessage(session, rfc822Ins)
        return toSimplifiedEmailMessage(javaMailMessage)
    }

    @Throws(MessagingException::class, IOException::class)
    private fun toSimplifiedEmailMessage(message: Message): SimplifiedEmailMessage {
        val sender = message.from?.map { it.toString() } ?: emptyList()
        val toRecipients =
            message.getRecipients(Message.RecipientType.TO)?.map { it.toString() }
                ?: emptyList()
        val ccRecipients =
            message.getRecipients(Message.RecipientType.CC)?.map { it.toString() }
                ?: emptyList()
        val bccRecipients =
            message.getRecipients(Message.RecipientType.BCC)?.map { it.toString() }
                ?: emptyList()

        val body: String
        if (message.isMimeType("text/plain")) {
            body = buildString { append(message) }
        } else {
            val multipart = MimeMultipart(message.dataHandler.dataSource)
            body =
                buildString {
                    for (i in 0 until multipart.count) {
                        val bodyPart = multipart.getBodyPart(i)
                        if (bodyPart.isMimeType("text/plain")) {
                            append(bodyPart.content)
                        } else if (bodyPart.isMimeType("multipart/*")) {
                            val mimeMultipart = bodyPart.content as MimeMultipart
                            for (j in 0 until mimeMultipart.count) {
                                val mimeBodyPart = mimeMultipart.getBodyPart(j)
                                if (mimeBodyPart.isMimeType("text/plain")) {
                                    append(mimeBodyPart.content)
                                }
                            }
                        }
                    }
                }
        }

        return SimplifiedEmailMessage(
            id = "",
            from = sender,
            to = toRecipients,
            cc = ccRecipients,
            bcc = bccRecipients,
            subject = message.subject ?: "<No subject>",
            body = body,
        )
    }
}
