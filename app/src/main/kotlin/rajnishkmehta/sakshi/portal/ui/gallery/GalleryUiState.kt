/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.ui.gallery

sealed interface GalleryUiState {
    data object Loading : GalleryUiState
    data class Success(val media: List<ParsedMediaItem>) : GalleryUiState
    data class Error(val message: String) : GalleryUiState
    data object VaultUnavailable : GalleryUiState
}
