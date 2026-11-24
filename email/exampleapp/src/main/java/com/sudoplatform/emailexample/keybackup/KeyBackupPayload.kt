/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.keybackup

import org.json.JSONArray
import org.json.JSONObject

/** The decrypted contents of a key backup: each SDK's exported keys, keyed by SDK. */
data class KeyBackupPayload(
    val sudoProfiles: SudoProfilesKeys?,
    val sudoEmail: SudoEmailKeys?,
    /** Names of SDK entries in the backup that this app doesn't use, e.g. from a newer app. They're skipped on import. */
    val unrecognisedSdks: List<String> = emptyList(),
    /** The subject of the user whose keys these are, or null in a backup from before it was recorded. */
    val userSubject: String? = null,
) {
    data class SudoProfilesKeys(
        val currentKeyId: String,
        val keys: List<SudoProfilesKey>,
    )

    data class SudoProfilesKey(
        val id: String,
        val namespace: String,
        val algorithm: String,
        val version: Int,
        /** Base64 encoded raw key bytes. */
        val key: String,
    )

    /** The email SDK's own key archive, carried unchanged. */
    class SudoEmailKeys(
        val archive: ByteArray,
    ) {
        override fun equals(other: Any?): Boolean = other is SudoEmailKeys && archive.contentEquals(other.archive)

        override fun hashCode(): Int = archive.contentHashCode()
    }

    companion object {
        private const val SDKS = "sdks"
        private const val USER = "user"
        private const val SUBJECT = "subject"
        private const val SUDO_PROFILES = "sudoProfiles"
        private const val SUDO_EMAIL = "sudoEmail"
        private val KNOWN_SDKS = setOf(SUDO_PROFILES, SUDO_EMAIL)

        fun decode(data: ByteArray): KeyBackupPayload {
            val json = JSONObject(data.toString(Charsets.UTF_8))
            val sdks = json.getJSONObject(SDKS)
            val sudoProfiles =
                sdks.optJSONObject(SUDO_PROFILES)?.let { entry ->
                    val keys = entry.getJSONArray("keys")
                    SudoProfilesKeys(
                        currentKeyId = entry.getString("currentKeyId"),
                        keys = (0 until keys.length()).map { decodeKey(keys.getJSONObject(it)) },
                    )
                }
            val sudoEmail = sdks.optJSONObject(SUDO_EMAIL)?.let { SudoEmailKeys(it.getBase64("archive")) }
            val unrecognisedSdks =
                sdks
                    .keys()
                    .asSequence()
                    .filterNot { it in KNOWN_SDKS }
                    .sorted()
                    .toList()
            val userSubject = json.optJSONObject(USER)?.getString(SUBJECT)
            return KeyBackupPayload(sudoProfiles, sudoEmail, unrecognisedSdks, userSubject)
        }

        private fun decodeKey(json: JSONObject) =
            SudoProfilesKey(
                id = json.getString("id"),
                namespace = json.getString("namespace"),
                algorithm = json.getString("algorithm"),
                version = json.getInt("version"),
                key = json.getString("key"),
            )
    }

    fun encoded(): ByteArray {
        val sdks = JSONObject()
        sudoProfiles?.let { entry ->
            val keys = JSONArray()
            entry.keys.forEach { key ->
                keys.put(
                    JSONObject()
                        .put("id", key.id)
                        .put("namespace", key.namespace)
                        .put("algorithm", key.algorithm)
                        .put("version", key.version)
                        .put("key", key.key),
                )
            }
            sdks.put(SUDO_PROFILES, JSONObject().put("currentKeyId", entry.currentKeyId).put("keys", keys))
        }
        sudoEmail?.let { sdks.put(SUDO_EMAIL, JSONObject().putBase64("archive", it.archive)) }
        val json = JSONObject().put(SDKS, sdks)
        userSubject?.let { json.put(USER, JSONObject().put(SUBJECT, it)) }
        return json.toString().toByteArray(Charsets.UTF_8)
    }
}
