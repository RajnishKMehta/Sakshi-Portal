/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.ui

import androidx.lifecycle.ViewModel
import rajnishkmehta.sakshi.portal.data.deletion.RemovedMediaStore

/**
 * Activity-scoped ViewModel that retains the [RemovedMediaStore] shared by the gallery and viewer
 * across configuration changes and navigation events.
 *
 * This ensures that when media is scheduled for deletion or recovered in the viewer, the gallery
 * can instantly hide it upon return, maintaining a consistent UI state without requiring an
 * immediate full data reload from the Vault.
 */
class RemovedMediaViewModel : ViewModel() {

    /**
     * Store shared by all screens of the hosting activity. Keeps track of media IDs that
     * should be temporarily hidden from the gallery until the next successful refresh.
     */
    val removedMediaStore: RemovedMediaStore = RemovedMediaStore()
}
