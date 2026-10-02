/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.ui.vault

import androidx.compose.ui.res.stringResource
import rajnishkmehta.sakshi.portal.R

import android.widget.Toast
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import android.graphics.drawable.Drawable
import androidx.compose.ui.geometry.Size

import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewmodel.compose.viewModel
import rajnishkmehta.sakshi.portal.vault.AppInfo
import rajnishkmehta.sakshi.portal.vault.VaultSelectionViewModel
import rajnishkmehta.sakshi.portal.debug.DebugLogger as Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSelectionScreen(
    viewModel: VaultSelectionViewModel = viewModel(),
    onAppSelected: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        searchQuery = ""
        viewModel.filter("")
        // try { Log.i(tag = "VaultSelection", message = "VaultSelectionScreen opened") } catch (e: Exception) {}
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.cancelVerification()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vault_selection_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.cancelVerification()
                        searchQuery = ""
                        viewModel.filter("")
                        onBackClick()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back_content_description)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(context.resources.getString(R.string.vault_download_url)))
                        try {
                            if (intent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, context.resources.getString(R.string.vault_download_error), Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, context.resources.getString(R.string.vault_download_error), Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_download),
                            contentDescription = stringResource(R.string.vault_selection_download_content_desc)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.filter(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(stringResource(R.string.vault_selection_search_hint)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Box(modifier = Modifier.fillMaxSize()) {
                // try { Log.d(tag = "VaultSelection", message = "Current UI State: ${uiState.javaClass.simpleName}") } catch (e: Exception) {}
                when (val state = uiState) {
                    is VaultSelectionViewModel.UiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is VaultSelectionViewModel.UiState.Success -> {
                        // try { Log.i(tag = "VaultSelection", message = "State is Success, rendering AppList") } catch (e: Exception) {}
                        AppList(
                            apps = state.apps,
                            onAppClick = { app ->
                                if (!isVerifying) {
                                    isVerifying = true
                                    // try { Log.i(tag = "VaultSelection", message = "Pinging selected app: ${app.packageName}") } catch (e: Exception) {}
                                    viewModel.verifyVaultApp(app.packageName) { isSuccess, error ->
                                        isVerifying = false
                                        if (isSuccess) {
                                            // try { Log.i(tag = "VaultSelection", message = "Ping success for: ${app.packageName}") } catch (e: Exception) {}
                                            searchQuery = ""
                                            viewModel.filter("")
                                            onAppSelected(app.packageName)
                                        } else {
                                            // try { Log.e(tag = "VaultSelection", message = "Ping failed for ${app.packageName}: $error") } catch (e: Exception) {}
                                            Toast.makeText(context, context.resources.getString(R.string.vault_selection_connection_failed, error ?: context.resources.getString(R.string.unknown_error)), Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        )
                    }
                    is VaultSelectionViewModel.UiState.Filtering -> {
                        AppList(
                            apps = state.apps,
                            onAppClick = { app ->
                                if (!isVerifying) {
                                    isVerifying = true
                                    // try { Log.i(tag = "VaultSelection", message = "Pinging selected app: ${app.packageName}") } catch (e: Exception) {}
                                    viewModel.verifyVaultApp(app.packageName) { isSuccess, error ->
                                        isVerifying = false
                                        if (isSuccess) {
                                            // try { Log.i(tag = "VaultSelection", message = "Ping success for: ${app.packageName}") } catch (e: Exception) {}
                                            searchQuery = ""
                                            viewModel.filter("")
                                            onAppSelected(app.packageName)
                                        } else {
                                            // try { Log.e(tag = "VaultSelection", message = "Ping failed for ${app.packageName}: $error") } catch (e: Exception) {}
                                            Toast.makeText(context, context.resources.getString(R.string.vault_selection_connection_failed, error ?: context.resources.getString(R.string.unknown_error)), Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        )
                    }
                    is VaultSelectionViewModel.UiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.error_prefix, state.message),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                if (isVerifying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.Center)
                            .clickable(enabled = false) {}
                    ) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
}

@Composable
fun AppList(apps: List<AppInfo>, onAppClick: (AppInfo) -> Unit) {
    // try { Log.i(tag = "VaultSelection", message = "AppList called with ${apps.size} apps") } catch (e: Exception) {}
    if (apps.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.vault_selection_no_apps_found))
        }
    } else {
        // try { Log.i(tag = "VaultSelection", message = "Initializing LazyColumn") } catch (e: Exception) {}
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items = apps, key = { it.packageName }) { app ->
                // try { Log.d(tag = "VaultSelection", message = "LazyColumn item for: ${app.packageName}") } catch (e: Exception) {}
                AppItem(app = app, onClick = { onAppClick(app) })
            }
        }
    }
}

@Composable
fun AppItem(app: AppInfo, onClick: () -> Unit) {
    val context = LocalContext.current
    var iconDrawable by remember { mutableStateOf<Drawable?>(null) }
    var useFallback by remember { mutableStateOf(false) }

    LaunchedEffect(app.packageName) {
        // try { Log.d(tag = "VaultSelection", message = "LaunchedEffect started for: ${app.packageName}") } catch (e: Exception) {}
        withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val icon = pm.getApplicationIcon(app.packageName)
                // try { Log.d(tag = "VaultSelection", message = "Icon loaded for ${app.packageName}: type ${icon.javaClass.simpleName}") } catch (e: Exception) {}
                withContext(Dispatchers.Main) {
                    iconDrawable = icon
                }
            } catch (e: Exception) {
                // try { Log.e(tag = "VaultSelection", message = "Failed to load icon for ${app.packageName}: ${e.message}") } catch (logE: Exception) {}
                withContext(Dispatchers.Main) {
                    useFallback = true
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Fallback default app icon (rendered as raw Drawable because it's AdaptiveIconDrawable)
        val defaultDrawable = remember {
            ContextCompat.getDrawable(context, android.R.drawable.sym_def_app_icon)
        }

        val targetDrawable = iconDrawable ?: (if (useFallback || iconDrawable == null) defaultDrawable else null)

        // Custom Painter for Drawable
        val painter = remember(targetDrawable) {
            val d = targetDrawable
            if (d != null) {
                try {
                    object : Painter() {
                        override val intrinsicSize: Size
                            get() = Size(
                                d.intrinsicWidth.toFloat().takeIf { it > 0f } ?: 144f,
                                d.intrinsicHeight.toFloat().takeIf { it > 0f } ?: 144f
                            )

                        override fun DrawScope.onDraw() {
                            try {
                                val w = size.width.toInt().takeIf { it > 0 } ?: intrinsicSize.width.toInt()
                                val h = size.height.toInt().takeIf { it > 0 } ?: intrinsicSize.height.toInt()
                                drawIntoCanvas { canvas ->
                                    d.setBounds(0, 0, w, h)
                                    d.draw(canvas.nativeCanvas)
                                }
                            } catch (e: Exception) {
                                // try { Log.e(tag = "VaultSelection", message = "Error rendering icon for ${app.packageName}: ${e.message}") } catch (logE: Exception) {}
                            }
                        }
                    }
                } catch (e: Exception) {
                    // try { Log.e(tag = "VaultSelection", message = "Error creating painter for ${app.packageName}: ${e.message}") } catch (logE: Exception) {}
                    null
                }
            } else null
        }

        if (painter != null) {
            Image(
                painter = painter,
                contentDescription = stringResource(R.string.vault_selection_app_icon_content_desc),
                modifier = Modifier.size(48.dp)
            )
        } else {
            // Invisible placeholder to keep space
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = app.name, style = MaterialTheme.typography.bodyLarge)
            Text(text = app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
