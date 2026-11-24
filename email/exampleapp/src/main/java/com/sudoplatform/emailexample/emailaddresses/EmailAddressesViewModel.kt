/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.emailexample.emailaddresses

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sudoplatform.emailexample.App
import com.sudoplatform.emailexample.NavArgs
import com.sudoplatform.emailexample.R
import com.sudoplatform.emailexample.emailaliases.aliasesOwnedBySudo
import com.sudoplatform.emailexample.emailaliases.formatMetadataJson
import com.sudoplatform.emailexample.emailaliases.parseMetadataJson
import com.sudoplatform.sudoemail.SudoEmailClient
import com.sudoplatform.sudoemail.types.EmailAddress
import com.sudoplatform.sudoemail.types.EmailAlias
import com.sudoplatform.sudoemail.types.ListAPIResult
import com.sudoplatform.sudoemail.types.inputs.DeprovisionEmailAliasInput
import com.sudoplatform.sudoemail.types.inputs.DisableEmailAliasInput
import com.sudoplatform.sudoemail.types.inputs.EnableEmailAliasInput
import com.sudoplatform.sudoemail.types.inputs.ListEmailAddressesForSudoIdInput
import com.sudoplatform.sudoemail.types.inputs.ListEmailAliasesForOwnerInput
import com.sudoplatform.sudoemail.types.inputs.UpdateEmailAliasInput
import com.sudoplatform.sudoemail.types.inputs.VerifyExternalEmailAddressInput
import com.sudoplatform.sudokeymanager.KeyManagerException
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
import java.util.Date

/** The account's email alias features, from the email service's configuration. */
data class AliasConfiguration(
    val aliasesEnabled: Boolean,
    val externalAliasesEnabled: Boolean,
)

/** The alias being edited in the update dialog. */
data class AliasUpdateForm(
    val alias: EmailAlias,
    val metadataJson: String,
    /** The new expiry, or null to clear it. */
    val expiresAt: Date?,
    /** The user tried to save metadata that isn't a JSON object of strings. */
    val showInvalidMetadata: Boolean = false,
)

/** The external alias whose real address is awaiting the code the service emailed to it. */
data class AliasVerificationForm(
    val alias: EmailAlias,
    val code: String = "",
    /** The user tried to verify without a code. */
    val showCodeRequired: Boolean = false,
)

/** A failed action the user started, such as deleting or updating an alias. */
data class ActionError(
    @StringRes val titleResId: Int,
    val message: String,
    /** Whether the action can be tried again; otherwise the error is only acknowledged. */
    val canRetry: Boolean,
)

data class EmailAddressesUiState(
    val isLoading: Boolean = false,
    /** True once the service has returned the addresses, so an empty list means there are none. */
    val hasLoaded: Boolean = false,
    val emailAddresses: List<EmailAddress> = emptyList(),
    /**
     * True if addresses couldn't be read because this device doesn't have their keys, e.g. they
     * were provisioned on another device. Importing a key backup fixes this.
     */
    val needsKeyImport: Boolean = false,
    /** Null until the configuration has loaded. */
    val aliasConfiguration: AliasConfiguration? = null,
    /** Every alias the user owns; the screen shows those of the selected type. */
    val aliases: List<EmailAlias> = emptyList(),
    val showExternalAliases: Boolean = false,
    /** An address the user swiped away, awaiting confirmation before it is deprovisioned. */
    val pendingAddressDeprovision: EmailAddress? = null,
    /** An alias the user swiped away, awaiting confirmation before it is deprovisioned. */
    val pendingAliasDeprovision: EmailAlias? = null,
    /** The alias whose actions are being offered. */
    val selectedAlias: EmailAlias? = null,
    val updateForm: AliasUpdateForm? = null,
    val verificationForm: AliasVerificationForm? = null,
    /** The caption of the blocking progress dialog while an action runs. */
    @StringRes val busyResId: Int? = null,
    val listAddressesError: String? = null,
    val configurationError: String? = null,
    val listAliasesError: String? = null,
    val actionError: ActionError? = null,
)

/** Emitted once an external alias's real address has been verified. */
data object AliasVerified

/**
 * Lists a Sudo's email addresses and email aliases, deprovisions them, and enables,
 * disables, updates and verifies aliases.
 */
