package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.service.BluetoothMonitorService
import com.example.util.PreferencesManager

class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
      val prefs = PreferencesManager.getInstance(context)
      if (prefs.autoStartOnBoot.value &&
        prefs.serviceEnabled.value &&
        BluetoothMonitorService.hasForegroundServicePermission(context)
      ) {
        BluetoothMonitorService.startService(context)
      }
    }
  }
}
