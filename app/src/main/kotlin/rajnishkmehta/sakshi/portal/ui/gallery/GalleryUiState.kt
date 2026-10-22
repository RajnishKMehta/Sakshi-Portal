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

/**
 * Returns [state] without the media whose IDs are in [removedIds]. Non-success states are returned unchanged.
 */
fun excludeRemovedMedia(state: GalleryUiState, removedIds: Set<String>): GalleryUiState =
    if (state is GalleryUiState.Success && removedIds.isNotEmpty()) {
        state.copy(media = state.media.filterNot { it.id in removedIds })
    } else {
        state
    }