class EmailAddressesViewModel(
    private val sudoId: String,
    private val app: App,
) : ViewModel() {
    companion object {
        /** The longest verification code the service sends. */
        private const val VERIFICATION_CODE_MAX_LENGTH = 10

        val Factory =
            viewModelFactory {
                initializer {
                    EmailAddressesViewModel(
                        sudoId = checkNotNull(createSavedStateHandle().get<String>(NavArgs.SUDO_ID)),
                        app = this[APPLICATION_KEY] as App,
                    )
                }
            }
    }

    private val emailClient: SudoEmailClient get() = app.sudoEmailClient

    private val _uiState = MutableStateFlow(EmailAddressesUiState())
    val uiState: StateFlow<EmailAddressesUiState> = _uiState.asStateFlow()

    private val _verified = Channel<AliasVerified>(Channel.BUFFERED)
    val verified: Flow<AliasVerified> = _verified.receiveAsFlow()

    /** Repeats the action that produced [EmailAddressesUiState.actionError]. */
    private var retryAction: (() -> Unit)? = null

    init {
        loadConfiguration()
    }

    /** Fetch the addresses, and the aliases if the configuration enables them, from the service. */
    fun refresh() {
        listEmailAddresses()
        if (_uiState.value.aliasConfiguration?.aliasesEnabled == true) {
            listEmailAliases()
        }
    }

    /**
     * The configuration says whether email aliases are enabled, and whether their real
     * address can be outside the platform.
     */
    fun loadConfiguration() {
        _uiState.update { it.copy(configurationError = null) }
        viewModelScope.launch {
            try {
                val config = withContext(Dispatchers.IO) { app.getConfigurationData() }
                _uiState.update {
                    it.copy(aliasConfiguration = AliasConfiguration(config.emailAliasesEnabled, config.externalEmailAliasesEnabled))
                }
                if (config.emailAliasesEnabled) {
                    listEmailAliases()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(configurationError = e.describe()) }
            }
        }
    }

    fun showExternalAliases(showExternal: Boolean) {
        _uiState.update { it.copy(showExternalAliases = showExternal) }
    }

    fun requestDeprovisionAddress(emailAddressId: String) {
        _uiState.update { state -> state.copy(pendingAddressDeprovision = state.emailAddresses.find { it.id == emailAddressId }) }
    }

    fun cancelDeprovisionAddress() {
        _uiState.update { it.copy(pendingAddressDeprovision = null) }
    }

    /** Deprovision the address awaiting confirmation, which deletes its messages. */
    fun confirmDeprovisionAddress() {
        val emailAddress = _uiState.value.pendingAddressDeprovision ?: return
        _uiState.update { it.copy(pendingAddressDeprovision = null) }
        runAction(R.string.deleting_email_address, R.string.deleting_email_address_failure) {
            try {
                withContext(Dispatchers.IO) { emailClient.deprovisionEmailAddress(emailAddress.id) }
            } catch (e: SudoEmailClient.EmailAddressException.KeyPairRemovalFailedException) {
                // The address was deleted; only removing its key from this device failed, so drop
                // it from the list before saying why.
                app.logger.error("Deleted email address ${e.emailAddress.id}, but couldn't remove keys ${e.keyIds}: ${e.cause}")
                _uiState.update {
                    it.copy(
                        actionError =
                            ActionError(
                                R.string.email_address_deleted,
                                app.getString(R.string.email_address_key_not_removed, emailAddress.emailAddress),
                                canRetry = false,
                            ),
                    )
                }
            }
            _uiState.update { state -> state.copy(emailAddresses = state.emailAddresses.filterNot { it.id == emailAddress.id }) }
        }
    }

    fun selectAlias(alias: EmailAlias) {
        _uiState.update { it.copy(selectedAlias = alias) }
    }

    fun dismissAliasActions() {
        _uiState.update { it.copy(selectedAlias = null) }
    }

    fun enableAlias(aliasId: String) {
        _uiState.update { it.copy(selectedAlias = null) }
        runAction(R.string.enabling_email_alias, R.string.enable_email_alias_failure, retry = { enableAlias(aliasId) }) {
            withContext(Dispatchers.IO) { emailClient.enableEmailAlias(EnableEmailAliasInput(emailAliasId = aliasId)) }
            listEmailAliases()
        }
    }

    fun disableAlias(aliasId: String) {
        _uiState.update { it.copy(selectedAlias = null) }
        runAction(R.string.disabling_email_alias, R.string.disable_email_alias_failure, retry = { disableAlias(aliasId) }) {
            withContext(Dispatchers.IO) { emailClient.disableEmailAlias(DisableEmailAliasInput(emailAliasId = aliasId)) }
            listEmailAliases()
        }
    }

    /** Open the update dialog with the alias's current metadata and expiry. */
    fun startUpdate(alias: EmailAlias) {
        _uiState.update {
            it.copy(
                selectedAlias = null,
                updateForm =
                    AliasUpdateForm(
                        alias = alias,
                        metadataJson = formatMetadataJson(alias.metadata),
                        expiresAt = alias.expiresAt?.takeIf { date -> date.time != 0L },
                    ),
            )
        }
    }

    fun updateMetadataJson(metadataJson: String) {
        _uiState.update { state ->
            state.copy(updateForm = state.updateForm?.copy(metadataJson = metadataJson, showInvalidMetadata = false))
        }
    }

    fun updateExpiresAt(expiresAt: Date?) {
        _uiState.update { state -> state.copy(updateForm = state.updateForm?.copy(expiresAt = expiresAt)) }
    }

    fun cancelUpdate() {
        _uiState.update { it.copy(updateForm = null) }
    }

    /**
     * Save the update dialog. Empty metadata clears the alias's metadata, and no expiry is sent as
     * `Date(0)`, which clears it.
     */
    fun saveUpdate() {
        val form = _uiState.value.updateForm ?: return
        val metadataJson = form.metadataJson.trim()
        val metadata =
            if (metadataJson.isEmpty()) {
                emptyMap()
            } else {
                parseMetadataJson(metadataJson) ?: run {
                    _uiState.update { it.copy(updateForm = form.copy(showInvalidMetadata = true)) }
                    return
                }
            }
        _uiState.update { it.copy(updateForm = null) }
        updateAlias(form.alias.id, metadata, form.expiresAt ?: Date(0))
    }

    fun requestDeprovisionAlias(aliasId: String) {
        _uiState.update { state -> state.copy(pendingAliasDeprovision = state.aliases.find { it.id == aliasId }) }
    }

    fun cancelDeprovisionAlias() {
        _uiState.update { it.copy(pendingAliasDeprovision = null) }
    }

    fun confirmDeprovisionAlias() {
        val alias = _uiState.value.pendingAliasDeprovision ?: return
        _uiState.update { it.copy(pendingAliasDeprovision = null) }
        deprovisionAlias(alias.id)
    }

    /**
     * Start verifying an external alias's real address. The service emails a code to the address,
     * which the user then enters.
     */
    fun startVerification(alias: EmailAlias) {
        runAction(R.string.verifying_email, R.string.verification_initiation_failure, retry = { startVerification(alias) }) {
            withContext(Dispatchers.IO) {
                emailClient.verifyExternalEmailAddress(
                    VerifyExternalEmailAddressInput(emailAddress = alias.realAddress, emailAliasId = alias.id),
                )
            }
            _uiState.update { it.copy(verificationForm = AliasVerificationForm(alias)) }
        }
    }

    fun updateVerificationCode(code: String) {
        _uiState.update { state ->
            state.copy(
                verificationForm = state.verificationForm?.copy(code = code.take(VERIFICATION_CODE_MAX_LENGTH), showCodeRequired = false),
            )
        }
    }

    fun cancelVerification() {
        _uiState.update { it.copy(verificationForm = null) }
    }

    /** Send the code the user entered. A wrong code can be entered again. */
    fun submitVerificationCode() {
        val form = _uiState.value.verificationForm ?: return
        val code = form.code.trim()
        if (code.isEmpty()) {
            _uiState.update { it.copy(verificationForm = form.copy(showCodeRequired = true)) }
            return
        }
        _uiState.update { it.copy(verificationForm = null) }
        val reopen = { _uiState.update { it.copy(verificationForm = AliasVerificationForm(form.alias)) } }
        runAction(R.string.verifying_email, R.string.verification_completion_failure, retry = reopen) {
            val result =
                withContext(Dispatchers.IO) {
                    emailClient.verifyExternalEmailAddress(
                        VerifyExternalEmailAddressInput(
                            emailAddress = form.alias.realAddress,
                            emailAliasId = form.alias.id,
                            verificationCode = code,
                        ),
                    )
                }
            if (result.isVerified) {
                _verified.send(AliasVerified)
                listEmailAliases()
            } else {
                retryAction = reopen
                _uiState.update {
                    it.copy(
                        actionError =
                            ActionError(
                                titleResId = R.string.verification_completion_failure,
                                message = result.reason ?: app.getString(R.string.verification_completion_failure),
                                canRetry = true,
                            ),
                    )
                }
            }
        }
    }

    fun retryAction() {
        retryAction?.invoke()
    }

    fun dismissActionError() {
        _uiState.update { it.copy(actionError = null) }
    }

    fun dismissListAddressesError() {
        _uiState.update { it.copy(listAddressesError = null) }
    }

    fun dismissConfigurationError() {
        _uiState.update { it.copy(configurationError = null) }
    }

    fun dismissListAliasesError() {
        _uiState.update { it.copy(listAliasesError = null) }
    }

    fun listEmailAliases() {
        _uiState.update { it.copy(listAliasesError = null) }
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { emailClient.listEmailAliasesForOwner(ListEmailAliasesForOwnerInput()) }
                when (result) {
                    // The user's aliases across all their Sudos, so keep this Sudo's.
                    is ListAPIResult.Success -> _uiState.update { it.copy(aliases = aliasesOwnedBySudo(result.result.items, sudoId)) }
                    is ListAPIResult.Partial ->
                        _uiState.update {
                            it.copy(
                                aliases = aliasesOwnedBySudo(result.result.items, sudoId),
                                listAliasesError =
                                    result.result.failed
                                        .first()
                                        .cause
                                        .describe(),
                            )
                        }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(listAliasesError = e.describe()) }
            }
        }
    }

    private fun listEmailAddresses() {
        _uiState.update { it.copy(isLoading = true, listAddressesError = null) }
        viewModelScope.launch {
            try {
                val result =
                    withContext(Dispatchers.IO) {
                        emailClient.listEmailAddressesForSudoId(ListEmailAddressesForSudoIdInput(sudoId = sudoId))
                    }
                when (result) {
                    is ListAPIResult.Success ->
                        _uiState.update {
                            it.copy(isLoading = false, hasLoaded = true, emailAddresses = result.result.items, needsKeyImport = false)
                        }
                    // Addresses this device has no keys for are returned as partial results.
                    is ListAPIResult.Partial -> {
                        val causes = result.result.failed.map { it.cause }
                        val needsKeyImport = causes.all { it.isMissingKey() }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                hasLoaded = true,
                                emailAddresses = result.result.items,
                                needsKeyImport = needsKeyImport,
                                listAddressesError = if (needsKeyImport) null else causes.first().describe(),
                            )
                        }
                    }
                }
            } catch (e: SudoEmailClient.EmailAddressException) {
                _uiState.update {
                    if (e.isMissingKey()) {
                        it.copy(isLoading = false, needsKeyImport = true)
                    } else {
                        it.copy(isLoading = false, listAddressesError = e.describe())
                    }
                }
            }
        }
    }

    private fun updateAlias(
        aliasId: String,
        metadata: Map<String, String>,
        expiresAt: Date,
    ) {
        runAction(
            R.string.updating_email_alias,
            R.string.update_email_alias_failure,
            retry = { updateAlias(aliasId, metadata, expiresAt) },
        ) {
            withContext(Dispatchers.IO) {
                emailClient.updateEmailAlias(UpdateEmailAliasInput(emailAliasId = aliasId, metadata = metadata, expiresAt = expiresAt))
            }
            listEmailAliases()
        }
    }

    private fun deprovisionAlias(aliasId: String) {
        runAction(R.string.deleting_email_alias, R.string.delete_email_alias_failure, retry = { deprovisionAlias(aliasId) }) {
            withContext(Dispatchers.IO) { emailClient.deprovisionEmailAlias(DeprovisionEmailAliasInput(emailAliasId = aliasId)) }
            _uiState.update { state -> state.copy(aliases = state.aliases.filterNot { it.id == aliasId }) }
        }
    }

    /**
     * Run [action] behind a progress dialog captioned [busyResId], reporting a failure under
     * [failureResId], with an option to [retry] it if given.
     */
    private fun runAction(
        @StringRes busyResId: Int,
        @StringRes failureResId: Int,
        retry: (() -> Unit)? = null,
        action: suspend () -> Unit,
    ) {
        _uiState.update { it.copy(busyResId = busyResId, actionError = null) }
        viewModelScope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                app.logger.error("Email address action failed: $e")
                retryAction = retry
                _uiState.update { it.copy(actionError = ActionError(failureResId, e.describe(), canRetry = retry != null)) }
            } finally {
                _uiState.update { it.copy(busyResId = null) }
            }
        }
    }

    /**
     * Whether this device lacks the keys to read an address. The key manager's failure is the
     * cause when unsealing an address's display name fails.
     */
    private fun Throwable.isMissingKey(): Boolean =
        generateSequence(this) { it.cause }.any {
            it is SudoEmailClient.EmailAddressException.UnsealingException ||
                it is SudoEmailClient.EmailAddressException.PublicKeyException ||
                it is KeyManagerException
        }

    private fun Throwable.describe(): String = localizedMessage ?: toString()
}
