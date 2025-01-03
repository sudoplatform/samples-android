/*
 * Copyright © 2025 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.util

import jakarta.mail.Message
import jakarta.mail.MessagingException
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * A factory that creates RFC 822 compatible email message content.
 * Note that the email message content created is also compatible with the more recent RFC 6854
 * standard which supersedes RFC 822.
 */
internal object Rfc822MessageFactory {

    /**
     * Construct an RFC 822 compatible mail message that has a subject and text body.
     */
    @Throws(MessagingException::class, IOException::class)
    fun makeRfc822Data(
        from: String,
        to: List<String>,
        cc: List<String>? = null,
        bcc: List<String>? = null,
        subject: String = "subject",
        body: String = "body",
    ): ByteArray {
        val session = Session.getDefaultInstance(System.getProperties(), null)

        val message = MimeMessage(session)
        message.setFrom(InternetAddress(from))
        to.map { if (it.isNotBlank()) message.addRecipient(Message.RecipientType.TO, InternetAddress(it)) }
        cc?.map { if (it.isNotBlank()) message.addRecipient(Message.RecipientType.CC, InternetAddress(it)) }
        bcc?.map { if (it.isNotBlank()) message.addRecipient(Message.RecipientType.BCC, InternetAddress(it)) }
        message.subject = subject

        val textPart = MimeBodyPart()
        textPart.setText(body, "UTF-8")
        textPart.setHeader("Content-Transfer-Encoding", "QUOTED-PRINTABLE")
        textPart.setHeader("Content-Disposition", "inline")
        textPart.setHeader("Content-Type", "text/plain")

        val bodyPart = MimeMultipart("related")
        bodyPart.addBodyPart(textPart)
        message.setContent(bodyPart)

        val byteStream = ByteArrayOutputStream()
        message.writeTo(byteStream)
        return byteStream.toByteArray()
    }
}
