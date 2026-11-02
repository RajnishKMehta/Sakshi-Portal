package rajnishkmehta.sakshi.portal.ui.common

import android.content.Context
import rajnishkmehta.sakshi.portal.R
import rajnishkmehta.sakshi.sdk.api.SakshiError

/**
 * Maps SakshiError types to localized, user-friendly UI messages.
 */
object ErrorMapper {
    /**
     * Returns a localized error message for the given error.
     *
     * @param context Nullable context. If null, an English fallback is returned.
     * @param error The nullable error to map. If null, a generic unknown error message is returned.
     */
    fun getMessage(context: Context?, error: Throwable?): String {
        if (error == null) return context?.getString(R.string.unknown_error) ?: "Unknown error"

        return when (error) {
            is SakshiError.VaultNotInstalled -> context?.getString(R.string.error_vault_not_installed) ?: "Vault application is not installed on this device."
            is SakshiError.ServiceUnavailable -> context?.getString(R.string.error_service_unavailable) ?: "Vault service connection could not be established."
            is SakshiError.ServiceDisconnected -> context?.getString(R.string.error_service_disconnected) ?: "Vault service disconnected unexpectedly."
            is SakshiError.PermissionDenied -> context?.getString(R.string.error_permission_denied) ?: "Permission denied."
            is SakshiError.RecordingNotFound -> context?.getString(R.string.error_recording_not_found) ?: "Media not found."
            is SakshiError.InvalidPayload -> context?.getString(R.string.error_invalid_payload) ?: "Invalid request payload."
            is SakshiError.DuplicateFileId -> context?.getString(R.string.error_duplicate_file_id) ?: "Media already exists."
            is SakshiError.SourceFileInaccessible -> context?.getString(R.string.error_source_file_inaccessible) ?: "Source file is inaccessible."
            is SakshiError.IpcError -> context?.getString(R.string.error_ipc_error) ?: "Vault operation failed."
            is SakshiError.Unknown -> context?.getString(R.string.error_unknown_sakshi, error.message) ?: "Unknown error: ${error.message}"
            else -> error.message ?: context?.getString(R.string.unknown_error) ?: "Unknown error"
        }
    }
}
