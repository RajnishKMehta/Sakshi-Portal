/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.ui

import androidx.lifecycle.ViewModel
import rajnishkmehta.sakshi.portal.data.deletion.RemovedMediaStore

/**
 * Activity-scoped ViewModel that retains the [RemovedMediaStore] shared by the gallery and viewer
 * across configuration changes.
 */
class RemovedMediaViewModel : ViewModel() {

    /** Store shared by all screens of the hosting activity. */
    val removedMediaStore: RemovedMediaStore = RemovedMediaStore()
}
