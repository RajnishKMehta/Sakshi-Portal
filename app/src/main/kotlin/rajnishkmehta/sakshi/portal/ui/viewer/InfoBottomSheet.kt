/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.media3.common.util.UnstableApi::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package rajnishkmehta.sakshi.portal.ui.viewer

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.inspector.MetadataRetriever
import rajnishkmehta.sakshi.sdk.api.models.MediaDetailsResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.guava.await

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun InfoBottomSheet(
    mediaDetails: MediaDetailsResponse,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(true) }
    var metadataList by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var technicalList by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    LaunchedEffect(mediaDetails) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val baseInfo = mutableListOf(
                "File ID" to mediaDetails.fileId,
                "Type" to mediaDetails.mediaType,
                "Extension" to mediaDetails.fileExtension.uppercase(),
                "Created Time" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(mediaDetails.createdTime))
            )

            metadataList = baseInfo
            technicalList = emptyList()

            try {
                val uri = Uri.parse(mediaDetails.contentUri)

                when (mediaDetails.mediaType.uppercase()) {
                    "PHOTO" -> {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            val exif = ExifInterface(inputStream)
                            val exifData = mutableListOf<Pair<String, String>>()

                            exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)?.let { exifData.add("Width" to it) }
                            exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)?.let { exifData.add("Height" to it) }
                            exif.getAttribute(ExifInterface.TAG_MAKE)?.let { exifData.add("Make" to it) }
                            exif.getAttribute(ExifInterface.TAG_MODEL)?.let { exifData.add("Model" to it) }
                            exif.getAttribute(ExifInterface.TAG_DATETIME)?.let { exifData.add("DateTime" to it) }
                            exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let { exifData.add("Focal Length" to it) }
                            exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)?.let { exifData.add("ISO" to it) }

                            technicalList = exifData
                        }
                    }
                    "VIDEO" -> {
                        val videoInfo = mutableListOf<Pair<String, String>>()

                        try {
                            MetadataRetriever.Builder(context, MediaItem.fromUri(uri)).build().use { retriever ->
                                val trackGroups = retriever.retrieveTrackGroups().await()

                                for (i in 0 until trackGroups.length) {
                                    val trackGroup = trackGroups.get(i)
                                    val format = trackGroup.getFormat(0)

                                    if (format.width != androidx.media3.common.Format.NO_VALUE) {
                                        videoInfo.add("Resolution" to "${format.width}x${format.height}")
                                    }
                                    if (format.frameRate != androidx.media3.common.Format.NO_VALUE.toFloat()) {
                                        videoInfo.add("Frame Rate" to "${format.frameRate} fps")
                                    }
                                    if (format.sampleMimeType != null) {
                                        videoInfo.add("MIME Type" to format.sampleMimeType!!)
                                    }
                                    if (format.codecs != null) {
                                        videoInfo.add("Codec" to format.codecs!!)
                                    }
                                    if (format.bitrate != androidx.media3.common.Format.NO_VALUE) {
                                        videoInfo.add("Bitrate" to "${format.bitrate / 1000} kbps")
                                    }
                                    if (format.rotationDegrees != androidx.media3.common.Format.NO_VALUE && format.rotationDegrees != 0) {
                                        videoInfo.add("Rotation" to "${format.rotationDegrees}°")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                        }

                        technicalList = videoInfo.distinct()
                    }
                    "AUDIO" -> {
                        val audioInfo = mutableListOf<Pair<String, String>>()
                        val descriptiveInfo = baseInfo.toMutableList()

                        try {
                            MetadataRetriever.Builder(context, MediaItem.fromUri(uri)).build().use { retriever ->
                                val trackGroups = retriever.retrieveTrackGroups().await()

                                for (i in 0 until trackGroups.length) {
                                    val trackGroup = trackGroups.get(i)
                                    val format = trackGroup.getFormat(0)

                                    val metadata = format.metadata
                                    if (metadata != null) {
                                        val builder = androidx.media3.common.MediaMetadata.Builder()
                                        for (j in 0 until metadata.length()) {
                                            metadata.get(j).populateMediaMetadata(builder)
                                        }
                                        val mediaMetadata = builder.build()
                                        mediaMetadata.title?.let { descriptiveInfo.add("Title" to it.toString()) }
                                        mediaMetadata.artist?.let { descriptiveInfo.add("Artist" to it.toString()) }
                                        mediaMetadata.albumTitle?.let { descriptiveInfo.add("Album" to it.toString()) }
                                        mediaMetadata.genre?.let { descriptiveInfo.add("Genre" to it.toString()) }
                                        mediaMetadata.releaseYear?.let { descriptiveInfo.add("Year" to it.toString()) }
                                    }

                                    if (format.sampleMimeType != null) {
                                        audioInfo.add("MIME Type" to format.sampleMimeType!!)
                                    }
                                    if (format.sampleRate != androidx.media3.common.Format.NO_VALUE) {
                                        audioInfo.add("Sample Rate" to "${format.sampleRate} Hz")
                                    }
                                    if (format.channelCount != androidx.media3.common.Format.NO_VALUE) {
                                        audioInfo.add("Channels" to "${format.channelCount}")
                                    }
                                    if (format.bitrate != androidx.media3.common.Format.NO_VALUE) {
                                        audioInfo.add("Bitrate" to "${format.bitrate / 1000} kbps")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                        }

                        metadataList = descriptiveInfo
                        technicalList = audioInfo.distinct()
                    }
                    else -> {
                        metadataList = baseInfo
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            } finally {
                isLoading = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "File Information",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isLoading) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp).align(androidx.compose.ui.Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn {
                    item {
                        Text(
                            text = "Descriptive Metadata",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(metadataList) { (key, value) ->
                        InfoRow(key = key, value = value)
                    }

                    if (technicalList.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Technical Information",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        items(technicalList) { (key, value) ->
                            InfoRow(key = key, value = value)
                        }
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
private fun InfoRow(key: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()) {
        Text(text = key, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
