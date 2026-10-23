/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal

import android.app.Application
import rajnishkmehta.sakshi.portal.debug.DebugLogger as Log

/**
 * Main Application class for the Portal app.
 * Initializes logging for the process.
 */
class PortalApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.init(this)
    }
}
