/*
 * Copyright © 2026 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.telephonyexample.entitlements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sudoplatform.sudoentitlements.types.Entitlement
import com.sudoplatform.sudoentitlements.types.EntitlementConsumer
import com.sudoplatform.sudoprofiles.ListOption
import com.sudoplatform.telephonyexample.App
import com.sudoplatform.telephonyexample.sudos.displayLabel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** How much of an entitlement has been used, by the user or by one of their Sudos. */
data class EntitlementUsage(
    val name: String,
    /** The Sudo's label or ID for usage counted per Sudo, or null for usage counted against the user. */
    val sudo: String?,
    val consumed: Long,
    val value: Long,
)

data class EntitlementsUiState(
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    /** Name of the entitlement set the user's entitlements come from, e.g. "TEST". */
    val entitlementsSetName: String? = null,
    val entitlements: List<Entitlement> = emptyList(),
    val usage: List<EntitlementUsage> = emptyList(),
    val loadError: String? = null,
)

/** Shows the user's entitlements and how much of each they and their Sudos have used. */
class EntitlementsViewModel(
    private val app: App,
) : ViewModel() {
    companion object {
        /** The issuer of a Sudo consumer. */
        private const val SUDO_SERVICE_ISSUER = "sudoplatform.sudoservice"

        val Factory =
            viewModelFactory {
                initializer { EntitlementsViewModel(this[APPLICATION_KEY] as App) }
            }
    }

    private val _uiState = MutableStateFlow(EntitlementsUiState())
    val uiState: StateFlow<EntitlementsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            try {
                val consumption = withContext(Dispatchers.IO) { app.sudoEntitlementsClient.getEntitlementsConsumption() }
                val sudoLabels = sudoLabelsById()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasLoaded = true,
                        entitlementsSetName = consumption.entitlements.entitlementsSetName,
                        entitlements = consumption.entitlements.entitlements.sortedBy(Entitlement::name),
                        usage =
                            consumption.consumption
                                .map { item ->
                                    EntitlementUsage(item.name, sudoName(item.consumer, sudoLabels), item.consumed, item.value)
                                }.sortedWith(compareBy(EntitlementUsage::name, { it.sudo.orEmpty() })),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                app.logger.error("Failed to get entitlements: $e")
                _uiState.update { it.copy(isLoading = false, loadError = e.localizedMessage ?: e.toString()) }
            }
        }
    }

    fun dismissLoadError() {
        _uiState.update { it.copy(loadError = null) }
    }

    private fun sudoName(
        consumer: EntitlementConsumer?,
        sudoLabels: Map<String, String>,
    ): String? {
        if (consumer == null || consumer.issuer != SUDO_SERVICE_ISSUER) {
            return null
        }
        return sudoLabels[consumer.id] ?: consumer.id
    }

    /** Labels of the user's Sudos, so usage by a Sudo can show its name. Falls back to IDs if they can't be listed. */
    private suspend fun sudoLabelsById(): Map<String, String> =
        try {
            withContext(Dispatchers.IO) { app.sudoProfilesClient.listSudos(ListOption.CACHE_ONLY) }
                .mapNotNull { sudo -> sudo.id?.let { it to sudo.displayLabel } }
                .toMap()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            app.logger.error("Failed to list Sudos for entitlement usage: $e")
            emptyMap()
        }
}
