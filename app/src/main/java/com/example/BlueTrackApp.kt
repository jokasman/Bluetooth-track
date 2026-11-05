package com.example

import android.app.Application
import com.example.notification.NotificationHelper
import com.example.service.BluetoothMonitorService
import com.example.util.BluetoothLogger
import com.example.util.PreferencesManager

class BlueTrackApp : Application() {
  override fun onCreate() {
    super.onCreate()
    // Initialize crash & error logger first to capture any startup issues
    BluetoothLogger.init(this)

    // Initialize notification channels early
    NotificationHelper.createNotificationChannels(this)

    // Check if background monitor should be started (only if permissions are already granted)
    val prefs = PreferencesManager.getInstance(this)
    if (prefs.serviceEnabled.value && BluetoothMonitorService.hasForegroundServicePermission(this)) {
      BluetoothMonitorService.startService(this)
    }
  }
}
