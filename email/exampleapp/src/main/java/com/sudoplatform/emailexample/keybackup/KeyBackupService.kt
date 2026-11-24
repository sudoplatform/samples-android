/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.keybackup

import android.content.Context
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudokeymanager.KeyManagerFactory
import com.sudoplatform.sudokeymanager.KeyManagerInterface
import com.sudoplatform.sudokeymanager.SecureKeyArchiveException
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudoprofiles.EncryptionKey
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.util.Base64

/** Errors exporting or importing this device's keys, as opposed to reading the file itself. */
sealed class KeyBackupServiceException(
    message: String,
) : Exception(message) {
    class NoKeysToExport : KeyBackupServiceException("There are no keys on this device to export. Sign in and try again.")

    class WrongUser : KeyBackupServiceException("This backup belongs to a different user. Sign in as that user to import it.")

    class NoSudoKeysInBackup : KeyBackupServiceException("This backup doesn't contain any Sudo keys.")

    class InvalidBackupContents : KeyBackupServiceException("This backup's contents are incomplete, so it can't be imported.")
}

/** The outcome of a successful import. */
data class KeyBackupImportResult(
    /** The number of Sudo encryption keys imported. */
    val sudoKeyCount: Int,
    /** True if the backup held email keys, which replaced this device's. */
    val importedEmailKeys: Boolean,
    /** SDK entries in the backup that this app doesn't use, which were skipped. */
    val unrecognisedSdks: List<String>,
)

/**
 * Backs up the keys this device uses to encrypt and decrypt user data, so they can be moved to
 * another device signed in as the same user: the Sudo keys, and the email keys that unseal each
 * email address's messages.
 */
interface KeyBackupService {
    /** Exports this device's keys as a backup file protected by [password]. */
    suspend fun exportBackup(password: String): ByteArray

    /** Reads a backup file and checks it can be imported, without needing the password. */
    fun readBackup(data: ByteArray): KeyBackupFile

    /** Decrypts a backup with [password] and adds its keys to this device's, unless it was exported by another user. */
    suspend fun importBackup(
        file: KeyBackupFile,
        password: String,
    ): KeyBackupImportResult
}

