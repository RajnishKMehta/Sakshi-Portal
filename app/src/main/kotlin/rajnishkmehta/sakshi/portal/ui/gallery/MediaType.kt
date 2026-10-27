/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.ui.gallery

/**
 * Represents the type of media file supported by the Vault.
 * Used for filtering and selecting appropriate viewers and placeholder icons.
 */
enum class MediaType {
    /** Represents all media types (typically used as a filter state). */
    ALL,
    /** Represents image files. */
    PHOTO,
    /** Represents video files. */
    VIDEO,
    /** Represents audio files. */
    AUDIO,
    /** Represents any other file types that don't fit the specific categories. */
    OTHER
}
