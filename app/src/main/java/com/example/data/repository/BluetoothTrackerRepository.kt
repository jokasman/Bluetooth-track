package com.example.data.repository

import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.BluetoothErrorLogEntity
import com.example.data.model.BluetoothEventEntity
import com.example.data.model.TrackedPeripheralEntity
import com.example.notification.NotificationHelper
import com.example.service.AntiLostAlarmManager
import com.example.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class BluetoothTrackerRepository(
  private val context: Context,
  private val database: AppDatabase = AppDatabase.getInstance(context)
) {
  private val eventDao = database.bluetoothEventDao()
  private val peripheralDao = database.trackedPeripheralDao()
  private val errorLogDao = database.bluetoothErrorLogDao()
  private val prefs = PreferencesManager.getInstance(context)

  val allEvents: Flow<List<BluetoothEventEntity>> = eventDao.getAllEvents()
  val allPeripherals: Flow<List<TrackedPeripheralEntity>> = peripheralDao.getAllPeripherals()
  val connectedPeripherals: Flow<List<TrackedPeripheralEntity>> = peripheralDao.getConnectedPeripherals()
  val connectedCount: Flow<Int> = peripheralDao.getConnectedCount()
  val totalEventCount: Flow<Int> = eventDao.getTotalEventCount()
  val allErrorLogs: Flow<List<BluetoothErrorLogEntity>> = errorLogDao.getAllLogs()
  val errorLogCount: Flow<Int> = errorLogDao.getErrorCount()

  fun getRecentEvents(limit: Int): Flow<List<BluetoothEventEntity>> = eventDao.getRecentEvents(limit)

  fun getEventsByType(type: String): Flow<List<BluetoothEventEntity>> = eventDao.getEventsByType(type)

  fun searchEvents(query: String): Flow<List<BluetoothEventEntity>> = eventDao.searchEvents(query)

  fun getEventsForDevice(address: String): Flow<List<BluetoothEventEntity>> =
    eventDao.getEventsForDevice(address)

  fun getEventCountSince(sinceTimestamp: Long): Flow<Int> =
    eventDao.getEventCountSince(sinceTimestamp)

  suspend fun handleDeviceConnected(
    deviceName: String,
    deviceAddress: String,
    deviceType: String,
    isSimulated: Boolean = false,
    note: String? = null
  ) = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()

    // Query existing peripheral record if any
    val existing = peripheralDao.getPeripheralByAddress(deviceAddress)
    val displayName = existing?.displayName ?: deviceName

    // Log connection event
    val event = BluetoothEventEntity(
      deviceName = displayName,
      deviceAddress = deviceAddress,
      deviceType = deviceType,
      eventType = "CONNECTED",
      timestamp = now,
      durationMs = null,
      isSimulated = isSimulated,
      note = note
    )
    eventDao.insertEvent(event)

    // Update tracked peripheral table
    if (existing != null) {
      peripheralDao.markConnected(deviceAddress, true, now)
    } else {
      peripheralDao.upsertPeripheral(
        TrackedPeripheralEntity(
          deviceAddress = deviceAddress,
          deviceName = deviceName,
          customNickname = null,
          deviceType = deviceType,
          isConnected = true,
          lastConnectedTimestamp = now,
          totalConnectionCount = 1
        )
      )
    }

    // Evaluate notification policy: Per-device override or global default
    val shouldNotify = when (existing?.alertMode) {
      TrackedPeripheralEntity.ALERT_MODE_ALWAYS_ALERT -> true
      TrackedPeripheralEntity.ALERT_MODE_MUTED -> false
      else -> {
        if (existing?.isMuted == true) false
        else prefs.globalAlertByDefault.value
      }
    }

    if (shouldNotify) {
      NotificationHelper.showPeripheralAlert(
        context = context,
        deviceName = displayName,
        deviceAddress = deviceAddress,
        deviceType = deviceType,
        isConnected = true,
        durationMs = null,
        isMuted = false
      )
    }

    // Check Anti-Lost Alarm on Connect
    if (existing?.isAntiLostEnabled == true && existing.alarmOnConnect) {
      AntiLostAlarmManager.startAlarm(context, displayName, deviceAddress, "CONNECTED")
    }
  }

  suspend fun handleDeviceDisconnected(
    deviceName: String,
    deviceAddress: String,
    deviceType: String,
    isSimulated: Boolean = false,
    note: String? = null
  ) = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()

    // Determine connection duration from last connected event
    val lastConnectEvent = eventDao.getLastConnectedEvent(deviceAddress)
    val sessionDurationMs = if (lastConnectEvent != null && lastConnectEvent.timestamp <= now) {
      now - lastConnectEvent.timestamp
    } else {
      null
    }

    val existing = peripheralDao.getPeripheralByAddress(deviceAddress)
    val displayName = existing?.displayName ?: deviceName

    // Log disconnection event
    val event = BluetoothEventEntity(
      deviceName = displayName,
      deviceAddress = deviceAddress,
      deviceType = deviceType,
      eventType = "DISCONNECTED",
      timestamp = now,
      durationMs = sessionDurationMs,
      isSimulated = isSimulated,
      note = note
    )
    eventDao.insertEvent(event)

    // Update peripheral
    if (existing != null) {
      peripheralDao.markDisconnected(deviceAddress, now, sessionDurationMs ?: 0L)
    } else {
      peripheralDao.upsertPeripheral(
        TrackedPeripheralEntity(
          deviceAddress = deviceAddress,
          deviceName = deviceName,
          deviceType = deviceType,
          isConnected = false,
          lastDisconnectedTimestamp = now,
          totalConnectionCount = 1,
          totalConnectedDurationMs = sessionDurationMs ?: 0L
        )
      )
    }

    // Evaluate notification policy: Per-device override or global default
    val shouldNotify = when (existing?.alertMode) {
      TrackedPeripheralEntity.ALERT_MODE_ALWAYS_ALERT -> true
      TrackedPeripheralEntity.ALERT_MODE_MUTED -> false
      else -> {
        if (existing?.isMuted == true) false
        else prefs.globalAlertByDefault.value
      }
    }

    if (shouldNotify) {
      NotificationHelper.showPeripheralAlert(
        context = context,
        deviceName = displayName,
        deviceAddress = deviceAddress,
        deviceType = deviceType,
        isConnected = false,
        durationMs = sessionDurationMs,
        isMuted = false
      )
    }

    // Check Anti-Lost Alarm on Disconnect (primary safeguard against forgetting items)
    if (existing?.isAntiLostEnabled == true && existing.alarmOnDisconnect) {
      AntiLostAlarmManager.startAlarm(context, displayName, deviceAddress, "DISCONNECTED")
    }
  }

  suspend fun updateNickname(address: String, nickname: String?) = withContext(Dispatchers.IO) {
    peripheralDao.updateNickname(address, nickname?.trim()?.ifEmpty { null })
  }

  suspend fun toggleFavorite(address: String) = withContext(Dispatchers.IO) {
    peripheralDao.toggleFavorite(address)
  }

  suspend fun toggleMuted(address: String) = withContext(Dispatchers.IO) {
    peripheralDao.toggleMuted(address)
  }

  suspend fun updateAlertMode(address: String, mode: String) = withContext(Dispatchers.IO) {
    peripheralDao.updateAlertMode(address, mode)
  }

  suspend fun updateAntiLostConfig(
    address: String,
    enabled: Boolean,
    alarmDisconnect: Boolean,
    alarmConnect: Boolean
  ) = withContext(Dispatchers.IO) {
    peripheralDao.updateAntiLostConfig(address, enabled, alarmDisconnect, alarmConnect)
  }

  suspend fun deleteEvent(id: Long) = withContext(Dispatchers.IO) {
    eventDao.deleteEventById(id)
  }

  suspend fun clearAllEvents() = withContext(Dispatchers.IO) {
    eventDao.clearAllEvents()
  }

  suspend fun trackDevice(
    deviceName: String,
    deviceAddress: String,
    deviceType: String
  ): TrackedPeripheralEntity = withContext(Dispatchers.IO) {
    val existing = peripheralDao.getPeripheralByAddress(deviceAddress)
    if (existing != null) {
      existing
    } else {
      val newEntity = TrackedPeripheralEntity(
        deviceAddress = deviceAddress,
        deviceName = deviceName,
        customNickname = null,
        deviceType = deviceType,
        isConnected = false,
        lastConnectedTimestamp = 0L,
        totalConnectionCount = 0
      )
      peripheralDao.upsertPeripheral(newEntity)
      newEntity
    }
  }

  suspend fun deletePeripheral(address: String) = withContext(Dispatchers.IO) {
    peripheralDao.deletePeripheral(address)
  }

  suspend fun deleteErrorLog(id: Long) = withContext(Dispatchers.IO) {
    errorLogDao.deleteLogById(id)
  }

  suspend fun clearAllErrorLogs() = withContext(Dispatchers.IO) {
    errorLogDao.clearAllLogs()
  }

  companion object {
    @Volatile
    private var INSTANCE: BluetoothTrackerRepository? = null

    fun getInstance(context: Context): BluetoothTrackerRepository {
      return INSTANCE ?: synchronized(this) {
        val instance = BluetoothTrackerRepository(context.applicationContext)
        INSTANCE = instance
        instance
      }
    }

    fun parseDeviceType(bluetoothClass: BluetoothClass?): String {
      if (bluetoothClass == null) return "Bluetooth Device"
      val major = bluetoothClass.majorDeviceClass
      val deviceClass = bluetoothClass.deviceClass

      return when {
        major == BluetoothClass.Device.Major.AUDIO_VIDEO -> {
          when (deviceClass) {
            BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES -> "Headphones"
            BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET -> "Headset / Earbuds"
            BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO -> "Car Audio"
            BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER -> "Speaker"
            else -> "Audio Device"
          }
        }
        major == BluetoothClass.Device.Major.WEARABLE -> "Smartwatch / Wearable"
        major == BluetoothClass.Device.Major.PERIPHERAL -> {
          when (deviceClass) {
            BluetoothClass.Device.Major.PERIPHERAL or 0x0540 -> "Keyboard"
            BluetoothClass.Device.Major.PERIPHERAL or 0x0580 -> "Mouse / Pointer"
            BluetoothClass.Device.Major.PERIPHERAL or 0x0504 -> "Gamepad / Controller"
            else -> "Input Peripheral"
          }
        }
        major == BluetoothClass.Device.Major.COMPUTER -> "Computer / Laptop"
        major == BluetoothClass.Device.Major.PHONE -> "Smartphone"
        major == BluetoothClass.Device.Major.HEALTH -> "Health Device"
        else -> "Bluetooth Peripheral"
      }
    }
  }
}
