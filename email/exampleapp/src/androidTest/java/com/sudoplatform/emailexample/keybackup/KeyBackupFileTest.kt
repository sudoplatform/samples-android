/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.keybackup

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sudoplatform.sudokeymanager.KeyManagerFactory
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.Base64

/**
 * Test reading, writing and decrypting key backup files.
 */
@RunWith(AndroidJUnit4::class)
class KeyBackupFileTest {
    companion object {
        private const val PASSWORD = "correct horse battery staple"
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    // Only the stateless crypto methods are used, so the app's key store isn't touched.
    private val keyManager = KeyManagerFactory(instrumentation.targetContext).createAndroidKeyManager("keyBackupFileTest")

    private val payload =
        KeyBackupPayload(
            sudoProfiles =
                KeyBackupPayload.SudoProfilesKeys(
                    currentKeyId = "KEY-2",
                    keys = listOf(sudoKey("KEY-1", 0), sudoKey("KEY-2", 32)),
                ),
            sudoEmail = KeyBackupPayload.SudoEmailKeys(byteArrayOf(1, 2, 3, 4)),
            userSubject = "test-subject",
        )

    @Test
    fun sealedBackupShouldOpenWithItsPassword() {
        val encoded = KeyBackupFile.seal(payload, PASSWORD, keyManager).encoded()

        val opened = KeyBackupFile.read(encoded).open(PASSWORD, keyManager)

        assertEquals(payload, opened)
    }

    @Test
    fun sealedBackupShouldUseTheSharedFormat() {
        val createdAt = Instant.parse("2026-11-03T01:02:03.456Z")
        val json = JSONObject(String(KeyBackupFile.seal(payload, PASSWORD, keyManager, createdAt).encoded()))

        assertEquals("sudoplatform-sample-key-backup", json.getString("format"))
        assertEquals(1, json.getInt("version"))
        assertEquals("android", json.getString("platform"))
        // iOS can't decode fractional seconds.
        assertEquals("2026-11-03T01:02:03Z", json.getString("createdAt"))
        assertEquals("PBKDF2-HMAC-SHA256", json.getJSONObject("kdf").getString("algorithm"))
        assertEquals(600_000, json.getJSONObject("kdf").getInt("iterations"))
        assertEquals(32, Base64.getDecoder().decode(json.getJSONObject("kdf").getString("salt")).size)
        assertEquals("AES-256-GCM", json.getJSONObject("cipher").getString("algorithm"))
        assertEquals(12, Base64.getDecoder().decode(json.getJSONObject("cipher").getString("nonce")).size)
    }

    @Test
    fun openShouldRejectTheWrongPassword() {
        val file = KeyBackupFile.read(KeyBackupFile.seal(payload, PASSWORD, keyManager).encoded())

        assertThrows<KeyBackupException.WrongPasswordOrDamaged> { file.open("wrong password", keyManager) }
    }

    @Test
    fun openShouldRejectAModifiedFile() {
        val json = JSONObject(String(KeyBackupFile.seal(payload, PASSWORD, keyManager).encoded()))
        val ciphertext = Base64.getDecoder().decode(json.getString("ciphertext"))
        ciphertext[0] = (ciphertext[0].toInt() xor 1).toByte()
        json.put("ciphertext", Base64.getEncoder().encodeToString(ciphertext))

        val file = KeyBackupFile.read(json.toString().toByteArray())

        assertThrows<KeyBackupException.WrongPasswordOrDamaged> { file.open(PASSWORD, keyManager) }
    }

    @Test
    fun readShouldRejectAFileThatIsNotABackup() {
        assertThrows<KeyBackupException.NotKeyBackup> { KeyBackupFile.read("not json".toByteArray()) }
        assertThrows<KeyBackupException.NotKeyBackup> {
            KeyBackupFile.read(sealedJson().put("format", "something-else").toString().toByteArray())
        }
    }

    @Test
    fun readShouldRejectANewerVersion() {
        val exception =
            assertThrows<KeyBackupException.NewerVersion> {
                KeyBackupFile.read(sealedJson().put("version", 2).toString().toByteArray())
            }
        assertEquals(2, exception.version)
    }

    @Test
    fun readShouldRejectUnsupportedParameters() {
        val json = sealedJson()
        json.getJSONObject("kdf").put("iterations", 1_000)

        assertThrows<KeyBackupException.UnsupportedParameters> { KeyBackupFile.read(json.toString().toByteArray()) }
    }

    @Test
    fun payloadShouldRecordTheUserNextToTheSdks() {
        val json = JSONObject(String(payload.encoded()))

        assertEquals("test-subject", json.getJSONObject("user").getString("subject"))
        assertEquals(
            setOf("sudoProfiles", "sudoEmail"),
            json
                .getJSONObject("sdks")
                .keys()
                .asSequence()
                .toSet(),
        )
        assertFalse(JSONObject(String(payload.copy(userSubject = null).encoded())).has("user"))
    }

    @Test
    fun payloadShouldSkipUnrecognisedSdks() {
        val json =
            JSONObject(String(payload.encoded())).apply {
                getJSONObject("sdks").put("sudoTelephony", JSONObject()).put("sudoVirtualCards", JSONObject())
            }

        val decoded = KeyBackupPayload.decode(json.toString().toByteArray())

        assertEquals(payload.sudoProfiles, decoded.sudoProfiles)
        assertEquals(payload.sudoEmail, decoded.sudoEmail)
        assertEquals(listOf("sudoTelephony", "sudoVirtualCards"), decoded.unrecognisedSdks)
    }

    @Test
    fun passwordShouldBeNormalisedBeforeDerivingTheKey() {
        val encoded = KeyBackupFile.seal(payload, "café", keyManager).encoded()

        val opened = KeyBackupFile.read(encoded).open("café", keyManager)

        assertFalse("café" == "café")
        assertEquals(payload, opened)
    }

    private fun sealedJson() = JSONObject(String(KeyBackupFile.seal(payload, PASSWORD, keyManager).encoded()))

    private fun sudoKey(
        id: String,
        firstByte: Int,
    ) = KeyBackupPayload.SudoProfilesKey(
        id = id,
        namespace = "ss",
        algorithm = "AES/CBC/PKCS7Padding",
        version = 1,
        key = Base64.getEncoder().encodeToString(ByteArray(32) { (it + firstByte).toByte() }),
    )
}
