/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.data.export

/**
 * Shared-storage MediaStore collections that a single Vault file can be exported into.
 *
 * Relative paths match the standard `Environment.DIRECTORY_*` folders.
 *
 * @property relativePath The `RELATIVE_PATH` (with trailing slash) under which exported files are stored.
 * @property requiredMimePrefix The MIME type prefix MediaStore requires for this collection,
 * or `null` when any MIME type is accepted.
 */
enum class ExportCollection(val relativePath: String, val requiredMimePrefix: String?) {
    /** `MediaStore.Images.Media`, stored in `Pictures/Sakshi/`. */
    IMAGES("Pictures/$EXPORT_FOLDER/", "image/"),

    /** `MediaStore.Video.Media`, stored in `Movies/Sakshi/`. */
    VIDEO("Movies/$EXPORT_FOLDER/", "video/"),

    /** `MediaStore.Audio.Media`, stored in `Music/Sakshi/`. */
    AUDIO("Music/$EXPORT_FOLDER/", "audio/"),

    /** `MediaStore.Downloads`, stored in `Download/Sakshi/`. Accepts any MIME type. */
    DOWNLOADS("Download/$EXPORT_FOLDER/", null);

    /** Returns `true` if MediaStore accepts [mimeType] in this collection. */
    fun accepts(mimeType: String): Boolean =
        requiredMimePrefix == null || mimeType.startsWith(requiredMimePrefix, ignoreCase = true)

    companion object {
        /**
         * Selects the destination collection for a Vault media item.
         *
         * `PHOTO`, `VIDEO` and `AUDIO` map to their dedicated collections; everything else goes to
         * [DOWNLOADS]. If [mimeType] is not accepted by the dedicated collection (for example an
         * unusual extension), [DOWNLOADS] is used so the export can still succeed.
         *
         * @param mediaType The Vault media type (`PHOTO`, `VIDEO`, `AUDIO` or `OTHER`), case-insensitive.
         * @param mimeType The MIME type that will be written for the exported file.
         */
        fun forMedia(mediaType: String, mimeType: String): ExportCollection {
            val preferred = when (mediaType.uppercase()) {
                "PHOTO" -> IMAGES
                "VIDEO" -> VIDEO
                "AUDIO" -> AUDIO
                else -> DOWNLOADS
            }
            return if (preferred.accepts(mimeType)) preferred else DOWNLOADS
        }
    }
}

private const val EXPORT_FOLDER = "Sakshi"
