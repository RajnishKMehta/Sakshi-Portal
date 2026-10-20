/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.data.export

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExportFileNameTest {

    @Test
    fun `file name is file id plus extension`() {
        assertEquals("photo_2026_1001.jpg", buildExportFileName("photo_2026_1001", "jpg"))
    }

    @Test
    fun `leading dots and whitespace are removed from extension`() {
        assertEquals("clip.mp4", buildExportFileName("clip", " .mp4 "))
    }

    @Test
    fun `blank extension yields file id only`() {
        assertEquals("note", buildExportFileName("note", ""))
        assertEquals("note", buildExportFileName("note", " . "))
    }

    @Test
    fun `invalid characters are replaced`() {
        assertEquals("a_b_c_d.jpg", buildExportFileName("a/b\\c:d", "jpg"))
        assertEquals("x_y.m_a", buildExportFileName("x\u0000y", "m*a"))
    }

    @Test
    fun `blank file id gets a fallback name`() {
        assertEquals("sakshi_export.pdf", buildExportFileName("  ", "pdf"))
    }

    @Test
    fun `extension case is preserved in file name but normalized for lookup`() {
        assertEquals("img.JPG", buildExportFileName("img", "JPG"))
        assertEquals("jpg", normalizeExtension(" .JPG"))
    }
}
