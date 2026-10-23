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
import rajnishkmehta.sakshi.portal.data.deletion.RemovedMediaStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import rajnishkmehta.sakshi.sdk.api.SakshiClient
import rajnishkmehta.sakshi.sdk.api.SakshiClientConfig
import rajnishkmehta.sakshi.portal.debug.DebugLogger as Log
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin

/**
 * Represents a raw media item as returned by the Vault SDK.
 *
 * @property fileId The unique identifier of the media file.
 * @property timestamp The timestamp representing when the media was captured or created, in milliseconds.
 */
@Serializable
data class RawMediaItem(
    val fileId: String,
    val timestamp: Long
)

/**
 * Represents a parsed media item ready for UI rendering.
 *
 * @property id The unique identifier of the media file.
 * @property type The type of the media (e.g., PHOTO, VIDEO).
 * @property thumbnailUri The URI used to load the thumbnail for this media item.
 * @property timestamp The creation timestamp, used for sorting.
 */
data class ParsedMediaItem(
    val id: String,
    val type: MediaType,
    val thumbnailUri: Uri,
    val timestamp: Long
)

/**
 * ViewModel for the gallery screen. It is responsible for loading the list of media from the Vault,
 * handling pull-to-refresh, and managing UI state including loading, success, and error states.
 *
 * The ViewModel handles both standard gallery mode and deleted-media mode (scheduled deletions).
 * To ensure UI consistency, it hides media files that were just scheduled for deletion or recovered,
 * relying on the [removedMediaStore] to filter out IDs until a fresh Vault list is fetched.
 *
 * @param application The application context.
 * @param settingsRepository Repository for retrieving the configured Vault package name.
 * @param removedMediaStore Source of media IDs to hide immediately (optimistic UI updates).
 * @param isDeletedMode True if this ViewModel is specifically configured for the Scheduled Deletions screen.
 */
class GalleryViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val removedMediaStore: RemovedMediaStore,
    private val isDeletedMode: Boolean = false
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<GalleryUiState>(GalleryUiState.Loading)

    /**
     * The current state of the gallery UI. The list of media excludes any item whose ID is
     * present in the [removedMediaStore].
     */
    val uiState: StateFlow<GalleryUiState> = combine(_uiState, removedMediaStore.removedIds, ::excludeRemovedMedia)
        .stateIn(viewModelScope, SharingStarted.Eagerly, GalleryUiState.Loading)

    private val _isRefreshing = MutableStateFlow(false)

    /** Indicates whether a pull-to-refresh operation is currently in progress. */
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

    /**
     * Triggers a refresh of the media list, showing the pull-to-refresh indicator.
     */
    fun refreshMedia() {
        _isRefreshing.value = true
        loadMedia(isRefresh = true)
    }

    /**
     * Loads the media list from Vault using the Sakshi SDK.
     *
     * Every load (app start, Vault switch, retry, pull-to-refresh) first clears [removedMediaStore];
     * the currently shown list keeps those items hidden until the fresh Vault result replaces it.
     *
     * @param specificPackage An optional package name to use instead of the one in settings.
     * @param isRefresh If true, suppresses the initial loading state for seamless pull-to-refresh.
     */
    fun loadMedia(specificPackage: String? = null, isRefresh: Boolean = false) {
        val clearedIds = removedMediaStore.clear()
        if (clearedIds.isNotEmpty()) {
            _uiState.update { excludeRemovedMedia(it, clearedIds) }
        }
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
                val listMediaDeferred = async { if (isDeletedMode) client.listDeletedMedia() else client.listMedia() }
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
                                    thumbnailUri = uri,
                                    timestamp = rawItem.timestamp
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

/**
 * Factory for creating [GalleryViewModel].
 *
 * @param application The application context.
 * @param settingsRepository Repository for settings.
 * @param removedMediaStore Callers must provide the store shared with the viewer to ensure immediate sync of deleted files.
 * @param isDeletedMode When true, configures the ViewModel to load data exclusively from the deleted media data source.
 */
class GalleryViewModelFactory(
    private val application: Application,
    private val settingsRepository: SettingsRepository,
    private val removedMediaStore: RemovedMediaStore,
    private val isDeletedMode: Boolean = false
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GalleryViewModel(application, settingsRepository, removedMediaStore, isDeletedMode) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
