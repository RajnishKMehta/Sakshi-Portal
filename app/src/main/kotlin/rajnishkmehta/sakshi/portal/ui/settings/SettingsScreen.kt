/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.ui.settings

import android.content.Intent
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import rajnishkmehta.sakshi.portal.R
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import rajnishkmehta.sakshi.portal.debug.DebugLogger as Log
import rajnishkmehta.sakshi.portal.BuildConfig
import rajnishkmehta.sakshi.portal.data.SettingsRepository
import rajnishkmehta.sakshi.portal.ui.AppLockManager

/**
 * Displays the application settings screen.
 * Allows configuration of appearance, vault selection, and security preferences.
 *
 * @param onVaultSelectionClick Callback invoked to navigate to the vault selection screen.
 * @param repository Repository for persisting and retrieving user settings.
 * @param onBackClick Callback invoked to navigate back.
 * @param appLockManager Manager for the application lock functionality (optional).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onVaultSelectionClick: () -> Unit,
    repository: SettingsRepository,
    onBackClick: () -> Unit,
    appLockManager: AppLockManager? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val useDynamicColor by repository.useDynamicColorFlow.collectAsState(initial = true)
    val vaultPackage by repository.vaultPackageFlow.collectAsState(initial = null)

    val appLockEnabled by repository.appLockEnabledFlow.collectAsState(initial = false)
    val appLockTimeoutMs by repository.appLockTimeoutMsFlow.collectAsState(initial = 60000L)

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back_content_description)
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
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {

            Text(
                "General",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (BuildConfig.DEBUG) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_debug_logs_title), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.settings_debug_logs_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(context, Class.forName("rajnishkmehta.sakshi.portal.debug.DebugLogsActivity"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.settings_debug_logs_button))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_dynamic_color_title), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.settings_dynamic_color_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = useDynamicColor,
                    onCheckedChange = { isChecked ->
                        try { Log.i(tag = "SettingsScreen", message = "Material You (Dynamic Colors) changed to \$isChecked") } catch (e: Exception) {}
                        coroutineScope.launch {
                            repository.setUseDynamicColor(isChecked)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onVaultSelectionClick)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_select_vault_title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        vaultPackage ?: stringResource(R.string.settings_no_vault_selected),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (vaultPackage == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                stringResource(R.string.settings_security_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_app_lock_title), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.settings_app_lock_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = appLockEnabled,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            val biometricManager = BiometricManager.from(context)
                            val canAuthenticate = biometricManager.canAuthenticate(
                                BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                            )
                            if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
                                Toast.makeText(context, context.resources.getString(R.string.app_lock_setup_error_no_credentials), Toast.LENGTH_LONG).show()
                                return@Switch
                            }
                        }
                        coroutineScope.launch { repository.setAppLockEnabled(isChecked) }
                    }
                )
            }

            if (appLockEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_timeout_title), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.settings_timeout_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    var expanded by remember { mutableStateOf(false) }

                    val timeoutOptions = listOf(
                        0L to stringResource(R.string.timeout_immediately),
                        30000L to stringResource(R.string.timeout_30s),
                        60000L to stringResource(R.string.timeout_1m),
                        300000L to stringResource(R.string.timeout_5m),
                        600000L to stringResource(R.string.timeout_10m)
                    )

                    val currentSelectionText = timeoutOptions.find { it.first == appLockTimeoutMs }?.second ?: stringResource(R.string.timeout_1m)

                    Box {
                        OutlinedButton(onClick = { expanded = true }) {
                            Text(currentSelectionText)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            timeoutOptions.forEach { (duration, text) ->
                                DropdownMenuItem(
                                    text = { Text(text) },
                                    onClick = {
                                        coroutineScope.launch { repository.setAppLockTimeoutMs(duration) }
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
