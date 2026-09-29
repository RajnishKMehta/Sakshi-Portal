/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.ui.gallery

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.ui.res.painterResource
import rajnishkmehta.sakshi.portal.R

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import rajnishkmehta.sakshi.portal.debug.DebugLogger as Log
import kotlinx.coroutines.delay
import rajnishkmehta.sakshi.portal.debug.DebugLogger

enum class MediaType {
    ALL, PHOTO, VIDEO, AUDIO, OTHER
}

data class DummyMediaItem(
    val id: String,
    val type: MediaType,
    val aspectRatio: Float
)

sealed interface GalleryUiState {
    data object Loading : GalleryUiState
    data class Success(val media: List<DummyMediaItem>) : GalleryUiState
    data class Error(val message: String) : GalleryUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(modifier: Modifier = Modifier, onSettingsClick: () -> Unit = {}) {
    var selectedType by remember { mutableStateOf(MediaType.ALL) }
    var uiState by remember { mutableStateOf<GalleryUiState>(GalleryUiState.Loading) }
    var isDynamicColorEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try { Log.i(tag = "GalleryScreen", message = "Loading gallery items") } catch (e: Exception) {}
        delay(1000) // Simulate network/db load
        val dummyData = List(30) { index ->
            DummyMediaItem(
                id = index.toString(),
                type = when (index % 4) { 0 -> MediaType.VIDEO; 1 -> MediaType.AUDIO; 2 -> MediaType.OTHER; else -> MediaType.PHOTO },
                aspectRatio = if (index % 2 == 0) 1.5f else 0.75f
            )
        }
        uiState = GalleryUiState.Success(dummyData)
    }


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
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Settings") },
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
    )
 { innerPadding ->
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
                    label = { Text("All") }
                )
                FilterChip(
                    selected = selectedType == MediaType.PHOTO,
                    onClick = { selectedType = MediaType.PHOTO },
                    label = { Text("Photos") }
                )
                FilterChip(
                    selected = selectedType == MediaType.VIDEO,
                    onClick = { selectedType = MediaType.VIDEO },
                    label = { Text("Videos") }
                )
                FilterChip(
                    selected = selectedType == MediaType.AUDIO,
                    onClick = { selectedType = MediaType.AUDIO },
                    label = { Text("Audio") }
                )
                FilterChip(
                    selected = selectedType == MediaType.OTHER,
                    onClick = { selectedType = MediaType.OTHER },
                    label = { Text("Other") }
                )
            }

            when (val state = uiState) {
                is GalleryUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is GalleryUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
                is GalleryUiState.Success -> {
                    val filteredData = if (selectedType == MediaType.ALL) state.media else state.media.filter { it.type == selectedType }

                    // Media Grid
                    if (filteredData.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No media found.")
                        }
                    } else {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(3),
                            contentPadding = PaddingValues(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalItemSpacing = 2.dp,
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

@Composable
fun MediaItem(item: DummyMediaItem) {
    val aspectRatio = if (item.type == MediaType.AUDIO || item.type == MediaType.OTHER) 1f else item.aspectRatio
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .background(Color.LightGray)
    ) {
        if (item.type == MediaType.VIDEO) {
            Icon(
                painter = painterResource(android.R.drawable.ic_media_play),
                contentDescription = "Video",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(32.dp)
            )
        } else if (item.type == MediaType.AUDIO) {
            Icon(
                painter = painterResource(R.drawable.ic_audio),
                contentDescription = "Audio",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(32.dp)
            )
        } else if (item.type == MediaType.OTHER) {
            Icon(
                painter = painterResource(android.R.drawable.ic_media_play),
                contentDescription = "Other File",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(32.dp)
            )
        }
    }
}
