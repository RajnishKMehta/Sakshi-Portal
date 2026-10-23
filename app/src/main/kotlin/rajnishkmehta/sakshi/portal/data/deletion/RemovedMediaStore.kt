/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.data.deletion

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update

/**
 * In-memory store of media IDs that Vault confirmed as scheduled for deletion or recovered during this session.
 *
 * The gallery hides these IDs immediately to update the UI optimistically without waiting for the next `listMedia` refresh
 * (which naturally excludes them based on the current mode).
 */
class RemovedMediaStore {

    private val _removedIds = MutableStateFlow<Set<String>>(emptySet())

    /** IDs of media removed from the gallery. */
    val removedIds: StateFlow<Set<String>> = _removedIds.asStateFlow()

    /** Hides [fileId] from the gallery. */
    fun markRemoved(fileId: String) {
        _removedIds.update { it + fileId }
    }

    /**
     * Removes all IDs from the store, e.g. before the gallery is refreshed from Vault.
     *
     * @return The IDs that were in the store before clearing.
     */
    fun clear(): Set<String> = _removedIds.getAndUpdate { emptySet() }
}
