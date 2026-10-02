/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
        val VAULT_PACKAGE_KEY = stringPreferencesKey("vault_package")
    }

    val useDynamicColorFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            // By default, Material You is enabled
            preferences[DYNAMIC_COLOR_KEY] ?: true
        }

    val vaultPackageFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[VAULT_PACKAGE_KEY]
        }

    suspend fun setUseDynamicColor(useDynamicColor: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR_KEY] = useDynamicColor
        }
    }

    suspend fun setVaultPackage(packageName: String?) {
        context.dataStore.edit { preferences ->
            if (packageName == null) {
                preferences.remove(VAULT_PACKAGE_KEY)
            } else {
                preferences[VAULT_PACKAGE_KEY] = packageName
            }
        }
    }

    suspend fun setVaultPackageIfUnset(packageName: String) {
        context.dataStore.edit { preferences ->
            if (!preferences.contains(VAULT_PACKAGE_KEY)) {
                preferences[VAULT_PACKAGE_KEY] = packageName
            }
        }
    }
}
