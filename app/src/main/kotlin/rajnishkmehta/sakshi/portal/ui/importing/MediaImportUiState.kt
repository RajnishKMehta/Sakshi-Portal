/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.ui.importing

import android.net.Uri

sealed interface MediaImportUiState {
    data object Idle : MediaImportUiState

    data class InProgress(
        val totalFiles: Int,
        val completedFiles: Int,
        val currentFileName: String,
        val failedFiles: Int
    ) : MediaImportUiState

    data class Completed(
        val totalFiles: Int,
        val successfulFiles: Int,
        val failedFiles: Int
    ) : MediaImportUiState
}
