/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.ui.viewer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rajnishkmehta.sakshi.portal.data.SettingsRepository
import rajnishkmehta.sakshi.sdk.api.SakshiClient
import rajnishkmehta.sakshi.sdk.api.SakshiClientConfig
import rajnishkmehta.sakshi.sdk.api.models.MediaDetailsResponse
import kotlinx.coroutines.flow.firstOrNull
import rajnishkmehta.sakshi.portal.debug.DebugLogger as Log

sealed interface MediaViewerUiState {
    data object Loading : MediaViewerUiState
    data class Success(val mediaDetails: MediaDetailsResponse) : MediaViewerUiState
    data class Error(val message: String) : MediaViewerUiState
}

class MediaViewerViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<MediaViewerUiState>(MediaViewerUiState.Loading)
    val uiState: StateFlow<MediaViewerUiState> = _uiState.asStateFlow()

    private var sakshiClient: SakshiClient? = null

    fun loadMedia(fileId: String) {
        viewModelScope.launch {
            _uiState.value = MediaViewerUiState.Loading
            try {
                val vaultPackage = settingsRepository.vaultPackageFlow.firstOrNull() ?: "rajnishkmehta.sakshi.vault"

                val config = SakshiClientConfig(
                    vaultPackageName = vaultPackage,
                    connectionTimeoutMs = 5000L
                )

                sakshiClient?.disconnect()
                val client = SakshiClient.create(getApplication(), config)
                sakshiClient = client

                val result = client.getMedia(fileId)

                if (result.isSuccess) {
                    _uiState.value = MediaViewerUiState.Success(result.getOrNull()!!)
                } else {
                    _uiState.value = MediaViewerUiState.Error(result.errorOrNull()?.message ?: "Failed to load media")
                }
            } catch (e: Exception) {
                Log.e("MediaViewerViewModel", "Error loading media: ${e.message}")
                _uiState.value = MediaViewerUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sakshiClient?.disconnect()
    }
}

class MediaViewerViewModelFactory(
    private val application: Application,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaViewerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MediaViewerViewModel(application, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
