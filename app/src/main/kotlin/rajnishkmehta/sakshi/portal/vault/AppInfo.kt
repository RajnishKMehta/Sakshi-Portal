/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.vault

/**
 * Represents information about a discovered Vault application installed on the device.
 *
 * @property name The human-readable application label.
 * @property packageName The application's unique package name.
 * @property hasLauncherActivity True if the application has a resolvable launcher activity, indicating it is a standard app rather than a headless service.
 */
data class AppInfo(
    val name: String,
    val packageName: String,
    val hasLauncherActivity: Boolean
)
