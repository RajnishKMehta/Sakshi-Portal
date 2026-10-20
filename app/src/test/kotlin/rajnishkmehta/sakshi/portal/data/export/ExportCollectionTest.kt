/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.data.export

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExportCollectionTest {

    @Test
    fun `relative paths point to Sakshi folders`() {
        assertEquals("Pictures/Sakshi/", ExportCollection.IMAGES.relativePath)
        assertEquals("Movies/Sakshi/", ExportCollection.VIDEO.relativePath)
        assertEquals("Music/Sakshi/", ExportCollection.AUDIO.relativePath)
        assertEquals("Download/Sakshi/", ExportCollection.DOWNLOADS.relativePath)
    }

    @Test
    fun `media types map to their dedicated collections`() {
        assertEquals(ExportCollection.IMAGES, ExportCollection.forMedia("PHOTO", "image/jpeg"))
        assertEquals(ExportCollection.VIDEO, ExportCollection.forMedia("VIDEO", "video/mp4"))
        assertEquals(ExportCollection.AUDIO, ExportCollection.forMedia("AUDIO", "audio/mp4"))
        assertEquals(ExportCollection.DOWNLOADS, ExportCollection.forMedia("OTHER", "application/pdf"))
    }

    @Test
    fun `media type matching is case insensitive`() {
        assertEquals(ExportCollection.IMAGES, ExportCollection.forMedia("photo", "image/png"))
        assertEquals(ExportCollection.VIDEO, ExportCollection.forMedia("Video", "video/webm"))
    }

    @Test
    fun `unknown media type falls back to downloads`() {
        assertEquals(ExportCollection.DOWNLOADS, ExportCollection.forMedia("DOCUMENT", "image/jpeg"))
        assertEquals(ExportCollection.DOWNLOADS, ExportCollection.forMedia("", DEFAULT_EXPORT_MIME_TYPE))
    }

    @Test
    fun `mime type not accepted by dedicated collection falls back to downloads`() {
        assertEquals(ExportCollection.DOWNLOADS, ExportCollection.forMedia("PHOTO", DEFAULT_EXPORT_MIME_TYPE))
        assertEquals(ExportCollection.DOWNLOADS, ExportCollection.forMedia("VIDEO", "audio/mpeg"))
        assertEquals(ExportCollection.DOWNLOADS, ExportCollection.forMedia("AUDIO", "video/mp4"))
    }

    @Test
    fun `accepts checks the required mime prefix`() {
        assertTrue(ExportCollection.IMAGES.accepts("IMAGE/JPEG"))
        assertFalse(ExportCollection.IMAGES.accepts("video/mp4"))
        assertTrue(ExportCollection.DOWNLOADS.accepts("anything/at-all"))
    }
}
