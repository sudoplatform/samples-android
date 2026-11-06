/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.keybackup

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sudoplatform.sudokeymanager.KeyManagerFactory
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import com.sudoplatform.sudotelephony.SudoTelephonyClient
import com.sudoplatform.telephonyexample.App
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
 * Test moving Sudo and telephony keys between two devices, using the real SDK clients on key
 * stores kept apart from the app's own. No sign in is needed, as the keys never leave the device.
 */
@RunWith(AndroidJUnit4::class)
class KeyBackupServiceTest {
    companion object {
        private const val DATABASE = "keyBackupTest.db"
        private const val PASSWORD = "test password"
        private const val NUMBER_KEY = "number-key"
        private const val TARGET_NUMBER_KEY = "target-number-key"
        private const val SUBJECT = "test-subject"
    }

    private val app = ApplicationProvider.getApplicationContext<App>()
    private val source = Device("keyBackupSource")
    private val target = Device("keyBackupTarget")

    /** One device's Sudo and telephony key stores, signed in as [subject]. */
    private inner class Device(
        name: String,
        var subject: String? = SUBJECT,
    ) {
        val profilesClient: SudoProfilesClient =
            SudoProfilesClient
                .builder(app, app.sudoUserClient, Uri.fromFile(app.cacheDir))
                .setNamespace("$name-ss")
                .setDatabaseName(DATABASE)
                .setLogger(app.logger)
                .build()
        val telephonyKeyManager = KeyManagerFactory(app).createAndroidKeyManager("$name-tel", DATABASE)
        val telephonyClient: SudoTelephonyClient =
            SudoTelephonyClient
                .builder()
                .setContext(app)
                .setSudoUserClient(app.sudoUserClient)
                .setKeyManager(telephonyKeyManager)
                .setLogger(app.logger)
                .build()
        val service = DefaultKeyBackupService(profilesClient, telephonyClient, app.keyManager, app.logger) { subject }

        val sudoKeyIds get() = profilesClient.exportEncryptionKeys().map { it.id }.toSet()
    }

    @After
    fun fini() {
        runBlocking {
            for (device in listOf(source, target)) {
                device.profilesClient.reset()
                device.telephonyKeyManager.removeAllKeys()
            }
        }
    }

    @Test
    fun importShouldAddSudoKeysAndReplaceTelephonyKeys() =
        runBlocking {
            source.telephonyKeyManager.generateKeyPair(NUMBER_KEY)
            target.telephonyKeyManager.generateKeyPair(TARGET_NUMBER_KEY)
            val sourceKeyId = source.profilesClient.getSymmetricKeyId()!!
            val targetKeyId = target.profilesClient.getSymmetricKeyId()!!

            val backup = source.service.exportBackup(PASSWORD)
            val result = target.service.importBackup(target.service.readBackup(backup), PASSWORD)

            assertEquals(KeyBackupImportResult(sudoKeyCount = 1, importedTelephonyKeys = true, unrecognisedSdks = emptyList()), result)
            assertEquals(sourceKeyId, target.profilesClient.getSymmetricKeyId())
            // Sudos created on the target before the import stay readable.
            assertEquals(setOf(sourceKeyId, targetKeyId), target.sudoKeyIds)
            assertEquals(
                SUBJECT,
                target.service
                    .readBackup(backup)
                    .open(PASSWORD, app.keyManager)
                    .userSubject,
            )
            assertArrayEquals(
                source.telephonyKeyManager.getPrivateKeyData(NUMBER_KEY),
                target.telephonyKeyManager.getPrivateKeyData(NUMBER_KEY),
            )
            assertNull(target.telephonyKeyManager.getPrivateKeyData(TARGET_NUMBER_KEY))
        }

