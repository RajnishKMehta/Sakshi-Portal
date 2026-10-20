/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.data.export

import android.net.Uri

/** Outcome of exporting a single Vault file to shared storage. */
sealed interface MediaExportResult {
    /**
     * The file was fully written and published.
     *
     * @property uri The MediaStore URI of the exported file.
     * @property displayName The final file name, which may differ from the requested one if MediaStore renamed it.
     * @property relativePath The shared-storage folder the file was written to, e.g. `Pictures/Sakshi/`.
     * @property bytesCopied The number of bytes written.
     */
    data class Success(
        val uri: Uri,
        val displayName: String,
        val relativePath: String,
        val bytesCopied: Long
    ) : MediaExportResult

    /**
     * The export failed and any partially written destination file was removed.
     *
     * @property message A human-readable error description.
     * @property cause The underlying exception, if any.
     */
    data class Failure(val message: String, val cause: Throwable? = null) : MediaExportResult
}
