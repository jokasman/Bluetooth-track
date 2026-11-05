package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.service.AntiLostAlarmManager

class AlarmActionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      ACTION_DISMISS -> {
        AntiLostAlarmManager.dismiss(context)
      }
      ACTION_SNOOZE -> {
        AntiLostAlarmManager.snooze(context, 2)
      }
    }
  }

  companion object {
    const val ACTION_DISMISS = "com.example.receiver.ACTION_DISMISS_ALARM"
    const val ACTION_SNOOZE = "com.example.receiver.ACTION_SNOOZE_ALARM"
  }
}
