/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.keybackup

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sudoplatform.emailexample.App
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudokeymanager.KeyManagerFactory
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Base64

/**
 * Test moving Sudo and email keys between two devices, using the real SDK clients on key stores
 * kept apart from the app's own. No sign in is needed, as the keys never leave the device.
 */
@RunWith(AndroidJUnit4::class)
class KeyBackupServiceTest {
    companion object {
        private const val DATABASE = "keyBackupTest.db"
        private const val PASSWORD = "test password"
        private const val ADDRESS_KEY = "address-key"
        private const val TARGET_ADDRESS_KEY = "target-address-key"
        private const val SUBJECT = "test-subject"
    }

    private val app = ApplicationProvider.getApplicationContext<App>()

    // Only the stateless crypto methods are used, to seal backups the way another device would.
    private val keyManager = KeyManagerFactory(app).createAndroidKeyManager("keyBackupServiceTest")
    private val devices = mutableListOf<Device>()
    private val source = device("keyBackupSource")
    private val target = device("keyBackupTarget")

    private fun device(
        name: String,
        emailNamespace: String = "$name-eml",
    ) = Device(name, emailNamespace).also { devices.add(it) }

    /**
     * One device's Sudo and email key stores, signed in as [subject]. Email key archives import
     * only into the namespace they were exported from, so a device restoring another's email keys
     * uses that device's [emailNamespace].
     */
    private inner class Device(
        name: String,
        val emailNamespace: String,
        var subject: String? = SUBJECT,
    ) {
        val profilesClient: SudoProfilesClient =
            SudoProfilesClient
                .builder(app, app.sudoUserClient, Uri.fromFile(app.cacheDir))
                .setNamespace("$name-ss")
                .setDatabaseName(DATABASE)
                .setLogger(app.logger)
                .build()
        val emailKeyManager = KeyManagerFactory(app).createAndroidKeyManager(emailNamespace, DATABASE)
        val emailClient: SudoEmailClient =
            SudoEmailClient
                .builder()
                .setContext(app)
                .setSudoUserClient(app.sudoUserClient)
                .setKeyManager(emailKeyManager)
                .setLogger(app.logger)
                .build()
        val service = DefaultKeyBackupService(app, profilesClient, emailClient, app.logger) { subject }

        val sudoKeyIds get() = profilesClient.exportEncryptionKeys().map { it.id }.toSet()
    }

    @After
    fun fini() {
        runBlocking {
            for (device in devices) {
                device.profilesClient.reset()
                device.emailKeyManager.removeAllKeys()
            }
        }
    }

    @Test
    fun importShouldAddSudoKeysAndReplaceEmailKeys() =
        runBlocking {
            source.emailKeyManager.generateKeyPair(ADDRESS_KEY)
            val sourceAddressKey = source.emailKeyManager.getPrivateKeyData(ADDRESS_KEY)
            val sourceKeyId = source.profilesClient.getSymmetricKeyId()!!
            val backup = source.service.exportBackup(PASSWORD)
            // The restoring device uses the source's email namespace. The two share this emulator's key
            // store, so the source's email keys are removed before the restoring device creates its own.
            source.emailKeyManager.removeAllKeys()
            val restoring = device("keyBackupTarget", emailNamespace = source.emailNamespace)
            restoring.emailKeyManager.generateKeyPair(TARGET_ADDRESS_KEY)
            val targetKeyId = restoring.profilesClient.getSymmetricKeyId()!!

            val result = restoring.service.importBackup(restoring.service.readBackup(backup), PASSWORD)

            assertEquals(KeyBackupImportResult(sudoKeyCount = 1, importedEmailKeys = true, unrecognisedSdks = emptyList()), result)
            assertEquals(sourceKeyId, restoring.profilesClient.getSymmetricKeyId())
            // Sudos created on the target before the import stay readable.
            assertEquals(setOf(sourceKeyId, targetKeyId), restoring.sudoKeyIds)
            assertEquals(
                SUBJECT,
                restoring.service
                    .readBackup(backup)
                    .open(PASSWORD, keyManager)
                    .userSubject,
            )
            assertArrayEquals(sourceAddressKey, restoring.emailKeyManager.getPrivateKeyData(ADDRESS_KEY))
            assertNull(restoring.emailKeyManager.getPrivateKeyData(TARGET_ADDRESS_KEY))
        }

    @Test
    fun backupForAnotherUserShouldBeRejectedBeforeAnyKeysChange() =
        runBlocking {
            target.emailKeyManager.generateKeyPair(TARGET_ADDRESS_KEY)
            val targetKeyId = target.profilesClient.getSymmetricKeyId()!!
            val backup = source.service.exportBackup(PASSWORD)

            target.subject = "another-subject"
            assertThrows<KeyBackupServiceException.WrongUser> {
                target.service.importBackup(target.service.readBackup(backup), PASSWORD)
            }

            assertEquals(targetKeyId, target.profilesClient.getSymmetricKeyId())
            assertEquals(setOf(targetKeyId), target.sudoKeyIds)
            assertNotNull(target.emailKeyManager.getPrivateKeyData(TARGET_ADDRESS_KEY))
        }

