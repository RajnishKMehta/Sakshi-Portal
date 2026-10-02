/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.vault

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rajnishkmehta.sakshi.sdk.api.SakshiClient
import rajnishkmehta.sakshi.sdk.api.SakshiClientConfig
import rajnishkmehta.sakshi.sdk.api.SakshiError
import rajnishkmehta.sakshi.portal.debug.DebugLogger as Log
import rajnishkmehta.sakshi.portal.R

class VaultSelectionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppDiscoveryRepository(application)
    private var allApps: List<AppInfo> = emptyList()

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private var verificationJob: Job? = null

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            // try { Log.i(tag = "VaultSelectionViewModel", message = "Loading apps...") } catch (e: Exception) {}
            try {
                allApps = repository.getInstalledApplications()
                // try { Log.i(tag = "VaultSelectionViewModel", message = "Apps loaded: ${allApps.size}") } catch (e: Exception) {}
                _uiState.value = UiState.Success(allApps)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // try { Log.e(tag = "VaultSelectionViewModel", message = "Error loading apps: ${e.message}") } catch (logE: Exception) {}
                _uiState.value = UiState.Error(e.message ?: getApplication<Application>().getString(R.string.vault_selection_load_failed))
            }
        }
    }

    fun filter(query: String) {
        val currentState = _uiState.value
        if (currentState is UiState.Success || currentState is UiState.Filtering) {
            val filtered = if (query.isEmpty()) {
                allApps
            } else {
                allApps.filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
                }
            }
            _uiState.value = UiState.Filtering(filtered)
        }
    }

    fun cancelVerification() {
        verificationJob?.cancel()
        verificationJob = null
    }

    fun verifyVaultApp(packageName: String, onResult: (Boolean, String?) -> Unit) {
        cancelVerification()
        verificationJob = viewModelScope.launch {
            // try { Log.i(tag = "VaultSelectionViewModel", message = "Verifying vault app: $packageName") } catch (e: Exception) {}
            val config = SakshiClientConfig(
                vaultPackageName = packageName,
                connectionTimeoutMs = 5000L
            )
            val tempClient = SakshiClient.create(getApplication(), config)

            val result = try {
                withContext(Dispatchers.IO) {
                    tempClient.pingVault()
                }
            } finally {
                tempClient.disconnect()
            }

            if (result.isSuccess) {
                // try { Log.i(tag = "VaultSelectionViewModel", message = "Verification success") } catch (e: Exception) {}
                onResult(true, null)
            } else {
                val err = result.errorOrNull()
                val message = err?.message ?: getApplication<Application>().getString(R.string.unknown_error)
                // try { Log.e(tag = "VaultSelectionViewModel", message = "Verification failed: $message") } catch (e: Exception) {}
                onResult(false, message)
            }
        }
    }

    sealed class UiState {
        object Loading : UiState()
        data class Success(val apps: List<AppInfo>) : UiState()
        data class Filtering(val apps: List<AppInfo>) : UiState()
        data class Error(val message: String) : UiState()
    }
}
