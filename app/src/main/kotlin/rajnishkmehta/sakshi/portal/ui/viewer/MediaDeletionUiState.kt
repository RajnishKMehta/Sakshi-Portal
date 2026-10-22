/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.ui.viewer

/** UI state of the "schedule deletion" action in the media viewer. */
sealed interface MediaDeletionUiState {
    /** No deletion request is pending. */
    data object Idle : MediaDeletionUiState

    /** The confirmation dialog is shown. */
    data object Confirming : MediaDeletionUiState

    /** Vault is scheduling the deletion. */
    data object InProgress : MediaDeletionUiState

    /** Vault confirmed that the file is scheduled for deletion. */
    data object Scheduled : MediaDeletionUiState

    /**
     * Scheduling failed and the file is unchanged.
     *
     * @property message A human-readable error description.
     */
    data class Failed(val message: String) : MediaDeletionUiState
}
