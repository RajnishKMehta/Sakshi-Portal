/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.ui.gallery

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import rajnishkmehta.sakshi.portal.data.SettingsRepository
import rajnishkmehta.sakshi.sdk.api.SakshiClient
import rajnishkmehta.sakshi.sdk.api.SakshiClientConfig
import rajnishkmehta.sakshi.portal.debug.DebugLogger as Log
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin

@Serializable
data class RawMediaItem(
    val fileId: String,
    val timestamp: Long
)

data class ParsedMediaItem(
    val id: String,
    val type: MediaType,
    val thumbnailUri: Uri
)

class GalleryViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<GalleryUiState>(GalleryUiState.Loading)
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var sakshiClient: SakshiClient? = null

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.vaultPackageFlow
                .distinctUntilChanged()
                .collectLatest { pkg ->
                    loadMedia(pkg)
                }
        }
    }

    fun refreshMedia() {
        _isRefreshing.value = true
        loadMedia(isRefresh = true)
    }

    fun loadMedia(specificPackage: String? = null, isRefresh: Boolean = false) {
        val previousJob = loadJob
        loadJob = viewModelScope.launch {
            previousJob?.cancelAndJoin()
            if (!isRefresh) {
                _uiState.value = GalleryUiState.Loading
            }
            try {
                var vaultPackage = specificPackage ?: settingsRepository.vaultPackageFlow.firstOrNull()

                if (vaultPackage == null) {
                    vaultPackage = "rajnishkmehta.sakshi.vault"
                    settingsRepository.setVaultPackageIfUnset(vaultPackage)
                }

                val config = SakshiClientConfig(
                    vaultPackageName = vaultPackage,
                    connectionTimeoutMs = 5000L
                )

                sakshiClient?.disconnect()
                val client = SakshiClient.create(getApplication(), config)
                sakshiClient = client

                // Concurrently run ping and listMedia
                val pingDeferred = async { client.pingVault() }
                val listMediaDeferred = async { client.listMedia() }
                val thumbnailDeferred = async { client.getThumbnail() }

                val pingResult = pingDeferred.await()

                if (pingResult.isFailure) {
                    Log.e("GalleryViewModel", "Vault is unavailable: ${pingResult.errorOrNull()?.message}")
                    _uiState.value = GalleryUiState.VaultUnavailable
                    return@launch
                }

                val listMediaResult = listMediaDeferred.await()
                if (listMediaResult.isFailure) {
                    _uiState.value = GalleryUiState.Error("Failed to list media: ${listMediaResult.errorOrNull()?.message}")
                    return@launch
                }

                val thumbnailResult = thumbnailDeferred.await()
                if (thumbnailResult.isFailure) {
                    _uiState.value = GalleryUiState.Error("Failed to get thumbnail template: ${thumbnailResult.errorOrNull()?.message}")
                    return@launch
                }

                val thumbnailTemplate = thumbnailResult.getOrNull()!!

                val jsonString = listMediaResult.getOrNull()!!
                Log.d("GalleryViewModel", "Media JSON: $jsonString")

                val rawMediaMap = try {
                    Json.decodeFromString<Map<String, List<RawMediaItem>>>(jsonString)
                } catch (e: CancellationException) {
                Log.d("GalleryViewModel", "loadMedia cancelled")
                throw e
            } catch (e: Exception) {
                    Log.e("GalleryViewModel", "Failed to parse JSON: ${e.message}")
                    _uiState.value = GalleryUiState.Error("Invalid response format from Vault.")
                    return@launch
                }

                val parsedItems = mutableListOf<ParsedMediaItem>()

                rawMediaMap.forEach { (typeStr, items) ->
                    val type = when (typeStr.lowercase()) {
                        "photo" -> MediaType.PHOTO
                        "video" -> MediaType.VIDEO
                        "audio" -> MediaType.AUDIO
                        else -> MediaType.OTHER
                    }

                    if (true) {
                        items.forEach { rawItem ->
                            val uriStr = thumbnailTemplate
                                .replace("{mediaType}", typeStr.lowercase())
                                .replace("{fileId}", rawItem.fileId)
                            val uri = Uri.parse(uriStr)

                            parsedItems.add(
                                ParsedMediaItem(
                                    id = rawItem.fileId,
                                    type = type,
                                    thumbnailUri = uri
                                )
                            )
                        }
                    }
                }

                _uiState.value = GalleryUiState.Success(parsedItems)

            } catch (e: CancellationException) {
                Log.d("GalleryViewModel", "loadMedia cancelled")
                throw e
            } catch (e: Exception) {
                Log.e("GalleryViewModel", "Error loading media: ${e.message}")
                _uiState.value = GalleryUiState.Error(e.message ?: "Unknown error")
            } finally {
                if (loadJob == coroutineContext[Job]) {
                    _isRefreshing.value = false
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sakshiClient?.disconnect()
    }
}

class GalleryViewModelFactory(
    private val application: Application,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GalleryViewModel(application, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
