/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.ui.gallery

/**
 * Represents the various UI states for the Gallery screen.
 */
sealed interface GalleryUiState {
    /** State indicating that media data is currently being loaded. */
    data object Loading : GalleryUiState

    /**
     * State indicating a successful load of media data.
     *
     * @property media The list of parsed media items to display.
     */
    data class Success(val media: List<ParsedMediaItem>) : GalleryUiState

    /**
     * State indicating that an error occurred during loading.
     *
     * @property message The error message detailing what went wrong.
     */
    data class Error(val message: String) : GalleryUiState

    /** State indicating that the Vault app is not installed or unreachable. */
    data object VaultUnavailable : GalleryUiState
}

/**
 * Returns [state] without the media whose IDs are in [removedIds].
 * This is used to optimistically remove items from the UI immediately after deletion
 * or recovery actions, before a full list refresh occurs.
 *
 * Non-success states are returned unchanged.
 *
 * @param state The current gallery UI state.
 * @param removedIds A set of media IDs to exclude from the resulting state.
 * @return A new [GalleryUiState] with the specified items filtered out.
 */
fun excludeRemovedMedia(state: GalleryUiState, removedIds: Set<String>): GalleryUiState =
    if (state is GalleryUiState.Success && removedIds.isNotEmpty()) {
        state.copy(media = state.media.filterNot { it.id in removedIds })
    } else {
        state
    }
