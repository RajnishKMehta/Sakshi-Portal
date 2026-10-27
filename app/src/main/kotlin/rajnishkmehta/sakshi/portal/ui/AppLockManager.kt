package rajnishkmehta.sakshi.portal.ui

import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rajnishkmehta.sakshi.portal.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import rajnishkmehta.sakshi.portal.debug.DebugLogger as Log

/**
 * Manages the app lock state based on lifecycle events and user settings.
 *
 * @param settingsRepository Repository to check if app lock is enabled and the timeout.
 * @param coroutineScope Scope used for launching state updates.
 */
class AppLockManager(
    private val settingsRepository: SettingsRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : DefaultLifecycleObserver {

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private var backgroundTime: Long = 0L

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /**
     * Checks if the app should be locked on a fresh start.
     */
    fun checkColdStart() {
        coroutineScope.launch {
            val enabled = settingsRepository.appLockEnabledFlow.first()
            _isLocked.value = enabled
            if (enabled) {
                Log.i("AppLockManager", "App started, locking immediately.")
            }
            _isInitialized.value = true
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (backgroundTime > 0) {
            val timeAway = SystemClock.elapsedRealtime() - backgroundTime

            coroutineScope.launch {
                val enabled = settingsRepository.appLockEnabledFlow.first()
                val timeout = settingsRepository.appLockTimeoutMsFlow.first()

                if (enabled && timeAway >= timeout) {
                    _isLocked.value = true
                    Log.i("AppLockManager", "App returned from background after ${timeAway}ms, locking.")
                }
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        backgroundTime = SystemClock.elapsedRealtime()
    }

    /**
     * Unlocks the application and resets the background time tracker.
     */
    fun unlock() {
        _isLocked.value = false
        backgroundTime = 0L // Reset background time on unlock
        Log.i("AppLockManager", "App unlocked.")
    }

    /**
     * Manually forces the application into a locked state if the lock feature is enabled.
     */
    fun lock() {
        coroutineScope.launch {
            val enabled = settingsRepository.appLockEnabledFlow.first()
            if (enabled) {
                _isLocked.value = true
                Log.i("AppLockManager", "App locked manually.")
            }
        }
    }
}
