package com.example.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences("bluetrack_prefs", Context.MODE_PRIVATE)

  private val _serviceEnabled = MutableStateFlow(prefs.getBoolean(KEY_SERVICE_ENABLED, true))
  val serviceEnabled: StateFlow<Boolean> = _serviceEnabled.asStateFlow()

  private val _notifyOnConnect = MutableStateFlow(prefs.getBoolean(KEY_NOTIFY_CONNECT, true))
  val notifyOnConnect: StateFlow<Boolean> = _notifyOnConnect.asStateFlow()

  private val _notifyOnDisconnect = MutableStateFlow(prefs.getBoolean(KEY_NOTIFY_DISCONNECT, true))
  val notifyOnDisconnect: StateFlow<Boolean> = _notifyOnDisconnect.asStateFlow()

  private val _vibrateOnAlert = MutableStateFlow(prefs.getBoolean(KEY_VIBRATE_ALERT, true))
  val vibrateOnAlert: StateFlow<Boolean> = _vibrateOnAlert.asStateFlow()

  private val _autoStartOnBoot = MutableStateFlow(prefs.getBoolean(KEY_BOOT_START, true))
  val autoStartOnBoot: StateFlow<Boolean> = _autoStartOnBoot.asStateFlow()

  // Global default alert policy: true = Alert by default (opt-out), false = Mute by default (opt-in)
  private val _globalAlertByDefault = MutableStateFlow(prefs.getBoolean(KEY_GLOBAL_ALERT_BY_DEFAULT, true))
  val globalAlertByDefault: StateFlow<Boolean> = _globalAlertByDefault.asStateFlow()

  fun setServiceEnabled(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
    _serviceEnabled.value = enabled
  }

  fun setNotifyOnConnect(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_NOTIFY_CONNECT, enabled).apply()
    _notifyOnConnect.value = enabled
  }

  fun setNotifyOnDisconnect(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_NOTIFY_DISCONNECT, enabled).apply()
    _notifyOnDisconnect.value = enabled
  }

  fun setVibrateOnAlert(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_VIBRATE_ALERT, enabled).apply()
    _vibrateOnAlert.value = enabled
  }

  fun setAutoStartOnBoot(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_BOOT_START, enabled).apply()
    _autoStartOnBoot.value = enabled
  }

  fun setGlobalAlertByDefault(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_GLOBAL_ALERT_BY_DEFAULT, enabled).apply()
    _globalAlertByDefault.value = enabled
  }

  companion object {
    private const val KEY_SERVICE_ENABLED = "key_service_enabled"
    private const val KEY_NOTIFY_CONNECT = "key_notify_connect"
    private const val KEY_NOTIFY_DISCONNECT = "key_notify_disconnect"
    private const val KEY_VIBRATE_ALERT = "key_vibrate_alert"
    private const val KEY_BOOT_START = "key_boot_start"
    private const val KEY_GLOBAL_ALERT_BY_DEFAULT = "key_global_alert_by_default"

    @Volatile
    private var INSTANCE: PreferencesManager? = null

    fun getInstance(context: Context): PreferencesManager {
      return INSTANCE ?: synchronized(this) {
        val instance = PreferencesManager(context)
        INSTANCE = instance
        instance
      }
    }
  }
}
