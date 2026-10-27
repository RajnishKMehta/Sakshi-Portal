/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.launch
import rajnishkmehta.sakshi.portal.data.SettingsRepository
import rajnishkmehta.sakshi.portal.ui.RemovedMediaViewModel
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
import androidx.fragment.app.FragmentActivity
import rajnishkmehta.sakshi.portal.ui.AppLockManager
import rajnishkmehta.sakshi.portal.ui.AppLockScreen
import rajnishkmehta.sakshi.portal.ui.AppLockSetupDialog

/**
 * The main entry point Activity for the Portal application.
 * Hosts the Jetpack Compose navigation graph and manages the App Lock lifecycle.
 */
class MainActivity : FragmentActivity() {
    private lateinit var appLockManager: AppLockManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as PortalApp
        val settingsRepository = app.settingsRepository
        appLockManager = app.appLockManager

        val removedMediaStore = ViewModelProvider(this)[RemovedMediaViewModel::class.java].removedMediaStore

        setContent {
            val useDynamicColor by settingsRepository.useDynamicColorFlow.collectAsState(initial = true)
            val isLocked by appLockManager.isLocked.collectAsState()
            val isInitialized by appLockManager.isInitialized.collectAsState()
            val setupCompleted by settingsRepository.appLockSetupCompletedFlow.collectAsState(initial = true)

            var showSetupDialog by remember(setupCompleted) { mutableStateOf(!setupCompleted) }

            val coroutineScope = rememberCoroutineScope()
            val backStack = rememberNavBackStack(GalleryScreenRoute)

            PortalTheme(useDynamicColor = useDynamicColor) {
                if (isInitialized) {
                    if (isLocked) {
                        AppLockScreen(appLockManager = appLockManager,

                            settingsRepository = settingsRepository
                        )
                    } else {
                        if (showSetupDialog) {
                            AppLockSetupDialog(
                                settingsRepository = settingsRepository,
                                onDismiss = { showSetupDialog = false }
                            )
                        }

                        NavDisplay(
                            backStack = backStack,
                            onBack = { if (backStack.size > 1) backStack.removeLastOrNull() else finish() },
                            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
                            entryProvider = entryProvider {
                                entry<GalleryScreenRoute> {
                                    GalleryScreen(
                                        settingsRepository = settingsRepository,
                                        removedMediaStore = removedMediaStore,
                                        onSettingsClick = { backStack.add(SettingsScreenRoute) },
                                        onVaultSelectionClick = { backStack.add(VaultSelectionScreenRoute) },
                                        onMediaClick = { fileId, mediaType, timestamp -> backStack.add(MediaViewerRoute(fileId, mediaType, false, timestamp)) },
                                        onScheduledDeletionsClick = { backStack.add(ScheduledDeletionsScreenRoute) }
                                    )
                                }
                                entry<ScheduledDeletionsScreenRoute> {
                                    GalleryScreen(
                                        settingsRepository = settingsRepository,
                                        removedMediaStore = removedMediaStore,
                                        onSettingsClick = { backStack.add(SettingsScreenRoute) },
                                        onVaultSelectionClick = { backStack.add(VaultSelectionScreenRoute) },
                                        onMediaClick = { fileId, mediaType, timestamp -> backStack.add(MediaViewerRoute(fileId, mediaType, true, timestamp)) },
                                        isDeletedMode = true,
                                        onBackClick = { backStack.removeLastOrNull() }
                                    )
                                }
                                entry<MediaViewerRoute> { route ->
                                    MediaViewerScreen(
                                        fileId = route.fileId,
                                        mediaType = route.mediaType,
                                        isDeletedMode = route.isDeletedMode,
                                        scheduledTime = route.scheduledTime,
                                        settingsRepository = settingsRepository,
                                        removedMediaStore = removedMediaStore,
                                        onBackClick = { backStack.removeLastOrNull() }
                                    )
                                }
                                entry<SettingsScreenRoute> {
                                    SettingsScreen(
                                        onVaultSelectionClick = { backStack.add(VaultSelectionScreenRoute) },
                                        repository = settingsRepository,
                                        onBackClick = { backStack.removeLastOrNull() },

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
    }
}

/** Navigation key for the Gallery screen. */
@Serializable
data object GalleryScreenRoute : NavKey

/** Navigation key for the Settings screen. */
@Serializable
data object SettingsScreenRoute : NavKey

/** Navigation key for the Vault Selection screen. */
@Serializable
data object VaultSelectionScreenRoute : NavKey

/**
 * Navigation key for the Scheduled Deletions screen.
 */
@Serializable
data object ScheduledDeletionsScreenRoute : NavKey


/**
 * Navigation key for the Media Viewer screen.
 *
 * @property fileId The unique identifier of the media file to view.
 * @property mediaType The type of media (e.g., photo, video) for rendering logic.
 * @property isDeletedMode Whether the viewer is opened from the scheduled deletions screen.
 */
@Serializable
data class MediaViewerRoute(val fileId: String, val mediaType: MediaType, val isDeletedMode: Boolean = false, val scheduledTime: Long = -1L) : NavKey
