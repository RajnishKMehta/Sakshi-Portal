/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState



import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import rajnishkmehta.sakshi.portal.R
import rajnishkmehta.sakshi.portal.data.SettingsRepository
import rajnishkmehta.sakshi.portal.debug.DebugLogger as Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    modifier: Modifier = Modifier,
    settingsRepository: SettingsRepository,
    onSettingsClick: () -> Unit = {},
    onVaultSelectionClick: () -> Unit = {}
) {
    val application = LocalContext.current.applicationContext as android.app.Application
    val viewModel: GalleryViewModel = viewModel(
        factory = GalleryViewModelFactory(application, settingsRepository)
    )

    var selectedType by remember { mutableStateOf(MediaType.ALL) }
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()

    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more_vert),
                                contentDescription = stringResource(R.string.settings_content_description),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_title)) },
                                onClick = {
                                    showMenu = false
                                    onSettingsClick()
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedType == MediaType.ALL,
                    onClick = { selectedType = MediaType.ALL },
                    label = { Text(stringResource(R.string.filter_all)) }
                )
                FilterChip(
                    selected = selectedType == MediaType.PHOTO,
                    onClick = { selectedType = MediaType.PHOTO },
                    label = { Text(stringResource(R.string.filter_photos)) }
                )
                FilterChip(
                    selected = selectedType == MediaType.VIDEO,
                    onClick = { selectedType = MediaType.VIDEO },
                    label = { Text(stringResource(R.string.filter_videos)) }
                )
                FilterChip(
                    selected = selectedType == MediaType.AUDIO,
                    onClick = { selectedType = MediaType.AUDIO },
                    label = { Text(stringResource(R.string.filter_audio)) }
                )
                FilterChip(
                    selected = selectedType == MediaType.OTHER,
                    onClick = { selectedType = MediaType.OTHER },
                    label = { Text(stringResource(R.string.filter_other)) }
                )
            }

            when (val state = uiState) {
                is GalleryUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator() /* Technical reason: LoadingIndicator is internal/unresolved in this BOM version */
                    }
                }
                                is GalleryUiState.VaultUnavailable -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.vault_not_found_message),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(
                            onClick = { viewModel.loadMedia() },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text(stringResource(R.string.retry_action))
                        }
                        Row(
                            modifier = Modifier.padding(top = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            OutlinedButton(onClick = onVaultSelectionClick) {
                                Text(stringResource(R.string.select_vault_action))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(application.resources.getString(R.string.vault_download_url)))
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    application.startActivity(intent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(application, application.resources.getString(R.string.vault_download_error), android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Text(stringResource(R.string.download_vault_action))
                            }
                        }
                    }
                }
                is GalleryUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.error_prefix, state.message),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(
                            onClick = { viewModel.loadMedia() },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text(stringResource(R.string.retry_action))
                        }
                    }
                }
                is GalleryUiState.Success -> {
                    val filteredData = if (selectedType == MediaType.ALL) state.media else state.media.filter { it.type == selectedType }

                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refreshMedia() },
                        state = pullToRefreshState,
                        modifier = Modifier.fillMaxSize(),
                        indicator = {
                            PullToRefreshDefaults.Indicator(
                                state = pullToRefreshState,
                                isRefreshing = isRefreshing,
                                modifier = Modifier.align(Alignment.TopCenter)
                            )
                        }
                    ) {
                        if (filteredData.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = stringResource(R.string.gallery_nothing_in_vault),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 120.dp),
                                contentPadding = PaddingValues(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredData, key = { it.id }) { item ->
                                    MediaItem(item)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MediaItem(item: ParsedMediaItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Ensure strictly 1:1 squares
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable {
                // TODO: Handle click to open media viewer
                Log.d("MediaItem", "Clicked media item: ${item.id}")
            }
    ) {
        val placeholderRes = when (item.type) {
            MediaType.PHOTO -> R.drawable.ic_placeholder_photo
            MediaType.VIDEO -> R.drawable.ic_placeholder_video
            MediaType.AUDIO -> R.drawable.ic_placeholder_audio
            else -> R.drawable.ic_placeholder_other
        }

        // Thumbnail Image
        coil3.compose.SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(if (item.type == MediaType.AUDIO || item.type == MediaType.OTHER) null else item.thumbnailUri) // Force fallback/error for audio/other since they have no thumbnail
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = if (item.type == MediaType.AUDIO || item.type == MediaType.OTHER) ContentScale.Inside else ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = placeholderRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            },
            error = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = placeholderRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        )

        // Overlay Icon (only for video)
        if (item.type == MediaType.VIDEO) {
            Icon(
                painter = painterResource(android.R.drawable.ic_media_play),
                contentDescription = stringResource(R.string.media_type_video),
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(32.dp)
            )
        }
    }
}