class DefaultKeyBackupService(
    context: Context,
    private val profilesClient: SudoProfilesClient,
    private val emailClient: SudoEmailClient,
    private val logger: Logger,
    /** Returns the signed in user's subject, which a backup records so it can't be imported for another user. */
    private val userSubject: () -> String?,
) : KeyBackupService {
    private companion object {
        const val SUDO_KEY_SIZE = 32
        const val KEY_NAMESPACE = "key-backup"
    }

    /** Used only for random data, password key derivation and encryption, so it stores no keys. */
    private val keyManager: KeyManagerInterface = KeyManagerFactory(context).createAndroidKeyManager(KEY_NAMESPACE)

    override suspend fun exportBackup(password: String): ByteArray {
        val currentKeyId = profilesClient.getSymmetricKeyId() ?: throw KeyBackupServiceException.NoKeysToExport()
        val keys =
            profilesClient.exportEncryptionKeys().map {
                KeyBackupPayload.SudoProfilesKey(it.id, it.namespace, it.algorithm, it.version, it.key)
            }
        if (keys.isEmpty()) {
            throw KeyBackupServiceException.NoKeysToExport()
        }
        val payload =
            KeyBackupPayload(
                sudoProfiles = KeyBackupPayload.SudoProfilesKeys(currentKeyId, keys),
                sudoEmail = exportEmailKeys()?.let { KeyBackupPayload.SudoEmailKeys(it) },
                userSubject = userSubject(),
            )
        return KeyBackupFile.seal(payload, password, keyManager).encoded()
    }

    override fun readBackup(data: ByteArray): KeyBackupFile = KeyBackupFile.read(data)

    /**
     * The Sudo keys are merged with this device's, so Sudos made here before the import stay
     * readable. The email SDK can only replace its keys, so a backup's email keys replace this
     * device's.
     */
    override suspend fun importBackup(
        file: KeyBackupFile,
        password: String,
    ): KeyBackupImportResult {
        val payload = file.open(password, keyManager)
        if (payload.userSubject != null && payload.userSubject != userSubject()) {
            throw KeyBackupServiceException.WrongUser()
        }
        val sudoKeys = payload.sudoProfiles?.takeIf { it.keys.isNotEmpty() } ?: throw KeyBackupServiceException.NoSudoKeysInBackup()
        val importedKeys = validate(sudoKeys)

        val existingKeys = profilesClient.exportEncryptionKeys()
        val existingCurrentKeyId = profilesClient.getSymmetricKeyId()
        val existingEmailArchive = payload.sudoEmail?.let { exportEmailKeys() }

        // Keys with the same id should be identical, so which copy wins doesn't matter.
        val mergedKeys = (existingKeys.associateBy { it.id } + importedKeys.associateBy { it.id }).values.toList()
        try {
            profilesClient.importEncryptionKeys(mergedKeys, sudoKeys.currentKeyId)
            payload.sudoEmail?.let { emailClient.importKeys(it.archive) }
        } catch (e: Exception) {
            // Both SDKs delete their existing keys before importing, so put them back rather than
            // leave this device unable to read its own data.
            withContext(NonCancellable) {
                restore(existingKeys, existingCurrentKeyId, existingEmailArchive)
            }
            throw e
        }
        return KeyBackupImportResult(
            sudoKeyCount = importedKeys.size,
            importedEmailKeys = payload.sudoEmail != null,
            unrecognisedSdks = payload.unrecognisedSdks,
        )
    }

    /**
     * Checks every key before any is imported. SudoProfiles deletes its keys and then adds the new
     * ones one at a time, so a bad key partway through would otherwise leave it with only some.
     * SudoProfiles on Android ignores the namespace, so keys from iOS import as they are.
     */
    private fun validate(sudoKeys: KeyBackupPayload.SudoProfilesKeys): List<EncryptionKey> {
        val ids = sudoKeys.keys.map { it.id }
        if (ids.toSet().size != ids.size || sudoKeys.currentKeyId !in ids) {
            throw KeyBackupServiceException.InvalidBackupContents()
        }
        return sudoKeys.keys.map { key ->
            val bytes =
                try {
                    Base64.getDecoder().decode(key.key)
                } catch (e: IllegalArgumentException) {
                    throw KeyBackupServiceException.InvalidBackupContents()
                }
            if (bytes.size != SUDO_KEY_SIZE) {
                throw KeyBackupServiceException.InvalidBackupContents()
            }
            EncryptionKey(key.id, key.namespace, key.key, key.algorithm, key.version)
        }
    }

    /**
     * Returns the email key archive, or null if this device has no email keys, e.g. no address
     * has been provisioned. The SDK refuses to archive an empty key store.
     */
    private suspend fun exportEmailKeys(): ByteArray? =
        try {
            emailClient.exportKeys()
        } catch (e: SudoEmailClient.EmailCryptographicKeysException) {
            val isEmpty =
                generateSequence<Throwable>(e) { it.cause }
                    .any { it is SecureKeyArchiveException && it.reason == SecureKeyArchiveException.ARCHIVE_EMPTY }
            if (!isEmpty) {
                throw e
            }
            null
        }

    private suspend fun restore(
        sudoKeys: List<EncryptionKey>,
        currentKeyId: String?,
        emailArchive: ByteArray?,
    ) {
        if (currentKeyId != null) {
            try {
                profilesClient.importEncryptionKeys(sudoKeys, currentKeyId)
            } catch (e: Exception) {
                logger.error("Failed to restore Sudo keys after a failed import: $e")
            }
        }
        if (emailArchive != null) {
            try {
                emailClient.importKeys(emailArchive)
            } catch (e: Exception) {
                logger.error("Failed to restore email keys after a failed import: $e")
            }
        }
    }
}
