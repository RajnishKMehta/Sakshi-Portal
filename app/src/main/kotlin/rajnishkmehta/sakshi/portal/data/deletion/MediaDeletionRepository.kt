/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.data.deletion

import kotlinx.coroutines.CancellationException
import rajnishkmehta.sakshi.sdk.api.SakshiResult
import rajnishkmehta.sakshi.sdk.api.models.BatchMediaDeleteResponse
import rajnishkmehta.sakshi.sdk.api.models.BatchMediaRecoverResponse

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
 * Represents the result of a batch schedule deletion operation.
 */
sealed interface BatchScheduleDeletionResult {
    /**
     * The batch request was successfully processed by the Vault, though individual items
     * within the batch may have been rejected.
     *
     * Note: This variant is returned even if every single ID in the batch failed, as long
     * as the batch request itself was successfully processed by the Vault.
     *
     * @property successfulIds The list of file IDs that were successfully scheduled for deletion.
     * @property failedIds A map of file IDs to error messages for items that were rejected.
     */
    data class Success(val successfulIds: List<String>, val failedIds: Map<String, String>) : BatchScheduleDeletionResult

    /**
     * Vault rejected the batch request entirely or could not be reached.
     *
     * @property message A human-readable error description.
     */
    data class Failed(val message: String) : BatchScheduleDeletionResult
}

/**
 * Represents the result of a batch recover media operation.
 */
sealed interface BatchRecoverMediaResult {
    /**
     * The batch request was successfully processed by the Vault, though individual items
     * within the batch may have been rejected.
     *
     * Note: This variant is returned even if every single ID in the batch failed, as long
     * as the batch request itself was successfully processed by the Vault.
     *
     * @property successfulIds The list of file IDs that were successfully recovered.
     * @property failedIds A map of file IDs to error messages for items that were rejected.
     */
    data class Success(val successfulIds: List<String>, val failedIds: Map<String, String>) : BatchRecoverMediaResult

    /**
     * Vault rejected the batch request entirely or could not be reached.
     *
     * @property message A human-readable error description.
     */
    data class Failed(val message: String) : BatchRecoverMediaResult
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
 * @param batchDeleteMedia Calls the SDK's `SakshiClient.deleteMedia(fileIds)`.
 * @param batchRecoverMedia Calls the SDK's `SakshiClient.recoverMedia(fileIds)`.
 * @param removedMediaStore Store notified after Vault confirmed scheduling.
 */
class MediaDeletionRepository(
    private val deleteMedia: suspend (String) -> SakshiResult<Unit>,
    private val recoverMedia: suspend (String) -> SakshiResult<Unit>,
    private val batchDeleteMedia: suspend (List<String>) -> SakshiResult<BatchMediaDeleteResponse>,
    private val batchRecoverMedia: suspend (List<String>) -> SakshiResult<BatchMediaRecoverResponse>,
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
            is SakshiResult.Failure -> ScheduleDeletionResult.Failed(rajnishkmehta.sakshi.portal.ui.common.ErrorMapper.getMessage(null, result.error))
        }
    }

    /**
     * Recovers [fileId] from scheduled deletion. Does not report success until Vault confirms it.
     * Marks the item as removed from the scheduled deletions UI.
     */
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

    /**
     * Schedules a batch of media files for deletion in Vault and removes them from the Portal gallery
     * once Vault confirms.
     *
     * If the batch request is successful, only the [BatchScheduleDeletionResult.Success.successfulIds]
     * are marked as removed in the local store. Coroutine cancellation exceptions are propagated.
     *
     * @param fileIds The list of file IDs to schedule for deletion.
     * @return The result of the batch schedule deletion operation.
     */
    suspend fun batchScheduleDeletion(fileIds: List<String>): BatchScheduleDeletionResult {
        val result = try {
            batchDeleteMedia(fileIds)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return BatchScheduleDeletionResult.Failed(e.message ?: DEFAULT_ERROR)
        }

        return when (result) {
            is SakshiResult.Success -> {
                val data = result.data
                data.successfulIds.forEach { removedMediaStore.markRemoved(it) }
                BatchScheduleDeletionResult.Success(data.successfulIds, data.failedIds)
            }
            is SakshiResult.Failure -> BatchScheduleDeletionResult.Failed(rajnishkmehta.sakshi.portal.ui.common.ErrorMapper.getMessage(null, result.error))
        }
    }

    /**
     * Recovers a batch of scheduled media files in Vault and removes them from the Scheduled Deletions screen.
     *
     * If the batch request is successful, only the [BatchRecoverMediaResult.Success.successfulIds]
     * are marked as removed in the local store. Coroutine cancellation exceptions are propagated.
     *
     * @param fileIds The list of file IDs to recover.
     * @return The result of the batch recover media operation.
     */
    suspend fun batchRecoverMediaAction(fileIds: List<String>): BatchRecoverMediaResult {
        val result = try {
            batchRecoverMedia(fileIds)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return BatchRecoverMediaResult.Failed(e.message ?: "Failed to recover files")
        }

        return when (result) {
            is SakshiResult.Success -> {
                val data = result.data
                data.successfulIds.forEach { removedMediaStore.markRemoved(it) }
                BatchRecoverMediaResult.Success(data.successfulIds, data.failedIds)
            }
            is SakshiResult.Failure -> BatchRecoverMediaResult.Failed(rajnishkmehta.sakshi.portal.ui.common.ErrorMapper.getMessage(null, result.error))
        }
    }

    private companion object {
        const val DEFAULT_ERROR = "Failed to schedule deletion"
    }
}