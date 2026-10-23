/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.ui.viewer

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import rajnishkmehta.sakshi.portal.R

/**
 * Confirmation dialog for scheduling the current media file for deletion.
 *
 * Explains that the file is not deleted immediately but permanently deleted by Vault after the
 * retention period. Dismissing the dialog leaves the file unchanged.
 */
@Composable
fun ScheduleDeletionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(painterResource(R.drawable.ic_trash_clock), contentDescription = null) },
        title = { Text(stringResource(R.string.viewer_schedule_deletion_title)) },
        text = { Text(stringResource(R.string.viewer_schedule_deletion_message)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(R.string.viewer_schedule_deletion_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.viewer_schedule_deletion_cancel))
            }
        }
    )
}

/**
 * Dialog to confirm the recovery of media scheduled for deletion.
 *
 * @param onConfirm Callback invoked when the user confirms the recovery action.
 * @param onDismiss Callback invoked when the user dismisses the dialog without confirming.
 */
@Composable
fun RecoverMediaDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(painterResource(R.drawable.ic_restore), contentDescription = null) },
        title = { Text(stringResource(R.string.viewer_recover_title)) },
        text = { Text(stringResource(R.string.viewer_recover_message)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Recover")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.viewer_schedule_deletion_cancel))
            }
        }
    )
}
