/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.utils

import android.content.Context
import android.net.Uri

object MediaUtils {
    fun getMediaType(context: Context, uri: Uri): String {
        val mimeType = context.contentResolver.getType(uri) ?: ""
        return when {
            mimeType.startsWith("image/") -> "PHOTO"
            mimeType.startsWith("video/") -> "VIDEO"
            mimeType.startsWith("audio/") -> "AUDIO"
            else -> "OTHER"
        }
    }
}
