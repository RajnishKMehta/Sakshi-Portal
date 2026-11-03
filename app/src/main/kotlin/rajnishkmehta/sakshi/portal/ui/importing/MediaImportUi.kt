/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.ui.importing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import rajnishkmehta.sakshi.portal.R

@Composable
fun MediaImportUi(
    uiState: MediaImportUiState,
    isMinimized: Boolean,
    onMinimizeToggle: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState is MediaImportUiState.Idle) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (isMinimized) {
            MinimizedImportUi(
                uiState = uiState,
                onClick = onMinimizeToggle
            )
        } else {
            ExpandedImportUi(
                uiState = uiState,
                onMinimize = onMinimizeToggle,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun MinimizedImportUi(
    uiState: MediaImportUiState,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_download),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                when (uiState) {
                    is MediaImportUiState.InProgress -> {
                        Text(
                            text = "Importing ${uiState.completedFiles}/${uiState.totalFiles}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { if (uiState.totalFiles > 0) uiState.completedFiles.toFloat() / uiState.totalFiles else 0f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    is MediaImportUiState.Completed -> {
                        Text(
                            text = "Import completed",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    else -> {}
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_sort_asc),
                contentDescription = "Expand"
            )
        }
    }
}

@Composable
private fun ExpandedImportUi(
    uiState: MediaImportUiState,
    onMinimize: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (uiState) {
                        is MediaImportUiState.InProgress -> "Adding to Vault..."
                        is MediaImportUiState.Completed -> "Import Summary"
                        else -> ""
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                if (uiState is MediaImportUiState.InProgress) {
                    IconButton(onClick = onMinimize) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_sort_desc),
                            contentDescription = "Minimize"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (uiState) {
                is MediaImportUiState.InProgress -> {
                    val progress = if (uiState.totalFiles > 0) uiState.completedFiles.toFloat() / uiState.totalFiles else 0f

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = uiState.currentFileName,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${uiState.completedFiles} of ${uiState.totalFiles}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (uiState.failedFiles > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${uiState.failedFiles} failed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                is MediaImportUiState.Completed -> {
                    Text(text = "Successfully imported: ${uiState.successfulFiles}")
                    if (uiState.failedFiles > 0) {
                        Text(
                            text = "Failed to import: ${uiState.failedFiles}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Close")
                        }
                    }
                }
                else -> {}
            }
        }
    }
}