    @Test
    fun backupWithoutAUserShouldImport() =
        runBlocking {
            source.subject = null
            val sourceKeyId = source.profilesClient.getSymmetricKeyId()!!
            val backup = source.service.exportBackup(PASSWORD)

            target.service.importBackup(target.service.readBackup(backup), PASSWORD)

            assertNull(
                target.service
                    .readBackup(backup)
                    .open(PASSWORD, keyManager)
                    .userSubject,
            )
            assertEquals(sourceKeyId, target.profilesClient.getSymmetricKeyId())
        }

    @Test
    fun deviceWithoutEmailKeysShouldExportOnlySudoKeys() =
        runBlocking {
            target.emailKeyManager.generateKeyPair(TARGET_ADDRESS_KEY)

            val backup = source.service.exportBackup(PASSWORD)
            val file = target.service.readBackup(backup)
            val result = target.service.importBackup(file, PASSWORD)

            assertNull(file.open(PASSWORD, keyManager).sudoEmail)
            assertFalse(result.importedEmailKeys)
            assertEquals(source.profilesClient.getSymmetricKeyId(), target.profilesClient.getSymmetricKeyId())
            assertNotNull(target.emailKeyManager.getPrivateKeyData(TARGET_ADDRESS_KEY))
        }

    @Test
    fun failedEmailImportShouldRestoreBothSetsOfKeys() =
        runBlocking {
            target.emailKeyManager.generateKeyPair(TARGET_ADDRESS_KEY)
            val targetKeyId = target.profilesClient.getSymmetricKeyId()!!
            val payload =
                KeyBackupPayload(
                    sudoProfiles = KeyBackupPayload.SudoProfilesKeys("NEW-KEY", listOf(sudoKey("NEW-KEY"))),
                    sudoEmail = KeyBackupPayload.SudoEmailKeys("not a key archive".toByteArray()),
                )
            val file = KeyBackupFile.seal(payload, PASSWORD, keyManager)

            assertThrows<SudoEmailClient.EmailCryptographicKeysException> {
                target.service.importBackup(file, PASSWORD)
            }

            assertEquals(targetKeyId, target.profilesClient.getSymmetricKeyId())
            assertEquals(setOf(targetKeyId), target.sudoKeyIds)
            assertNotNull(target.emailKeyManager.getPrivateKeyData(TARGET_ADDRESS_KEY))
        }

    @Test
    fun invalidSudoKeysShouldBeRejectedBeforeAnyKeysChange() =
        runBlocking {
            val targetKeyId = target.profilesClient.getSymmetricKeyId()!!
            val invalidBackups =
                listOf(
                    // The current key isn't in the backup.
                    KeyBackupPayload.SudoProfilesKeys("MISSING", listOf(sudoKey("KEY-1"))),
                    // The same id twice.
                    KeyBackupPayload.SudoProfilesKeys("KEY-1", listOf(sudoKey("KEY-1"), sudoKey("KEY-1"))),
                    // A key that isn't 256 bits.
                    KeyBackupPayload.SudoProfilesKeys("KEY-1", listOf(sudoKey("KEY-1", size = 16))),
                )

            for (sudoKeys in invalidBackups) {
                val file = KeyBackupFile.seal(KeyBackupPayload(sudoKeys, sudoEmail = null), PASSWORD, keyManager)
                assertThrows<KeyBackupServiceException.InvalidBackupContents> {
                    target.service.importBackup(file, PASSWORD)
                }
            }

            assertEquals(targetKeyId, target.profilesClient.getSymmetricKeyId())
            assertEquals(setOf(targetKeyId), target.sudoKeyIds)
        }

    @Test
    fun backupWithoutSudoKeysShouldBeRejected() =
        runBlocking {
            val file =
                KeyBackupFile.seal(
                    KeyBackupPayload(sudoProfiles = null, sudoEmail = KeyBackupPayload.SudoEmailKeys(byteArrayOf(1))),
                    PASSWORD,
                    keyManager,
                )

            assertThrows<KeyBackupServiceException.NoSudoKeysInBackup> {
                target.service.importBackup(file, PASSWORD)
            }
            assertTrue(target.sudoKeyIds.isNotEmpty())
        }

    private fun sudoKey(
        id: String,
        size: Int = 32,
    ) = KeyBackupPayload.SudoProfilesKey(
        id = id,
        namespace = "ss",
        algorithm = "AES/CBC/PKCS7Padding",
        version = 1,
        key = Base64.getEncoder().encodeToString(ByteArray(size) { it.toByte() }),
    )
}
