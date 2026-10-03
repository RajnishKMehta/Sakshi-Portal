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
import kotlinx.serialization.Serializable

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
                    entryProvider = entryProvider {
                        entry<GalleryScreenRoute> {
                            GalleryScreen(
                                settingsRepository = settingsRepository,
                                onSettingsClick = { backStack.add(SettingsScreenRoute) },
                                onVaultSelectionClick = { backStack.add(VaultSelectionScreenRoute) }
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
