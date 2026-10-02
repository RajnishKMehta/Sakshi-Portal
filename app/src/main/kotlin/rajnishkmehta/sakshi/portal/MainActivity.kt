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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import rajnishkmehta.sakshi.portal.data.SettingsRepository
import rajnishkmehta.sakshi.portal.ui.gallery.GalleryScreen
import rajnishkmehta.sakshi.portal.ui.settings.SettingsScreen
import rajnishkmehta.sakshi.portal.ui.vault.VaultSelectionScreen
import rajnishkmehta.sakshi.portal.ui.theme.PortalTheme
import rajnishkmehta.sakshi.sdk.api.SakshiClient
import rajnishkmehta.sakshi.sdk.api.SakshiClientConfig
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

        lifecycleScope.launch {
            var vaultPackage = settingsRepository.vaultPackageFlow.firstOrNull()

            if (vaultPackage == null) {
                // First time launch: try default vault
                vaultPackage = "rajnishkmehta.sakshi.vault"
                try { Log.i(tag = "MainActivity", message = "First launch, pinging default vault: $vaultPackage") } catch (e: Exception) {}

                val config = SakshiClientConfig(
                    vaultPackageName = vaultPackage,
                    connectionTimeoutMs = 5000L
                )
                val client = SakshiClient.create(applicationContext, config)
                try {
                    val result = client.pingVault()
                    if (result.isSuccess) {
                        try { Log.i(tag = "MainActivity", message = "Default vault ping success, setting as default") } catch (e: Exception) {}
                        settingsRepository.setVaultPackageIfUnset(vaultPackage)
                    } else {
                        val err = result.errorOrNull()
                        try { Log.e(tag = "MainActivity", message = "Default vault ping failed: ${err?.message}, leaving unset") } catch (e: Exception) {}
                    }
                } finally {
                    client.disconnect()
                }
            } else {
                try { Log.i(tag = "MainActivity", message = "Pinging saved vault: $vaultPackage") } catch (e: Exception) {}
                val config = SakshiClientConfig(
                    vaultPackageName = vaultPackage,
                    connectionTimeoutMs = 5000L
                )
                val client = SakshiClient.create(applicationContext, config)
                val result = client.pingVault()
                if (result.isSuccess) {
                    try { Log.i(tag = "MainActivity", message = "Ping success") } catch (e: Exception) {}
                } else {
                    val err = result.errorOrNull()
                    try { Log.e(tag = "MainActivity", message = "Ping failed: ${err?.message}") } catch (e: Exception) {}
                }
                client.disconnect()
            }
        }

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
                                onSettingsClick = { backStack.add(SettingsScreenRoute) }
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
