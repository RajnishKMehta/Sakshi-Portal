package rajnishkmehta.sakshi.portal.ui

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import rajnishkmehta.sakshi.portal.R
import rajnishkmehta.sakshi.portal.data.SettingsRepository

/**
 * Dialog displayed on first launch to allow the user to enable the App Lock feature.
 *
 * @param settingsRepository Repository used to save the user choice.
 * @param onDismiss Callback invoked when the dialog is dismissed or an option is selected.
 */
@Composable
fun AppLockSetupDialog(
    settingsRepository: SettingsRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = {
            // Dismissed by touching outside, we just mark it setup
            coroutineScope.launch {
                settingsRepository.setAppLockSetupCompleted(true)
                onDismiss()
            }
        },
        title = {
            Text(text = stringResource(R.string.app_lock_setup_title))
        },
        text = {
            Text(text = stringResource(R.string.app_lock_setup_message))
        },
        confirmButton = {
            Button(
                onClick = {
                    val biometricManager = BiometricManager.from(context)
                    val canAuthenticate = biometricManager.canAuthenticate(
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )

                    if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
                        Toast.makeText(context, context.resources.getString(R.string.app_lock_setup_error_no_credentials), Toast.LENGTH_LONG).show()
                    } else {
                        coroutineScope.launch {
                            settingsRepository.setAppLockEnabled(true)
                            settingsRepository.setAppLockSetupCompleted(true)
                            onDismiss()
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.app_lock_setup_enable))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        settingsRepository.setAppLockSetupCompleted(true)
                        onDismiss()
                    }
                }
            ) {
                Text(stringResource(R.string.app_lock_setup_decline), color = MaterialTheme.colorScheme.error)
            }
        }
    )
}
