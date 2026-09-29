/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.debug

import android.content.Context

/**
 * A no-op implementation of the debug logger for release builds.
 */
object DebugLogger {

    @JvmStatic
    fun init(context: Context) {
        // No-op in release
    }

    @JvmStatic
    fun d(tag: String = "SakshiPortal", message: String, throwable: Throwable? = null) {
        // No-op in release
    }

    @JvmStatic
    fun i(tag: String = "SakshiPortal", message: String, throwable: Throwable? = null) {
        // No-op in release
    }

    @JvmStatic
    fun w(tag: String = "SakshiPortal", message: String, throwable: Throwable? = null) {
        // No-op in release
    }

    @JvmStatic
    fun e(tag: String = "SakshiPortal", message: String, throwable: Throwable? = null) {
        // No-op in release
    }
}
