/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.data.export

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import rajnishkmehta.sakshi.sdk.api.models.MediaDetailsResponse
import java.io.File
import kotlin.random.Random

/** Verifies [MediaExporter] against the real MediaStore of the device under test. */
@RunWith(AndroidJUnit4::class)
class MediaExporterTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resolver: ContentResolver = context.contentResolver
    private val exporter = MediaExporter(resolver)
    private val fileId = "sakshi_export_test_${System.currentTimeMillis()}"
    private val exportedUris = mutableListOf<Uri>()

    @After
    fun tearDown() {
        exportedUris.forEach { resolver.delete(it, null, null) }
    }

    @Test
    fun exportsEachMediaTypeIntoItsCollection() = runBlocking {
        val cases = listOf(
            Triple("PHOTO", "jpg", "Pictures/Sakshi/"),
            Triple("VIDEO", "mp4", "Movies/Sakshi/"),
            Triple("AUDIO", "m4a", "Music/Sakshi/"),
            Triple("OTHER", "pdf", "Download/Sakshi/")
        )
        for ((mediaType, extension, expectedPath) in cases) {
            val data = Random.nextBytes(300 * 1024)
            val result = exporter.export(details(sourceFile(data), mediaType, extension)) as MediaExportResult.Success
            exportedUris += result.uri

            assertEquals(expectedPath, result.relativePath)
            assertEquals("$fileId.$extension", result.displayName)
            assertEquals(data.size.toLong(), result.bytesCopied)
            assertArrayEquals(data, resolver.openInputStream(result.uri)!!.use { it.readBytes() })
            assertEquals(0, queryInt(result.uri, MediaStore.MediaColumns.IS_PENDING))
        }
    }

    @Test
    fun neverOverwritesExistingFile() = runBlocking {
        val first = Random.nextBytes(1024)
        val second = Random.nextBytes(2048)

        val firstResult = exporter.export(details(sourceFile(first), "PHOTO", "jpg")) as MediaExportResult.Success
        exportedUris += firstResult.uri
        val secondResult = exporter.export(details(sourceFile(second), "PHOTO", "jpg")) as MediaExportResult.Success
        exportedUris += secondResult.uri

        assertNotEquals(firstResult.displayName, secondResult.displayName)
        assertTrue(secondResult.displayName.endsWith(".jpg"))
        assertArrayEquals(first, resolver.openInputStream(firstResult.uri)!!.use { it.readBytes() })
        assertArrayEquals(second, resolver.openInputStream(secondResult.uri)!!.use { it.readBytes() })
    }

    @Test
    fun failedExportLeavesNoDestinationFile() = runBlocking {
        val missing = Uri.fromFile(File(context.cacheDir, "missing_$fileId.jpg"))

        val result = exporter.export(details(missing, "PHOTO", "jpg"))

        assertTrue(result is MediaExportResult.Failure)
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val matches = resolver.query(
            MediaStore.setIncludePending(collection),
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",
            arrayOf("$fileId%"),
            null
        )!!.use { it.count }
        assertEquals(0, matches)
    }

    private fun sourceFile(data: ByteArray): Uri {
        val file = File.createTempFile("source", ".bin", context.cacheDir)
        file.deleteOnExit()
        file.writeBytes(data)
        return Uri.fromFile(file)
    }

    private fun details(source: Uri, mediaType: String, extension: String) = MediaDetailsResponse(
        contentUri = source.toString(),
        fileId = fileId,
        mediaType = mediaType,
        fileExtension = extension,
        createdTime = System.currentTimeMillis()
    )

    private fun queryInt(uri: Uri, column: String): Int =
        resolver.query(uri, arrayOf(column), null, null, null)!!.use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
}
