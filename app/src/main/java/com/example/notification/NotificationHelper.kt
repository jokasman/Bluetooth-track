package com.example.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.util.PreferencesManager

object NotificationHelper {
  const val CHANNEL_ALERTS_ID = "channel_bluetooth_alerts"
  private const val CHANNEL_ALERTS_NAME = "Bluetooth Peripheral Alerts"
  private const val CHANNEL_ALERTS_DESC = "Instant notifications when Bluetooth peripherals connect or disconnect"

  const val CHANNEL_SERVICE_ID = "channel_bluetooth_monitor"
  private const val CHANNEL_SERVICE_NAME = "Bluetooth Monitor Service"
  private const val CHANNEL_SERVICE_DESC = "Background monitor service status"

  const val CHANNEL_ANTI_LOST_ALARM_ID = "channel_anti_lost_alarm"
  private const val CHANNEL_ANTI_LOST_ALARM_NAME = "Anti-Lost Peripheral Alarms"
  private const val CHANNEL_ANTI_LOST_ALARM_DESC = "Urgent continuous alarms for protected accessories"

  const val NOTIFICATION_SERVICE_ID = 1001
  private var notificationIdCounter = 2000

  fun createNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

      // Channel 1: High priority alerts for connect / disconnect
      val alertChannel = NotificationChannel(
        CHANNEL_ALERTS_ID,
        CHANNEL_ALERTS_NAME,
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = CHANNEL_ALERTS_DESC
        enableLights(true)
        enableVibration(true)
        setShowBadge(true)
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
      }
      notificationManager.createNotificationChannel(alertChannel)

      // Channel 2: Low priority persistent foreground service channel
      val serviceChannel = NotificationChannel(
        CHANNEL_SERVICE_ID,
        CHANNEL_SERVICE_NAME,
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = CHANNEL_SERVICE_DESC
        enableLights(false)
        enableVibration(false)
        setShowBadge(false)
      }
      notificationManager.createNotificationChannel(serviceChannel)

      // Channel 3: Critical Anti-Lost Alarm Channel
      val antiLostChannel = NotificationChannel(
        CHANNEL_ANTI_LOST_ALARM_ID,
        CHANNEL_ANTI_LOST_ALARM_NAME,
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = CHANNEL_ANTI_LOST_ALARM_DESC
        enableLights(true)
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 800, 400, 800, 400)
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
      }
      notificationManager.createNotificationChannel(antiLostChannel)
    }
  }

  fun showPeripheralAlert(
    context: Context,
    deviceName: String,
    deviceAddress: String,
    deviceType: String,
    isConnected: Boolean,
    durationMs: Long? = null,
    isMuted: Boolean = false
  ) {
    if (isMuted) return

    val prefs = PreferencesManager.getInstance(context)
    if (isConnected && !prefs.notifyOnConnect.value) return
    if (!isConnected && !prefs.notifyOnDisconnect.value) return

    // Verify notification permission on Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(
          context,
          android.Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        return
      }
    }

    val title = if (isConnected) {
      "🟢 Connected: $deviceName"
    } else {
      "🔴 Disconnected: $deviceName"
    }

    val subtitle = if (isConnected) {
      "Active peripheral ($deviceType) • Tap to view details"
    } else {
      val durationStr = durationMs?.let { formatDuration(it) } ?: "Session ended"
      "Connected for $durationStr • Tap to view history"
    }

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("EXTRA_DEVICE_ADDRESS", deviceAddress)
      putExtra("EXTRA_OPEN_TAB", "HISTORY")
    }

    val pendingIntent = PendingIntent.getActivity(
      context,
      System.currentTimeMillis().toInt(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
      .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
      .setContentTitle(title)
      .setContentText(subtitle)
      .setStyle(NotificationCompat.BigTextStyle().bigText("$subtitle\nDevice ID: $deviceAddress"))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_STATUS)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)

    if (prefs.vibrateOnAlert.value) {
      builder.setVibrate(longArrayOf(0, 250, 150, 250))
    }

    val notificationManager = NotificationManagerCompat.from(context)
    val notifId = synchronized(this) { notificationIdCounter++ }
    notificationManager.notify(notifId, builder.build())
  }

  fun buildForegroundServiceNotification(context: Context, connectedCount: Int): Notification {
    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val content = if (connectedCount > 0) {
      "Monitoring: $connectedCount peripheral(s) currently connected"
    } else {
      "Monitoring Bluetooth peripheral connections in real-time"
    }

    return NotificationCompat.Builder(context, CHANNEL_SERVICE_ID)
      .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
      .setContentTitle("BlueTrack Peripheral Monitor")
      .setContentText(content)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setContentIntent(pendingIntent)
      .setOngoing(true)
      .build()
  }

  private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return when {
      hours > 0 -> "${hours}h ${minutes % 60}m"
      minutes > 0 -> "${minutes}m ${seconds % 60}s"
      else -> "${seconds}s"
    }
  }
}
