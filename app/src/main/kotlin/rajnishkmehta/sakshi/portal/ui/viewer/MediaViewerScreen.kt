/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package rajnishkmehta.sakshi.portal.ui.viewer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import rajnishkmehta.sakshi.portal.R
import rajnishkmehta.sakshi.portal.data.SettingsRepository
import rajnishkmehta.sakshi.portal.ui.gallery.MediaType
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources

@Composable
fun MediaViewerScreen(
    fileId: String,
    mediaType: MediaType,
    settingsRepository: SettingsRepository,
    onBackClick: () -> Unit
) {
    val viewModel: MediaViewerViewModel = viewModel(
        factory = MediaViewerViewModelFactory(
            application = LocalContext.current.applicationContext as Application,
            settingsRepository = settingsRepository
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val context = LocalContext.current
    val resources = LocalResources.current

    var showInfoSheet by remember { mutableStateOf(false) }

    LaunchedEffect(fileId) {
        viewModel.loadMedia(fileId)
    }

    LaunchedEffect(exportState) {
        val message = when (val state = exportState) {
            is MediaExportUiState.Completed -> resources.getString(R.string.viewer_save_success, state.location)
            is MediaExportUiState.Failed -> resources.getString(R.string.viewer_save_failed, state.message)
            else -> null
        }
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.onExportResultShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                    }
                },
                actions = {
                    val isExporting = exportState is MediaExportUiState.InProgress
                    IconButton(
                        onClick = { viewModel.exportMedia() },
                        enabled = uiState is MediaViewerUiState.Success && !isExporting
                    ) {
                        if (isExporting) {
                            LoadingIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(
                                painterResource(R.drawable.ic_download),
                                contentDescription = stringResource(R.string.viewer_save_content_description)
                            )
                        }
                    }
                    IconButton(onClick = { showInfoSheet = true }) {
                        Icon(painterResource(R.drawable.ic_info), contentDescription = "Info")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is MediaViewerUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingIndicator()
                    }
                }
                is MediaViewerUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(
                            onClick = { viewModel.loadMedia(fileId) },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text(stringResource(R.string.retry_action))
                        }
                    }
                }
                is MediaViewerUiState.Success -> {
                    val mediaDetails = state.mediaDetails
                    when (mediaDetails.mediaType.uppercase()) {
                        "PHOTO" -> PhotoViewer(uri = mediaDetails.contentUri)
                        "VIDEO" -> VideoViewer(uri = mediaDetails.contentUri)
                        "AUDIO" -> AudioViewer(uri = mediaDetails.contentUri)
                        else -> OtherViewer(mediaDetails = mediaDetails)
                    }

                    if (showInfoSheet) {
                        InfoBottomSheet(
                            mediaDetails = mediaDetails,
                            onDismissRequest = { showInfoSheet = false }
                        )
                    }
                }
            }
        }
    }
}
