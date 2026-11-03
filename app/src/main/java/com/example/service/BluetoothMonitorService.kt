package com.example.service

import android.Manifest
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.example.data.repository.BluetoothTrackerRepository
import com.example.notification.NotificationHelper
import com.example.receiver.BluetoothReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BluetoothMonitorService : Service() {

  private val serviceJob = Job()
  private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
  private var dynamicReceiver: BluetoothReceiver? = null
  private lateinit var repository: BluetoothTrackerRepository
  private var isInitialized = false

  override fun onCreate() {
    super.onCreate()
    repository = BluetoothTrackerRepository.getInstance(this)
    NotificationHelper.createNotificationChannels(this)
  }

  private fun setupMonitoring() {
    if (isInitialized) return
    isInitialized = true

    // Register dynamic receiver for real-time Bluetooth ACL connection events
    val filter = IntentFilter().apply {
      addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
      addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
      addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
      addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
    }
    val receiver = BluetoothReceiver()
    dynamicReceiver = receiver
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
    } else {
      registerReceiver(receiver, filter)
    }

    // Monitor connected count to update foreground notification
    serviceScope.launch {
      repository.connectedCount.collectLatest { count ->
        val notification = NotificationHelper.buildForegroundServiceNotification(this@BluetoothMonitorService, count)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NotificationHelper.NOTIFICATION_SERVICE_ID, notification)
      }
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP_SERVICE) {
      stopForeground(STOP_FOREGROUND_REMOVE)
      stopSelf()
      return START_NOT_STICKY
    }

    // Guard against starting FGS connectedDevice without runtime permissions (Android 14+)
    if (!hasForegroundServicePermission(this)) {
      stopSelf()
      return START_NOT_STICKY
    }

    val notification = NotificationHelper.buildForegroundServiceNotification(this, 0)
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        startForeground(
          NotificationHelper.NOTIFICATION_SERVICE_ID,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        )
      } else {
        startForeground(NotificationHelper.NOTIFICATION_SERVICE_ID, notification)
      }
      setupMonitoring()
    } catch (e: SecurityException) {
      stopSelf()
      return START_NOT_STICKY
    } catch (e: Exception) {
      stopSelf()
      return START_NOT_STICKY
    }

    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()
    dynamicReceiver?.let {
      try {
        unregisterReceiver(it)
      } catch (_: Exception) {}
    }
    serviceJob.cancel()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  companion object {
    const val ACTION_STOP_SERVICE = "com.example.service.ACTION_STOP_SERVICE"

    fun hasForegroundServicePermission(context: Context): Boolean {
      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
      } else {
        true
      }
    }

    fun startService(context: Context) {
      if (!hasForegroundServicePermission(context)) {
        return
      }
      val intent = Intent(context, BluetoothMonitorService::class.java)
      try {
        ContextCompat.startForegroundService(context, intent)
      } catch (e: Exception) {
        try {
          context.startService(intent)
        } catch (_: Exception) {}
      }
    }

    fun stopService(context: Context) {
      val intent = Intent(context, BluetoothMonitorService::class.java).apply {
        action = ACTION_STOP_SERVICE
      }
      try {
        context.startService(intent)
      } catch (_: Exception) {}
    }
  }
}
