/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.data.deletion

import kotlinx.coroutines.CancellationException
import rajnishkmehta.sakshi.sdk.api.SakshiResult

/** Result of asking Vault to schedule a media file for deletion. */
sealed interface ScheduleDeletionResult {
    /** Vault confirmed that the file is scheduled for permanent deletion. */
    data object Scheduled : ScheduleDeletionResult

    /**
     * Vault rejected the request or could not be reached. The file is unchanged.
     *
     * @property message A human-readable error description.
     */
    data class Failed(val message: String) : ScheduleDeletionResult
}

/** Result of asking Vault to recover a scheduled deletion. */
sealed interface RecoverMediaResult {
    /** Vault confirmed that the file is recovered. */
    data object Recovered : RecoverMediaResult

    /**
     * Vault rejected the request or could not be reached.
     *
     * @property message A human-readable error description.
     */
    data class Failed(val message: String) : RecoverMediaResult
}

/**
 * Schedules single media files for deletion in Vault and removes them from the Portal gallery
 * once Vault confirms.
 *
 * Only the Vault copy is affected: Vault deletes its private file after the retention period.
 * Public exported copies and original Camera source files are never touched.
 *
 * @param deleteMedia Calls the SDK's `SakshiClient.deleteMedia(fileId)`.
 * @param recoverMedia Calls the SDK's `SakshiClient.recoverMedia(fileId)`.
 * @param removedMediaStore Store notified after Vault confirmed scheduling.
 */
class MediaDeletionRepository(
    private val deleteMedia: suspend (String) -> SakshiResult<Unit>,
    private val recoverMedia: suspend (String) -> SakshiResult<Unit>,
    private val removedMediaStore: RemovedMediaStore
) {

    /** Schedules [fileId] for deletion. Never reports success before Vault confirmed it. */
    suspend fun scheduleDeletion(fileId: String): ScheduleDeletionResult {
        val result = try {
            deleteMedia(fileId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return ScheduleDeletionResult.Failed(e.message ?: DEFAULT_ERROR)
        }

        return when (result) {
            is SakshiResult.Success<*> -> {
                removedMediaStore.markRemoved(fileId)
                ScheduleDeletionResult.Scheduled
            }
            is SakshiResult.Failure -> ScheduleDeletionResult.Failed(result.error.message ?: DEFAULT_ERROR)
        }
    }

    /** Recovers [fileId] from scheduled deletion. */
    suspend fun recoverMediaAction(fileId: String): RecoverMediaResult {
        val result = try {
            recoverMedia(fileId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return RecoverMediaResult.Failed(e.message ?: "Failed to recover file")
        }

        return if (result.isSuccess) {
            removedMediaStore.markRemoved(fileId) // Hide it from the Scheduled Deletions screen immediately
            RecoverMediaResult.Recovered
        } else {
            RecoverMediaResult.Failed(result.errorOrNull()?.message ?: "Failed to recover file")
        }
    }

    private companion object {
        const val DEFAULT_ERROR = "Failed to schedule deletion"
    }
}