    @Test
    fun backupForAnotherUserShouldBeRejectedBeforeAnyKeysChange() =
        runBlocking {
            target.telephonyKeyManager.generateKeyPair(TARGET_NUMBER_KEY)
            val targetKeyId = target.profilesClient.getSymmetricKeyId()!!
            val backup = source.service.exportBackup(PASSWORD)

            target.subject = "another-subject"
            assertThrows<KeyBackupServiceException.WrongUser> {
                target.service.importBackup(target.service.readBackup(backup), PASSWORD)
            }

            assertEquals(targetKeyId, target.profilesClient.getSymmetricKeyId())
            assertEquals(setOf(targetKeyId), target.sudoKeyIds)
            assertNotNull(target.telephonyKeyManager.getPrivateKeyData(TARGET_NUMBER_KEY))
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
                    .open(PASSWORD, app.keyManager)
                    .userSubject,
            )
            assertEquals(sourceKeyId, target.profilesClient.getSymmetricKeyId())
        }

    @Test
    fun deviceWithoutTelephonyKeysShouldExportOnlySudoKeys() =
        runBlocking {
            target.telephonyKeyManager.generateKeyPair(TARGET_NUMBER_KEY)

            val backup = source.service.exportBackup(PASSWORD)
            val file = target.service.readBackup(backup)
            val result = target.service.importBackup(file, PASSWORD)

            assertNull(file.open(PASSWORD, app.keyManager).sudoTelephony)
            assertFalse(result.importedTelephonyKeys)
            assertEquals(source.profilesClient.getSymmetricKeyId(), target.profilesClient.getSymmetricKeyId())
            assertNotNull(target.telephonyKeyManager.getPrivateKeyData(TARGET_NUMBER_KEY))
        }

    @Test
    fun failedTelephonyImportShouldRestoreBothSetsOfKeys() =
        runBlocking {
            target.telephonyKeyManager.generateKeyPair(TARGET_NUMBER_KEY)
            val targetKeyId = target.profilesClient.getSymmetricKeyId()!!
            val payload =
                KeyBackupPayload(
                    sudoProfiles = KeyBackupPayload.SudoProfilesKeys("NEW-KEY", listOf(sudoKey("NEW-KEY"))),
                    sudoTelephony = KeyBackupPayload.SudoTelephonyKeys("not a key archive".toByteArray()),
                )
            val file = KeyBackupFile.seal(payload, PASSWORD, app.keyManager)

            assertThrows<SudoTelephonyClient.CryptographicKeysException> {
                target.service.importBackup(file, PASSWORD)
            }

            assertEquals(targetKeyId, target.profilesClient.getSymmetricKeyId())
            assertEquals(setOf(targetKeyId), target.sudoKeyIds)
            assertNotNull(target.telephonyKeyManager.getPrivateKeyData(TARGET_NUMBER_KEY))
        }

    @Test
    fun sudoKeysFromIosShouldMergeWithThisDevicesKeys() =
        runBlocking {
            val targetKeyId = target.profilesClient.getSymmetricKeyId()!!
            val data =
                InstrumentationRegistry
                    .getInstrumentation()
                    .context.assets
                    .open("ios-key-backup.json")
                    .use { it.readBytes() }
            // The fixture's telephony archive is placeholder bytes, so only its Sudo keys are imported.
            val iosPayload = target.service.readBackup(data).open("café key backup", app.keyManager)
            val file = KeyBackupFile.seal(iosPayload.copy(sudoTelephony = null), PASSWORD, app.keyManager)

            val result = target.service.importBackup(file, PASSWORD)

            assertEquals(2, result.sudoKeyCount)
            assertEquals("IOS-KEY-2", target.profilesClient.getSymmetricKeyId())
            assertEquals(setOf(targetKeyId, "IOS-KEY-1", "IOS-KEY-2"), target.sudoKeyIds)
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
                val file = KeyBackupFile.seal(KeyBackupPayload(sudoKeys, sudoTelephony = null), PASSWORD, app.keyManager)
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
                    KeyBackupPayload(sudoProfiles = null, sudoTelephony = KeyBackupPayload.SudoTelephonyKeys(byteArrayOf(1))),
                    PASSWORD,
                    app.keyManager,
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
