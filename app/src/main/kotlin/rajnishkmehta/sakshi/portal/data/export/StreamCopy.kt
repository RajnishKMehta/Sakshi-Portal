/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.data.export

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.InputStream
import java.io.OutputStream

/** Default buffer size used when streaming exported files. */
const val EXPORT_BUFFER_SIZE: Int = 64 * 1024

/**
 * Streams [input] into [output] in fixed-size chunks without loading the whole content into memory.
 *
 * The calling coroutine's cancellation is checked between chunks so long copies stop promptly.
 * Neither stream is closed by this function.
 *
 * @return The total number of bytes copied.
 */
suspend fun copyStreamCancellable(
    input: InputStream,
    output: OutputStream,
    bufferSize: Int = EXPORT_BUFFER_SIZE
): Long {
    require(bufferSize > 0) { "bufferSize must be positive" }
    val context = currentCoroutineContext()
    val buffer = ByteArray(bufferSize)
    var total = 0L
    while (true) {
        context.ensureActive()
        val read = input.read(buffer)
        if (read < 0) break
        output.write(buffer, 0, read)
        total += read
    }
    output.flush()
    return total
}
