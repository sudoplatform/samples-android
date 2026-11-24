/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.keybackup

import com.sudoplatform.sudokeymanager.KeyManagerInterface
import com.sudoplatform.sudokeymanager.KeyManagerInterface.SymmetricEncryptionAlgorithm
import org.json.JSONException
import org.json.JSONObject
import java.text.Normalizer
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Base64

/** Errors reading or writing a key backup file. */
sealed class KeyBackupException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    class NotKeyBackup(
        cause: Throwable? = null,
    ) : KeyBackupException("This file isn't a Sudo key backup.", cause)

    class NewerVersion(
        val version: Int,
    ) : KeyBackupException(
            "This backup was made by a newer version of the app (format version $version). Update the app and try again.",
        )

    class UnsupportedParameters : KeyBackupException("This backup uses encryption settings this app doesn't support.")

    class WrongPasswordOrDamaged(
        cause: Throwable? = null,
    ) : KeyBackupException("The password is wrong or the file is damaged. Check the password and try again.", cause)

    class CryptoFailure(
        cause: Throwable?,
    ) : KeyBackupException("The backup couldn't be encrypted or decrypted on this device.", cause)
}

/**
 * A password-protected key backup file, as defined by the Sudo Key Backup Format spec shared with
 * the iOS sample.
 *
 * The file is plain JSON describing how the payload was encrypted. The payload, a
 * [KeyBackupPayload], is only readable with the password.
 *
 * Only the stateless methods of [KeyManagerInterface] are used, so no keys are stored.
 */
