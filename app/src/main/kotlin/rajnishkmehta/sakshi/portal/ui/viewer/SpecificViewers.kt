/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
@file:OptIn(UnstableApi::class)

package rajnishkmehta.sakshi.portal.ui.viewer

import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.material3.Player as Media3Player
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import rajnishkmehta.sakshi.portal.R
import rajnishkmehta.sakshi.sdk.api.models.MediaDetailsResponse
import kotlin.random.Random
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.input.pointer.pointerInput
import androidx.annotation.OptIn
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.ui.compose.material3.PlayerDefaults



/**
 * Viewer component for photo media files.
 * Supports pan and zoom gestures.
 *
 * @param uri The content URI of the photo to display.
 */
@Composable
fun PhotoViewer(uri: String) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    val maxX = (size.width * (scale - 1)) / 2
                    val maxY = (size.height * (scale - 1)) / 2
                    offset = Offset(
                        x = (offset.x + pan.x * scale).coerceIn(-maxX, maxX),
                        y = (offset.y + pan.y * scale).coerceIn(-maxY, maxY)
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(Uri.parse(uri))
                .crossfade(true)
                .build(),
            contentDescription = "Photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}



/**
 * Viewer component for video media files using ExoPlayer.
 *
 * @param uri The content URI of the video to play.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoViewer(uri: String) {
    val context = LocalContext.current
    val exoPlayer = remember(uri) {
        val extractorsFactory = DefaultExtractorsFactory().apply {
            setFragmentedMp4ExtractorFlags(
                FragmentedMp4Extractor.FLAG_READ_MFRA_FOR_SEEK_MAP or
                FragmentedMp4Extractor.FLAG_WORKAROUND_IGNORE_TFDT_BOX or
                FragmentedMp4Extractor.FLAG_ENABLE_EMSG_TRACK
            )
        }
        val mediaSourceFactory = DefaultMediaSourceFactory(context, extractorsFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setSeekBackIncrementMs(5000)
            .setSeekForwardIncrementMs(10000)
            .build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
            prepare()
            playWhenReady = true
        }
    }

    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var showControls by remember { mutableStateOf(true) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(isPlaying, showControls) {
        if (isPlaying && showControls) {
            delay(3000)
            showControls = false
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Media3Player(
            player = exoPlayer,
            modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectTapGestures {
                    showControls = !showControls
                }
            },
            showControls = showControls,
            centerControls = { player, visible ->
                PlayerDefaults.CenterControls(
                    player = player,
                    visible = visible,
                    modifier = Modifier.fillMaxWidth(),
                    backSecondary = {},
                    forwardSecondary = {}
                )
            }
        )
    }
}


/**
 * Viewer component for audio media files using ExoPlayer.
 * Displays embedded album artwork or a visualizer if unavailable.
 *
 * @param uri The content URI of the audio file to play.
 */
@OptIn(UnstableApi::class)
@Composable
fun AudioViewer(uri: String) {
    val context = LocalContext.current
    val exoPlayer = remember(uri) {
        val extractorsFactory = DefaultExtractorsFactory().apply {
            setFragmentedMp4ExtractorFlags(
                FragmentedMp4Extractor.FLAG_READ_MFRA_FOR_SEEK_MAP or
                FragmentedMp4Extractor.FLAG_WORKAROUND_IGNORE_TFDT_BOX or
                FragmentedMp4Extractor.FLAG_ENABLE_EMSG_TRACK
            )
        }
        val mediaSourceFactory = DefaultMediaSourceFactory(context, extractorsFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setSeekBackIncrementMs(5000)
            .setSeekForwardIncrementMs(10000)
            .build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
            prepare()
            playWhenReady = true
        }
    }

    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var hasArtwork by remember { mutableStateOf(false) }
    var artworkData by remember { mutableStateOf<ByteArray?>(null) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                if (mediaMetadata.artworkData != null) {
                    hasArtwork = true
                    artworkData = mediaMetadata.artworkData
                } else {
                    hasArtwork = false
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (hasArtwork) {
                if (!isPlaying) {
                    // Show artwork while paused
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(artworkData)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Album Art",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(0.8f)
                    )
                } else {
                    // During playback, show decorative animated visualization
                    AudioVisualization(isPlaying = true, modifier = Modifier.fillMaxSize(0.8f))
                }
            } else {
                // If album artwork does NOT exist: Keep the decorative bars/visualization visible even while paused.
                AudioVisualization(isPlaying = isPlaying, modifier = Modifier.fillMaxSize(0.8f))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) // Adjust height as needed for player controls
        ) {
             Media3Player(
                player = exoPlayer,
                modifier = Modifier.fillMaxSize(),
                centerControls = { player, visible ->
                    PlayerDefaults.CenterControls(
                        player = player,
                        visible = visible,
                        modifier = Modifier.fillMaxWidth(),
                        backSecondary = {},
                        forwardSecondary = {}
                    )
                }
            )
        }
    }
}


/**
 * Decorative animated visualization for audio playback.
 *
 * @param isPlaying True if audio is actively playing to animate the bars.
 * @param modifier Modifier applied to the visualization canvas.
 */
@Composable
fun AudioVisualization(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "audioBars")
    val barColor = MaterialTheme.colorScheme.primary

    // Create animated progress values for several bars
    val barCount = 10
    val barProgresses = (0 until barCount).map { i ->
        val duration = remember { Random.nextInt(500, 1200) }
        val target = remember { Random.nextFloat() * 0.8f + 0.2f }

        // Always call animateFloat to keep the slot table structure intact,
        // but change the target based on isPlaying so it either animates or stays fixed.
        infiniteTransition.animateFloat(
            initialValue = if (isPlaying) 0.2f else 0.4f,
            targetValue = if (isPlaying) target else 0.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = duration, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar$i"
        )
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barWidth = width / (barCount * 2)
        val maxBarHeight = height * 0.8f

        for (i in 0 until barCount) {
            val progress = barProgresses[i].value
            val currentHeight = maxBarHeight * progress
            val x = (i * 2 + 0.5f) * barWidth
            val yTop = height / 2 - currentHeight / 2
            val yBottom = height / 2 + currentHeight / 2

            drawLine(
                color = barColor,
                start = Offset(x, yTop),
                end = Offset(x, yBottom),
                strokeWidth = barWidth * 0.8f,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Fallback viewer component for unsupported or other media types.
 * Displays file information and metadata.
 *
 * @param mediaDetails The detailed information about the media file.
 */
@Composable
fun OtherViewer(mediaDetails: MediaDetailsResponse) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_placeholder_other),
            contentDescription = "File Icon",
            modifier = Modifier.size(128.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "File ID: ${mediaDetails.fileId}",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Type: ${mediaDetails.mediaType}",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Extension: ${mediaDetails.fileExtension.uppercase()}",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
