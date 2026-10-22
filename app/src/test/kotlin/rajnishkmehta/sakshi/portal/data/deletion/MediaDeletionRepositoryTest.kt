/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.data.deletion

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import rajnishkmehta.sakshi.sdk.api.SakshiError
import rajnishkmehta.sakshi.sdk.api.SakshiResult

class MediaDeletionRepositoryTest {

    private val store = RemovedMediaStore()

    @Test
    fun `success hides the file only after Vault confirmed`() = runBlocking {
        val requested = mutableListOf<String>()
        val repository = MediaDeletionRepository(
            deleteMedia = { id ->
                requested += id
                assertTrue(store.removedIds.value.isEmpty())
                SakshiResult.Success(Unit)
            },
            removedMediaStore = store
        )

        assertEquals(ScheduleDeletionResult.Scheduled, repository.scheduleDeletion("photo_0001"))
        assertEquals(listOf("photo_0001"), requested)
        assertEquals(setOf("photo_0001"), store.removedIds.value)
    }

    @Test
    fun `Vault rejection keeps the file visible`() = runBlocking {
        val repository = MediaDeletionRepository(
            deleteMedia = { SakshiResult.Failure(SakshiError.IpcError(message = "Media file is still being synchronized", cause = null)) },
            removedMediaStore = store
        )

        assertEquals(
            ScheduleDeletionResult.Failed("Media file is still being synchronized"),
            repository.scheduleDeletion("video_0001")
        )
        assertTrue(store.removedIds.value.isEmpty())
    }

    @Test
    fun `unexpected exception is reported as failure`() = runBlocking {
        val repository = MediaDeletionRepository(
            deleteMedia = { throw IllegalStateException("binder died") },
            removedMediaStore = store
        )

        assertEquals(ScheduleDeletionResult.Failed("binder died"), repository.scheduleDeletion("photo_0001"))
        assertTrue(store.removedIds.value.isEmpty())
    }
}
