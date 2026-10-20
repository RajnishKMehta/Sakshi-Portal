/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.ui.viewer

/** UI state of the single-file Save/Download action in the media viewer. */
sealed interface MediaExportUiState {
    /** No export is running and there is no pending result to show. */
    data object Idle : MediaExportUiState

    /** The file is being copied to shared storage. */
    data object InProgress : MediaExportUiState

    /**
     * The export finished successfully.
     *
     * @property location The user-visible location of the saved file, e.g. `Pictures/Sakshi/abc.jpg`.
     */
    data class Completed(val location: String) : MediaExportUiState

    /**
     * The export failed.
     *
     * @property message A human-readable error description.
     */
    data class Failed(val message: String) : MediaExportUiState
}
