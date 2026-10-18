/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import rajnishkmehta.sakshi.portal.data.SettingsRepository
import rajnishkmehta.sakshi.portal.ui.gallery.GalleryScreen
import rajnishkmehta.sakshi.portal.ui.settings.SettingsScreen
import rajnishkmehta.sakshi.portal.ui.vault.VaultSelectionScreen
import rajnishkmehta.sakshi.portal.ui.theme.PortalTheme
import rajnishkmehta.sakshi.portal.debug.DebugLogger as Log
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import kotlinx.serialization.Serializable
import rajnishkmehta.sakshi.portal.ui.viewer.MediaViewerScreen
import rajnishkmehta.sakshi.portal.ui.gallery.MediaType

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settingsRepository = SettingsRepository(applicationContext)

        setContent {
            val useDynamicColor by settingsRepository.useDynamicColorFlow.collectAsState(initial = true)
            val coroutineScope = rememberCoroutineScope()
            val backStack = rememberNavBackStack(GalleryScreenRoute)

            PortalTheme(useDynamicColor = useDynamicColor) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { if (backStack.size > 1) backStack.removeLastOrNull() else finish() },
                    entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
                    entryProvider = entryProvider {
                        entry<GalleryScreenRoute> {
                            GalleryScreen(
                                settingsRepository = settingsRepository,
                                onSettingsClick = { backStack.add(SettingsScreenRoute) },
                                onVaultSelectionClick = { backStack.add(VaultSelectionScreenRoute) },
                                onMediaClick = { fileId, mediaType -> backStack.add(MediaViewerRoute(fileId, mediaType)) }
                            )
                        }
                        entry<MediaViewerRoute> { route ->
                            MediaViewerScreen(
                                fileId = route.fileId,
                                mediaType = route.mediaType,
                                settingsRepository = settingsRepository,
                                onBackClick = { backStack.removeLastOrNull() }
                            )
                        }
                        entry<SettingsScreenRoute> {
                            SettingsScreen(
                                onVaultSelectionClick = { backStack.add(VaultSelectionScreenRoute) },
                                repository = settingsRepository,
                                onBackClick = { backStack.removeLastOrNull() }
                            )
                        }
                        entry<VaultSelectionScreenRoute> {
                            VaultSelectionScreen(
                                onAppSelected = { packageName ->
                                    coroutineScope.launch { settingsRepository.setVaultPackage(packageName) }
                                    backStack.removeLastOrNull()
                                },
                                onBackClick = { backStack.removeLastOrNull() }
                            )
                        }
                    }
                )
            }
        }
    }
}

@Serializable
data object GalleryScreenRoute : NavKey

@Serializable
data object SettingsScreenRoute : NavKey

@Serializable
data object VaultSelectionScreenRoute : NavKey


/**
 * Navigation key for the Media Viewer screen.
 *
 * @property fileId The unique identifier of the media file to view.
 * @property mediaType The type of media (e.g., photo, video) for rendering logic.
 */
@Serializable
data class MediaViewerRoute(val fileId: String, val mediaType: MediaType) : NavKey
