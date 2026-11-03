/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.ui.importing

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rajnishkmehta.sakshi.portal.data.SettingsRepository
import rajnishkmehta.sakshi.sdk.api.SakshiClient
import rajnishkmehta.sakshi.sdk.api.SakshiClientConfig
import rajnishkmehta.sakshi.sdk.api.models.FileCopyRequest
import rajnishkmehta.sakshi.portal.debug.DebugLogger as Log
import java.util.UUID
import rajnishkmehta.sakshi.portal.utils.MediaUtils
import kotlinx.coroutines.flow.firstOrNull
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CancellationException

class MediaImportViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<MediaImportUiState>(MediaImportUiState.Idle)
    val uiState: StateFlow<MediaImportUiState> = _uiState.asStateFlow()

    private val _isMinimized = MutableStateFlow(false)
    val isMinimized: StateFlow<Boolean> = _isMinimized.asStateFlow()

    fun setMinimized(minimized: Boolean) {
        _isMinimized.value = minimized
    }

    fun dismiss() {
        _uiState.value = MediaImportUiState.Idle
        _isMinimized.value = false
    }

    private var importJob: kotlinx.coroutines.Job? = null

    fun importMedia(uris: List<Uri>) {
        if (uris.isEmpty()) return
        if (importJob?.isActive == true) return

        importJob = viewModelScope.launch {
            val total = uris.size
            var completed = 0
            var failed = 0

            _uiState.value = MediaImportUiState.InProgress(total, completed, "Preparing...", failed)

            val app = getApplication<Application>()

            var vaultPackage = settingsRepository.vaultPackageFlow.firstOrNull()
            if (vaultPackage == null) {
                vaultPackage = "rajnishkmehta.sakshi.vault"
            }

            val config = SakshiClientConfig(
                vaultPackageName = vaultPackage,
                connectionTimeoutMs = 5000L
            )

            val client = SakshiClient.create(app, config)

            try {
                for (uri in uris) {
                    try {
                        var fileName = "unknown"
                        try {
                            app.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                    if (nameIndex != -1) {
                                        fileName = cursor.getString(nameIndex)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("MediaImportViewModel", "Error getting file name for $uri: ${e.message}")
                        }

                        _uiState.value = MediaImportUiState.InProgress(total, completed, fileName, failed)

                        try {
                            app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        } catch (e: SecurityException) {
                            // Ignore if we can't take persistable permission
                        }

                        app.grantUriPermission(vaultPackage, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

                        val mediaType = MediaUtils.getMediaType(app, uri)

                        var fileExtension = fileName.substringAfterLast('.', "")
                        try {
                            rajnishkmehta.sakshi.sdk.api.validation.PathValidator.validateFileExtension(fileExtension)
                        } catch (e: IllegalArgumentException) {
                            fileExtension = when (mediaType) {
                                "PHOTO" -> "jpg"
                                "VIDEO" -> "mp4"
                                "AUDIO" -> "m4a"
                                else -> "bin"
                            }
                        }

                        val nameWithoutExtension = fileName.substringBeforeLast('.', fileName)
                        val sanitizedFileName = nameWithoutExtension.lowercase().replace(Regex("[^a-z0-9_-]"), "")
                        val limitedFileName = if (sanitizedFileName.length > 50) sanitizedFileName.substring(0, 50) else sanitizedFileName

                        val prefix = when (mediaType) {
                            "PHOTO" -> "img_"
                            "VIDEO" -> "vid_"
                            "AUDIO" -> "aud_"
                            else -> "oth_"
                        }

                        var baseFileId = prefix + limitedFileName
                        if (baseFileId.length < 8) {
                            baseFileId += UUID.randomUUID().toString().replace("-", "").substring(0, 8 - baseFileId.length)
                        }

                        var success = false
                        var attempts = 0

                        while (!success && attempts < 3) {
                            val currentFileId = if (attempts == 0) {
                                baseFileId
                            } else {
                                prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                            }

                            val request = FileCopyRequest(
                                fileId = currentFileId,
                                uri = uri,
                                mediaType = mediaType,
                                fileExtension = fileExtension
                            )

                            val result = client.copyFile(request)
                            if (result is rajnishkmehta.sakshi.sdk.api.SakshiResult.Failure) {
                                if (result.error.errorCode == rajnishkmehta.sakshi.sdk.api.SakshiErrorCode.DUPLICATE_FILE_ID) {
                                    attempts++
                                } else {
                                    Log.e("MediaImportViewModel", "File ingestion failed: " + result.error.message)
                                    break
                                }
                            } else {
                                success = true
                            }
                        }

                        if (success) {
                            completed++
                        } else {
                            failed++
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        failed++
                    }
                }
                _uiState.value = MediaImportUiState.Completed(total, completed, failed)
            } finally {
                client.disconnect()
            }
        }
    }
}

class MediaImportViewModelFactory(
    private val application: Application,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaImportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MediaImportViewModel(application, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