class KeyBackupFile private constructor(
    val version: Int,
    val createdAt: Instant,
    val platform: String,
    val kdf: KeyDerivation,
    val cipher: Cipher,
    val ciphertext: ByteArray,
) {
    class KeyDerivation(
        val algorithm: String,
        val iterations: Int,
        val salt: ByteArray,
    )

    class Cipher(
        val algorithm: String,
        val nonce: ByteArray,
    )

    companion object {
        const val FORMAT = "sudoplatform-sample-key-backup"
        const val VERSION = 1
        const val PLATFORM = "android"
        const val KEY_DERIVATION_ALGORITHM = "PBKDF2-HMAC-SHA256"
        const val CIPHER_ALGORITHM = "AES-256-GCM"
        const val ITERATIONS = 600_000
        val SUPPORTED_ITERATIONS = 100_000..10_000_000
        private const val SALT_SIZE = 32
        private const val NONCE_SIZE = 12
        private const val TAG_SIZE = 16

        /**
         * Reads a backup file and checks it's one this app can decrypt, before asking the user
         * for the password.
         */
        fun read(data: ByteArray): KeyBackupFile {
            val file =
                try {
                    decode(JSONObject(data.toString(Charsets.UTF_8)))
                } catch (e: JSONException) {
                    throw KeyBackupException.NotKeyBackup(e)
                } catch (e: IllegalArgumentException) {
                    // Bad base64.
                    throw KeyBackupException.NotKeyBackup(e)
                } catch (e: DateTimeParseException) {
                    throw KeyBackupException.NotKeyBackup(e)
                }
            if (file.version > VERSION) {
                throw KeyBackupException.NewerVersion(file.version)
            }
            if (file.kdf.algorithm != KEY_DERIVATION_ALGORITHM ||
                file.kdf.iterations !in SUPPORTED_ITERATIONS ||
                file.cipher.algorithm != CIPHER_ALGORITHM ||
                file.cipher.nonce.size != NONCE_SIZE
            ) {
                throw KeyBackupException.UnsupportedParameters()
            }
            return file
        }

        /** Encrypts [payload] with a key derived from [password], using a new random salt and nonce. */
        fun seal(
            payload: KeyBackupPayload,
            password: String,
            keyManager: KeyManagerInterface,
            createdAt: Instant = Instant.now(),
        ): KeyBackupFile {
            val salt = keyManager.createRandomData(SALT_SIZE)
            val nonce = keyManager.createRandomData(NONCE_SIZE)
            val key = deriveKey(keyManager, password, salt, ITERATIONS)
            val ciphertext =
                try {
                    // GCM output is the ciphertext followed by the tag, the layout the file stores.
                    keyManager.encryptWithSymmetricKey(key, payload.encoded(), nonce, SymmetricEncryptionAlgorithm.AES_GCM_256)
                } catch (e: Exception) {
                    throw KeyBackupException.CryptoFailure(e)
                }
            return KeyBackupFile(
                version = VERSION,
                // iOS decodes ISO 8601 dates without fractional seconds only.
                createdAt = createdAt.truncatedTo(ChronoUnit.SECONDS),
                platform = PLATFORM,
                kdf = KeyDerivation(KEY_DERIVATION_ALGORITHM, ITERATIONS, salt),
                cipher = Cipher(CIPHER_ALGORITHM, nonce),
                ciphertext = ciphertext,
            )
        }

        /**
         * Derives the AES key with PBKDF2-HMAC-SHA256. The password is NFC normalised first so the
         * same characters typed on different keyboards or platforms give the same key. The
         * `ByteArray` overload is used because it hashes exactly these bytes, where the `String`
         * and `CharArray` overloads convert the characters themselves.
         */
        private fun deriveKey(
            keyManager: KeyManagerInterface,
            password: String,
            salt: ByteArray,
            iterations: Int,
        ): ByteArray {
            val passwordBytes = Normalizer.normalize(password, Normalizer.Form.NFC).toByteArray(Charsets.UTF_8)
            return try {
                keyManager.createSymmetricKeyFromPassword(passwordBytes, salt, iterations)
            } catch (e: Exception) {
                throw KeyBackupException.CryptoFailure(e)
            }
        }

        private fun decode(json: JSONObject): KeyBackupFile {
            if (json.getString("format") != FORMAT) {
                throw KeyBackupException.NotKeyBackup()
            }
            val kdf = json.getJSONObject("kdf")
            val cipher = json.getJSONObject("cipher")
            return KeyBackupFile(
                version = json.getInt("version"),
                createdAt = Instant.parse(json.getString("createdAt")),
                platform = json.getString("platform"),
                kdf = KeyDerivation(kdf.getString("algorithm"), kdf.getInt("iterations"), kdf.getBase64("salt")),
                cipher = Cipher(cipher.getString("algorithm"), cipher.getBase64("nonce")),
                ciphertext = json.getBase64("ciphertext"),
            )
        }
    }

    fun encoded(): ByteArray =
        JSONObject()
            .put("format", FORMAT)
            .put("version", version)
            .put("createdAt", DateTimeFormatter.ISO_INSTANT.format(createdAt))
            .put("platform", platform)
            .put(
                "kdf",
                JSONObject()
                    .put("algorithm", kdf.algorithm)
                    .put("iterations", kdf.iterations)
                    .putBase64("salt", kdf.salt),
            ).put(
                "cipher",
                JSONObject()
                    .put("algorithm", cipher.algorithm)
                    .putBase64("nonce", cipher.nonce),
            ).putBase64("ciphertext", ciphertext)
            .toString(2)
            .toByteArray(Charsets.UTF_8)

    /**
     * Decrypts the payload. A wrong password and a modified file both fail the GCM tag check, so
     * they can't be told apart.
     */
    fun open(
        password: String,
        keyManager: KeyManagerInterface,
    ): KeyBackupPayload {
        if (ciphertext.size < TAG_SIZE) {
            throw KeyBackupException.WrongPasswordOrDamaged()
        }
        val key = deriveKey(keyManager, password, kdf.salt, kdf.iterations)
        val plaintext =
            try {
                keyManager.decryptWithSymmetricKey(key, ciphertext, cipher.nonce, SymmetricEncryptionAlgorithm.AES_GCM_256)
            } catch (e: Exception) {
                throw KeyBackupException.WrongPasswordOrDamaged(e)
            }
        return try {
            KeyBackupPayload.decode(plaintext)
        } catch (e: JSONException) {
            throw KeyBackupException.WrongPasswordOrDamaged(e)
        } catch (e: IllegalArgumentException) {
            throw KeyBackupException.WrongPasswordOrDamaged(e)
        }
    }
}

internal fun JSONObject.getBase64(name: String): ByteArray = Base64.getDecoder().decode(getString(name))

internal fun JSONObject.putBase64(
    name: String,
    value: ByteArray,
): JSONObject = put(name, Base64.getEncoder().encodeToString(value))
