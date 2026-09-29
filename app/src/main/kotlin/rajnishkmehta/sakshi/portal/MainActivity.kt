/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import rajnishkmehta.sakshi.portal.data.SettingsRepository
import rajnishkmehta.sakshi.portal.ui.gallery.GalleryScreen
import rajnishkmehta.sakshi.portal.ui.settings.SettingsScreen
import rajnishkmehta.sakshi.portal.ui.theme.PortalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settingsRepository = SettingsRepository(applicationContext)

        setContent {
            val useDynamicColor by settingsRepository.useDynamicColorFlow.collectAsState(initial = true)
            var currentScreen by remember { mutableStateOf(Screen.Gallery) }

            BackHandler(enabled = currentScreen != Screen.Gallery) {
                currentScreen = Screen.Gallery
            }

            PortalTheme(useDynamicColor = useDynamicColor) {
                when (currentScreen) {
                    Screen.Gallery -> {
                        GalleryScreen(
                            onSettingsClick = { currentScreen = Screen.Settings }
                        )
                    }
                    Screen.Settings -> {
                        SettingsScreen(
                            repository = settingsRepository,
                            onBackClick = { currentScreen = Screen.Gallery }
                        )
                    }
                }
            }
        }
    }
}

enum class Screen {
    Gallery, Settings
}
