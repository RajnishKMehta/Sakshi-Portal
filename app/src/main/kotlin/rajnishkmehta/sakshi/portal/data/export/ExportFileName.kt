/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.data.export

/** MIME type used when the file extension does not map to a known type. */
const val DEFAULT_EXPORT_MIME_TYPE = "application/octet-stream"

private val INVALID_FILE_NAME_CHARS = Regex("""[\u0000-\u001F\u007F/\\:*?"<>|]""")

/**
 * Builds the requested display name for an exported file: `fileId + "." + fileExtension`.
 *
 * Characters that are not allowed in shared-storage file names are replaced with `_`, and leading
 * dots are removed from the extension. If the extension is blank, only the file ID is used.
 *
 * Name collisions are not handled here: MediaStore automatically makes the display name unique
 * (for example `name (1).jpg`) when a file with the same name already exists, so an existing file
 * is never overwritten.
 */
fun buildExportFileName(fileId: String, fileExtension: String): String {
    val baseName = sanitizeFileNamePart(fileId).ifBlank { "sakshi_export" }
    val extension = sanitizeFileNamePart(fileExtension.trim().trimStart('.'))
    return if (extension.isBlank()) baseName else "$baseName.$extension"
}

/** Normalizes [fileExtension] into the lowercase form used for MIME type lookups. */
fun normalizeExtension(fileExtension: String): String =
    fileExtension.trim().trimStart('.').lowercase()

private fun sanitizeFileNamePart(value: String): String =
    value.trim().replace(INVALID_FILE_NAME_CHARS, "_")
