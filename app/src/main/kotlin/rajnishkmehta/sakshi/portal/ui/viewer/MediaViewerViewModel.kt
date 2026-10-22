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
import rajnishkmehta.sakshi.portal.data.deletion.MediaDeletionRepository
import rajnishkmehta.sakshi.portal.data.deletion.RemovedMediaStore
import rajnishkmehta.sakshi.portal.data.deletion.ScheduleDeletionResult
import rajnishkmehta.sakshi.portal.data.export.MediaExportResult
import rajnishkmehta.sakshi.portal.data.export.MediaExporter
import rajnishkmehta.sakshi.sdk.api.SakshiClient
import rajnishkmehta.sakshi.sdk.api.SakshiClientConfig
import rajnishkmehta.sakshi.sdk.api.SakshiError
import rajnishkmehta.sakshi.sdk.api.SakshiResult
import rajnishkmehta.sakshi.sdk.api.models.MediaDetailsResponse
import kotlinx.coroutines.flow.firstOrNull
import rajnishkmehta.sakshi.portal.debug.DebugLogger as Log

sealed interface MediaViewerUiState {
    data object Loading : MediaViewerUiState
    data class Success(val mediaDetails: MediaDetailsResponse) : MediaViewerUiState
    data class Error(val message: String) : MediaViewerUiState
}

/**
 * ViewModel for the media viewer: loads media details from Vault, exports the current file, and
 * schedules it for deletion.
 *
 * @param removedMediaStore Store notified when Vault confirmed a scheduled deletion.
 * @param mediaExporter Exporter used by [exportMedia] to save the file into shared storage.
 */
class MediaViewerViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository,
    removedMediaStore: RemovedMediaStore,
    private val mediaExporter: MediaExporter = MediaExporter(application.contentResolver)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<MediaViewerUiState>(MediaViewerUiState.Loading)
    val uiState: StateFlow<MediaViewerUiState> = _uiState.asStateFlow()

    private val _exportState = MutableStateFlow<MediaExportUiState>(MediaExportUiState.Idle)

    /** State of the Save/Download action. */
    val exportState: StateFlow<MediaExportUiState> = _exportState.asStateFlow()

    private val _deletionState = MutableStateFlow<MediaDeletionUiState>(MediaDeletionUiState.Idle)

    /** State of the schedule-deletion action. */
    val deletionState: StateFlow<MediaDeletionUiState> = _deletionState.asStateFlow()

    private var sakshiClient: SakshiClient? = null

    private val mediaDeletionRepository = MediaDeletionRepository(
        deleteMedia = { fileId ->
            sakshiClient?.deleteMedia(fileId)
                ?: SakshiResult.Failure(SakshiError.ServiceUnavailable("Vault is not connected"))
        },
        removedMediaStore = removedMediaStore
    )

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

    /**
     * Saves the currently loaded media file into public shared storage.
     *
     * Does nothing if media details are not loaded yet or an export is already running.
     */
    fun exportMedia() {
        val mediaDetails = (_uiState.value as? MediaViewerUiState.Success)?.mediaDetails ?: return
        if (_exportState.value is MediaExportUiState.InProgress) return
        _exportState.value = MediaExportUiState.InProgress

        viewModelScope.launch {
            _exportState.value = when (val result = mediaExporter.export(mediaDetails)) {
                is MediaExportResult.Success -> MediaExportUiState.Completed(result.relativePath + result.displayName)
                is MediaExportResult.Failure -> MediaExportUiState.Failed(result.message)
            }
        }
    }

    /** Resets a completed or failed export back to [MediaExportUiState.Idle] once its feedback was shown. */
    fun onExportResultShown() {
        if (_exportState.value is MediaExportUiState.Completed || _exportState.value is MediaExportUiState.Failed) {
            _exportState.value = MediaExportUiState.Idle
        }
    }

    /** Shows the deletion confirmation dialog for the loaded media file. */
    fun requestDeletion() {
        if (_uiState.value !is MediaViewerUiState.Success) return
        if (_deletionState.value == MediaDeletionUiState.Idle) {
            _deletionState.value = MediaDeletionUiState.Confirming
        }
    }

    /** Closes the confirmation dialog without changing the file. */
    fun cancelDeletion() {
        if (_deletionState.value == MediaDeletionUiState.Confirming) {
            _deletionState.value = MediaDeletionUiState.Idle
        }
    }

    /**
     * Asks Vault to schedule the loaded media file for deletion after the user confirmed.
     * The file is removed from the gallery only after Vault confirms.
     */
    fun confirmDeletion() {
        val fileId = (_uiState.value as? MediaViewerUiState.Success)?.mediaDetails?.fileId ?: return
        if (_deletionState.value != MediaDeletionUiState.Confirming) return
        _deletionState.value = MediaDeletionUiState.InProgress

        viewModelScope.launch {
            _deletionState.value = when (val result = mediaDeletionRepository.scheduleDeletion(fileId)) {
                ScheduleDeletionResult.Scheduled -> MediaDeletionUiState.Scheduled
                is ScheduleDeletionResult.Failed -> {
                    Log.e("MediaViewerViewModel", "Failed to schedule deletion: ${result.message}")
                    MediaDeletionUiState.Failed(result.message)
                }
            }
        }
    }

    /** Resets a failed deletion back to [MediaDeletionUiState.Idle] once its feedback was shown. */
    fun onDeletionFailureShown() {
        if (_deletionState.value is MediaDeletionUiState.Failed) {
            _deletionState.value = MediaDeletionUiState.Idle
        }
    }

    override fun onCleared() {
        super.onCleared()
        sakshiClient?.disconnect()
    }
}

class MediaViewerViewModelFactory(
    private val application: Application,
    private val settingsRepository: SettingsRepository,
    private val removedMediaStore: RemovedMediaStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaViewerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MediaViewerViewModel(application, settingsRepository, removedMediaStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
