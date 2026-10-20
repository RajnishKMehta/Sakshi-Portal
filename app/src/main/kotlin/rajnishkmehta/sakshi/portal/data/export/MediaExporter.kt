/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.data.export

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import rajnishkmehta.sakshi.sdk.api.models.MediaDetailsResponse
import rajnishkmehta.sakshi.portal.debug.DebugLogger as Log

/**
 * Exports a single Vault media file into public shared storage through MediaStore.
 *
 * The original file is read from [MediaDetailsResponse.contentUri] and streamed into a new
 * MediaStore item owned by this app, so no storage permissions are required on Android 10+.
 * The new item is marked `IS_PENDING` while it is written and published only after a successful
 * copy; on any failure or cancellation the incomplete item is deleted.
 *
 * @param contentResolver Resolver used for both reading the source and writing the destination.
 * @param ioDispatcher Dispatcher used for all blocking I/O.
 */
class MediaExporter(
    private val contentResolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Copies the file described by [mediaDetails] into the matching shared-storage collection
     * (see [ExportCollection.forMedia]). Existing files are never overwritten.
     *
     * @throws CancellationException if the calling coroutine is cancelled; the partial file is removed first.
     */
    suspend fun export(mediaDetails: MediaDetailsResponse): MediaExportResult = withContext(ioDispatcher) {
        val extension = normalizeExtension(mediaDetails.fileExtension)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: DEFAULT_EXPORT_MIME_TYPE
        val collection = ExportCollection.forMedia(mediaDetails.mediaType, mimeType)
        val requestedName = buildExportFileName(mediaDetails.fileId, mediaDetails.fileExtension)

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, requestedName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, collection.relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val destination = try {
            contentResolver.insert(collection.contentUri(), values)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create export destination: ${e.message}")
            return@withContext MediaExportResult.Failure(e.message ?: "Unable to create destination file", e)
        } ?: return@withContext MediaExportResult.Failure("Unable to create destination file")

        try {
            val bytesCopied = copyInto(mediaDetails.contentUri.toUri(), destination)

            val published = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            check(contentResolver.update(destination, published, null, null) > 0) {
                "Unable to publish exported file"
            }

            val (displayName, relativePath) = queryLocation(destination)
                ?: (requestedName to collection.relativePath)
            Log.d(TAG, "Exported ${mediaDetails.fileId} to $relativePath$displayName ($bytesCopied bytes)")
            MediaExportResult.Success(destination, displayName, relativePath, bytesCopied)
        } catch (e: Throwable) {
            withContext(NonCancellable) { deleteQuietly(destination) }
            if (e is CancellationException) throw e
            Log.e(TAG, "Export of ${mediaDetails.fileId} failed: ${e.message}")
            MediaExportResult.Failure(e.message ?: "Export failed", e)
        }
    }

    private suspend fun copyInto(source: Uri, destination: Uri): Long {
        val input = contentResolver.openInputStream(source)
            ?: throw IllegalStateException("Unable to open source file")
        return input.use {
            val output = contentResolver.openOutputStream(destination, "w")
                ?: throw IllegalStateException("Unable to open destination file")
            output.use { copyStreamCancellable(input, it) }
        }
    }

    private fun queryLocation(uri: Uri): Pair<String, String>? = try {
        contentResolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.RELATIVE_PATH),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val name = cursor.getString(0)
                val path = cursor.getString(1)
                if (name != null && path != null) name to path else null
            } else {
                null
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "Unable to query exported file location: ${e.message}")
        null
    }

    private fun deleteQuietly(uri: Uri) {
        try {
            contentResolver.delete(uri, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete incomplete export $uri: ${e.message}")
        }
    }

    private fun ExportCollection.contentUri(): Uri = when (this) {
        ExportCollection.IMAGES -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        ExportCollection.VIDEO -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        ExportCollection.AUDIO -> MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        ExportCollection.DOWNLOADS -> MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    }

    private companion object {
        const val TAG = "MediaExporter"
    }
}
