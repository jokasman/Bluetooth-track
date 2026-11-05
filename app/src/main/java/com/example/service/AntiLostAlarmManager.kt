package com.example.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.notification.NotificationHelper
import com.example.receiver.AlarmActionReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActiveAlarmState(
  val deviceName: String,
  val deviceAddress: String,
  val triggerType: String, // "DISCONNECTED" or "CONNECTED"
  val startedAt: Long,
  val isSnoozed: Boolean = false,
  val snoozeUntil: Long = 0L
)

object AntiLostAlarmManager {
  private const val ALARM_NOTIFICATION_ID = 9999

  private val _activeAlarm = MutableStateFlow<ActiveAlarmState?>(null)
  val activeAlarm: StateFlow<ActiveAlarmState?> = _activeAlarm.asStateFlow()

  private var mediaPlayer: MediaPlayer? = null
  private var vibrator: Vibrator? = null
  private var snoozeJob: Job? = null
  private val scope = CoroutineScope(Dispatchers.Main)

  fun startAlarm(
    context: Context,
    deviceName: String,
    deviceAddress: String,
    triggerType: String
  ) {
    // Cancel any existing snooze timer
    snoozeJob?.cancel()

    val now = System.currentTimeMillis()
    _activeAlarm.value = ActiveAlarmState(
      deviceName = deviceName,
      deviceAddress = deviceAddress,
      triggerType = triggerType,
      startedAt = now,
      isSnoozed = false
    )

    playAlarmAudio(context)
    startVibration(context)
    postAlarmNotification(context, deviceName, deviceAddress, triggerType)
  }

  fun dismiss(context: Context) {
    snoozeJob?.cancel()
    stopAlarmAudio()
    stopVibration()
    _activeAlarm.value = null

    val notificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancel(ALARM_NOTIFICATION_ID)
  }

  fun snooze(context: Context, durationMinutes: Int = 2) {
    val current = _activeAlarm.value ?: return

    stopAlarmAudio()
    stopVibration()

    val snoozeUntil = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
    _activeAlarm.value = current.copy(isSnoozed = true, snoozeUntil = snoozeUntil)

    postSnoozedNotification(context, current.deviceName, current.deviceAddress, durationMinutes)

    snoozeJob?.cancel()
    snoozeJob = scope.launch {
      delay(durationMinutes * 60 * 1000L)
      val state = _activeAlarm.value
      if (state != null && state.isSnoozed && state.deviceAddress == current.deviceAddress) {
        // Re-trigger alarm after snooze expires
        startAlarm(context, current.deviceName, current.deviceAddress, current.triggerType)
      }
    }
  }

  private fun playAlarmAudio(context: Context) {
    stopAlarmAudio()
    try {
      val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

      val player = MediaPlayer().apply {
        setDataSource(context, alarmUri)
        setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        isLooping = true
        prepare()
        start()
      }
      mediaPlayer = player
    } catch (e: Exception) {
      // Audio playback fallback
    }
  }

  private fun stopAlarmAudio() {
    mediaPlayer?.let {
      try {
        if (it.isPlaying) it.stop()
        it.release()
      } catch (_: Exception) {}
      mediaPlayer = null
    }
  }

  private fun startVibration(context: Context) {
    try {
      val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator
      } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
      }
      vibrator = v

      val pattern = longArrayOf(0, 800, 400, 800, 400)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        v.vibrate(VibrationEffect.createWaveform(pattern, 0)) // 0 = repeat indefinitely
      } else {
        @Suppress("DEPRECATION")
        v.vibrate(pattern, 0)
      }
    } catch (_: Exception) {}
  }

  private fun stopVibration() {
    vibrator?.let {
      try {
        it.cancel()
      } catch (_: Exception) {}
      vibrator = null
    }
  }

  private fun postAlarmNotification(
    context: Context,
    deviceName: String,
    deviceAddress: String,
    triggerType: String
  ) {
    NotificationHelper.createNotificationChannels(context)

    // Action 1: Dismiss Alarm
    val dismissIntent = Intent(context, AlarmActionReceiver::class.java).apply {
      action = AlarmActionReceiver.ACTION_DISMISS
    }
    val dismissPendingIntent = PendingIntent.getBroadcast(
      context,
      101,
      dismissIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Action 2: Snooze 2 Minutes
    val snoozeIntent = Intent(context, AlarmActionReceiver::class.java).apply {
      action = AlarmActionReceiver.ACTION_SNOOZE
    }
    val snoozePendingIntent = PendingIntent.getBroadcast(
      context,
      102,
      snoozeIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Content Intent: Open App
    val openIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("EXTRA_ALARM_ACTIVE", true)
      putExtra("EXTRA_DEVICE_ADDRESS", deviceAddress)
    }
    val openPendingIntent = PendingIntent.getActivity(
      context,
      100,
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val actionText = if (triggerType == "DISCONNECTED") "Left Behind!" else "Connected!"
    val title = "🚨 ANTI-LOST ALERT: $deviceName"
    val content = "$actionText $deviceName is out of range. Tap Dismiss to stop alarm."

    val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ANTI_LOST_ALARM_ID)
      .setSmallIcon(android.R.drawable.stat_notify_error)
      .setContentTitle(title)
      .setContentText(content)
      .setStyle(NotificationCompat.BigTextStyle().bigText("$content\nMAC: $deviceAddress"))
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_ALARM)
      .setContentIntent(openPendingIntent)
      .setOngoing(true)
      .setAutoCancel(false)
      .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
      .addAction(android.R.drawable.ic_popup_reminder, "Snooze (2m)", snoozePendingIntent)
      .build()

    val notificationManager = NotificationManagerCompat.from(context)
    try {
      notificationManager.notify(ALARM_NOTIFICATION_ID, notification)
    } catch (_: SecurityException) {}
  }

  private fun postSnoozedNotification(
    context: Context,
    deviceName: String,
    deviceAddress: String,
    minutes: Int
  ) {
    val dismissIntent = Intent(context, AlarmActionReceiver::class.java).apply {
      action = AlarmActionReceiver.ACTION_DISMISS
    }
    val dismissPendingIntent = PendingIntent.getBroadcast(
      context,
      103,
      dismissIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ANTI_LOST_ALARM_ID)
      .setSmallIcon(android.R.drawable.ic_popup_reminder)
      .setContentTitle("⏰ Snoozed: $deviceName ($minutes min)")
      .setContentText("Alarm will sound again in $minutes minutes if item is still not found.")
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setOngoing(true)
      .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss Alarm", dismissPendingIntent)
      .build()

    val notificationManager = NotificationManagerCompat.from(context)
    try {
      notificationManager.notify(ALARM_NOTIFICATION_ID, notification)
    } catch (_: SecurityException) {}
  }
}
