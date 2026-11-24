/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaliases

import com.sudoplatform.sudoemail.types.EmailAlias
import com.sudoplatform.sudoemail.types.EmailAliasRealAddressType
import com.sudoplatform.sudoemail.types.EmailAliasStatus
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** The longest email address the service accepts. */
private const val MAX_EMAIL_LENGTH = 254

/** An option in the dialog shown when the user taps an alias. */
enum class EmailAliasAction {
    ENABLE,
    DISABLE,
    UPDATE,
    CANCEL,
}

/** The aliases that belong to the Sudo with [sudoId]. */
fun aliasesOwnedBySudo(
    aliases: List<EmailAlias>,
    sudoId: String,
): List<EmailAlias> = aliases.filter { alias -> alias.owners.any { it.id == sudoId } }

/**
 * The aliases shown for the selected type: EXTERNAL aliases when [showExternal], otherwise
 * INTERNAL and UNKNOWN ones.
 */
fun filterAliasesByType(
    aliases: List<EmailAlias>,
    showExternal: Boolean,
): List<EmailAlias> = aliases.filter { isShownForType(it.realAddressType, showExternal) }

/** Whether an alias with [realAddressType] is listed when the [showExternal] type is selected. */
fun isShownForType(
    realAddressType: EmailAliasRealAddressType,
    showExternal: Boolean,
): Boolean =
    if (showExternal) {
        realAddressType == EmailAliasRealAddressType.EXTERNAL
    } else {
        realAddressType == EmailAliasRealAddressType.INTERNAL || realAddressType == EmailAliasRealAddressType.UNKNOWN
    }

/**
 * Whether an alias row shows a Verify button: only while external aliases are shown, for an
 * EXTERNAL alias that is still PENDING verification of its real address.
 */
fun showVerifyButton(
    showingExternal: Boolean,
    realAddressType: EmailAliasRealAddressType,
    status: EmailAliasStatus,
): Boolean = showingExternal && realAddressType == EmailAliasRealAddressType.EXTERNAL && status == EmailAliasStatus.PENDING

/**
 * The options offered for an alias with [status]: Disable for an ENABLED alias, Enable for a
 * DISABLED one, neither otherwise, then always Update and Cancel.
 */
fun aliasActionOptions(status: EmailAliasStatus): List<EmailAliasAction> {
    val toggle =
        when (status) {
            EmailAliasStatus.ENABLED -> EmailAliasAction.DISABLE
            EmailAliasStatus.DISABLED -> EmailAliasAction.ENABLE
            EmailAliasStatus.LOCKED,
            EmailAliasStatus.PENDING,
            EmailAliasStatus.UNKNOWN,
            -> null
        }
    return listOfNotNull(toggle, EmailAliasAction.UPDATE, EmailAliasAction.CANCEL)
}

/**
 * Parse alias metadata entered as a JSON object of string values.
 *
 * @return the entries, or null if [jsonString] isn't a JSON object or a value can't be read as a
 *  string.
 */
fun parseMetadataJson(jsonString: String): Map<String, String>? =
    try {
        val jsonObject = JSONObject(jsonString)
        val map = mutableMapOf<String, String>()
        jsonObject.keys().forEach { key ->
            map[key] = jsonObject.getString(key)
        }
        map
    } catch (e: Exception) {
        null
    }

/** Alias metadata as a JSON object, or an empty string when there is none. */
fun formatMetadataJson(metadata: Map<String, String>?): String =
    if (metadata.isNullOrEmpty()) "" else JSONObject(metadata as Map<*, *>).toString()

/**
 * Whether [email] looks like an email address: at most 254 characters, with exactly one `@`
 * between a non-empty local part and a domain containing a `.`.
 */
fun isValidEmail(email: String): Boolean {
    if (email.length > MAX_EMAIL_LENGTH) return false
    val atIndex = email.indexOf('@')
    if (atIndex < 0) return false
    if (email.indexOf('@', atIndex + 1) >= 0) return false
    val localPart = email.substring(0, atIndex)
    val domain = email.substring(atIndex + 1)
    if (localPart.isEmpty()) return false
    if (domain.isEmpty()) return false
    if (!domain.contains('.')) return false
    return true
}

/**
 * An alias's expiry for its row, such as "Expires: 06/15/2027". The service reports an alias
 * without an expiry as either no date or `Date(0)`, both shown as "Expires: -".
 *
 * @param label the text before the date.
 */
fun formatExpiresAtForDisplay(
    expiresAt: Date?,
    label: String = "Expires:",
): String {
    val date = expiresAt?.takeIf { it.time != 0L }
    return "$label ${date?.let(::formatExpiryDate) ?: "-"}"
}

/** A date in the MM/dd/yyyy form used for alias expiry. */
fun formatExpiryDate(date: Date): String = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(date)

/**
 * The value for a Material date picker showing the local calendar day of [date]. The picker
 * works in UTC midnights.
 */
fun expiryDateToPickerMillis(date: Date): Long {
    val local = Calendar.getInstance().apply { time = date }
    return Calendar
        .getInstance(TimeZone.getTimeZone("UTC"))
        .apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
}

/** The day picked in a Material date picker, at the current local time of day. */
fun pickerMillisToExpiryDate(millis: Long): Date {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
    return Calendar
        .getInstance()
        .apply { set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH)) }
        .time
}
