/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.keybackup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sudoplatform.telephonyexample.App
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** The password dialog currently shown. */
sealed interface PasswordPrompt {
    data object Export : PasswordPrompt

    data class Import(
        val file: KeyBackupFile,
    ) : PasswordPrompt
}

data class KeyBackupUiState(
    val passwordPrompt: PasswordPrompt? = null,
    /** True while a backup is being encrypted, decrypted, read or written. */
    val isWorking: Boolean = false,
    /** Why the last password attempt failed, shown in the password dialog. */
    val passwordError: String? = null,
    /** The backup the user picked, awaiting confirmation before they enter its password. */
    val pendingImport: KeyBackupFile? = null,
    val importResult: KeyBackupImportResult? = null,
    val exportSaved: Boolean = false,
    val error: String? = null,
)

/** Asks the screen to let the user choose where to save the backup, suggesting [fileName]. */
data class CreateBackupDocument(
    val fileName: String,
)

/** Exports this device's keys to a password-protected file, and imports them from one. */
class KeyBackupViewModel(
    private val app: App,
    private val keyBackupService: KeyBackupService,
) : ViewModel() {
    companion object {
        const val BACKUP_MIME_TYPE = "application/json"

        /** Some document providers don't recognise `.json`, so files picked from them have this type instead. */
        val IMPORT_MIME_TYPES = arrayOf(BACKUP_MIME_TYPE, "application/octet-stream")

        val Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as App
                    KeyBackupViewModel(
                        app,
                        DefaultKeyBackupService(app, app.sudoProfilesClient, app.sudoTelephonyClient, app.logger) {
                            app.sudoUserClient.getSubject()
                        },
                    )
                }
            }
    }

    private val _uiState = MutableStateFlow(KeyBackupUiState())
    val uiState: StateFlow<KeyBackupUiState> = _uiState.asStateFlow()

    private val _createDocument = Channel<CreateBackupDocument>(Channel.BUFFERED)
    val createDocument: Flow<CreateBackupDocument> = _createDocument.receiveAsFlow()

    /** The encrypted backup, held until the user chooses where to save it. */
    private var exportedBackup: ByteArray? = null

    fun startExport() {
        _uiState.update { it.copy(passwordPrompt = PasswordPrompt.Export, passwordError = null) }
    }

    fun export(password: String) {
        _uiState.update { it.copy(isWorking = true, passwordError = null) }
        viewModelScope.launch {
            try {
                // Key derivation is deliberately slow, so keep it off the main thread.
                exportedBackup = withContext(Dispatchers.Default) { keyBackupService.exportBackup(password) }
                app.didExportKeys()
                _uiState.update { it.copy(passwordPrompt = null) }
                _createDocument.send(CreateBackupDocument(exportFileName()))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                app.logger.error("Failed to export keys: $e")
                _uiState.update { it.copy(passwordError = e.describe()) }
            } finally {
                _uiState.update { it.copy(isWorking = false) }
            }
        }
    }

    /** Writes the backup to the document the user created, or discards it if they cancelled. */
    fun saveExport(uri: Uri?) {
        val backup = exportedBackup ?: return
        exportedBackup = null
        if (uri == null) {
            return
        }
        _uiState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val stream = app.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("The file couldn't be opened.")
                    stream.use { it.write(backup) }
                }
                _uiState.update { it.copy(exportSaved = true) }
            } catch (e: IOException) {
                app.logger.error("Failed to save key backup: $e")
                _uiState.update { it.copy(error = e.describe()) }
            } finally {
                _uiState.update { it.copy(isWorking = false) }
            }
        }
    }

    /** Reads the backup the user picked, or does nothing if they cancelled. */
    fun readBackup(uri: Uri?) {
        if (uri == null) {
            return
        }
        _uiState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            try {
                val data =
                    withContext(Dispatchers.IO) {
                        val stream = app.contentResolver.openInputStream(uri) ?: throw IOException("The file couldn't be opened.")
                        stream.use { it.readBytes() }
                    }
                _uiState.update { it.copy(pendingImport = keyBackupService.readBackup(data)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                app.logger.error("Failed to read key backup: $e")
                _uiState.update { it.copy(error = e.describe()) }
            } finally {
                _uiState.update { it.copy(isWorking = false) }
            }
        }
    }

    fun confirmImport() {
        val file = _uiState.value.pendingImport ?: return
        _uiState.update { it.copy(pendingImport = null, passwordPrompt = PasswordPrompt.Import(file), passwordError = null) }
    }

    fun cancelImport() {
        _uiState.update { it.copy(pendingImport = null) }
    }

    fun import(password: String) {
        val prompt = _uiState.value.passwordPrompt as? PasswordPrompt.Import ?: return
        _uiState.update { it.copy(isWorking = true, passwordError = null) }
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.Default) { keyBackupService.importBackup(prompt.file, password) }
                _uiState.update { it.copy(passwordPrompt = null, importResult = result) }
                prepareTelephonyKeysAfterImport()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                app.logger.error("Failed to import keys: $e")
                _uiState.update { it.copy(passwordError = e.describe()) }
            } finally {
                _uiState.update { it.copy(isWorking = false) }
            }
        }
    }

    /** Creates the shared phone number key if the backup didn't contain one, so provisioning still works. */
    private suspend fun prepareTelephonyKeysAfterImport() {
        try {
            withContext(Dispatchers.IO) { app.prepareTelephonyKeys() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            app.logger.error("Failed to prepare phone number keys after import: $e")
            _uiState.update { it.copy(error = e.describe()) }
        }
    }

    fun cancelPasswordPrompt() {
        _uiState.update { it.copy(passwordPrompt = null, passwordError = null) }
    }

    fun dismissImportResult() {
        _uiState.update { it.copy(importResult = null) }
    }

    fun dismissExportSaved() {
        _uiState.update { it.copy(exportSaved = false) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun exportFileName(): String =
        "sudo-keys-${DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now(ZoneId.systemDefault()))}.json"

    private fun Throwable.describe(): String = localizedMessage ?: toString()
}
