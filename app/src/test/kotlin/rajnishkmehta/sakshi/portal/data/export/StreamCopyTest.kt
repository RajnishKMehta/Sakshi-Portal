/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.data.export

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.random.Random

class StreamCopyTest {

    @Test
    fun `copies all bytes across multiple chunks`() = runBlocking {
        val data = Random(42).nextBytes(EXPORT_BUFFER_SIZE * 3 + 123)
        val output = ByteArrayOutputStream()

        val copied = copyStreamCancellable(ByteArrayInputStream(data), output)

        assertEquals(data.size.toLong(), copied)
        assertArrayEquals(data, output.toByteArray())
    }

    @Test
    fun `copies empty stream`() = runBlocking {
        val output = ByteArrayOutputStream()
        assertEquals(0L, copyStreamCancellable(ByteArrayInputStream(ByteArray(0)), output))
        assertEquals(0, output.size())
    }

    @Test
    fun `reads in bounded chunks`() = runBlocking {
        var maxRequested = 0
        val input = object : InputStream() {
            private var remaining = 10_000
            override fun read(): Int = if (remaining-- > 0) 1 else -1
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                maxRequested = maxOf(maxRequested, len)
                if (remaining <= 0) return -1
                val n = minOf(len, remaining)
                remaining -= n
                return n
            }
        }

        val copied = copyStreamCancellable(input, ByteArrayOutputStream(), bufferSize = 1024)

        assertEquals(10_000L, copied)
        assertTrue(maxRequested <= 1024)
    }

    @Test
    fun `stops when coroutine is cancelled`() {
        var reads = 0
        assertThrows<CancellationException> {
            runBlocking {
                val job = coroutineContext[Job]!!
                val endless = object : InputStream() {
                    override fun read(): Int = 0
                    override fun read(b: ByteArray, off: Int, len: Int): Int {
                        if (++reads == 3) job.cancel()
                        return len
                    }
                }
                copyStreamCancellable(endless, ByteArrayOutputStream(), bufferSize = 16)
            }
        }
        assertEquals(3, reads)
    }

    @Test
    fun `rejects non positive buffer size`() {
        assertThrows<IllegalArgumentException> {
            runBlocking { copyStreamCancellable(ByteArrayInputStream(ByteArray(1)), ByteArrayOutputStream(), 0) }
        }
    }
}
