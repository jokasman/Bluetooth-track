package com.example.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BluetoothEventEntity
import com.example.data.model.TrackedPeripheralEntity
import com.example.data.repository.BluetoothTrackerRepository
import com.example.notification.NotificationHelper
import com.example.service.ActiveAlarmState
import com.example.service.AntiLostAlarmManager
import com.example.service.BluetoothMonitorService
import com.example.util.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class PairedDeviceItem(
  val name: String,
  val address: String,
  val type: String
)

enum class HistoryFilter {
  ALL, CONNECTED, DISCONNECTED, TODAY
}

class BluetoothTrackerViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = BluetoothTrackerRepository.getInstance(application)
  private val prefs = PreferencesManager.getInstance(application)

  val serviceEnabled: StateFlow<Boolean> = prefs.serviceEnabled
  val notifyOnConnect: StateFlow<Boolean> = prefs.notifyOnConnect
  val notifyOnDisconnect: StateFlow<Boolean> = prefs.notifyOnDisconnect
  val vibrateOnAlert: StateFlow<Boolean> = prefs.vibrateOnAlert
  val autoStartOnBoot: StateFlow<Boolean> = prefs.autoStartOnBoot
  val globalAlertByDefault: StateFlow<Boolean> = prefs.globalAlertByDefault

  // Active anti-lost emergency alarm state
  val activeAlarm: StateFlow<ActiveAlarmState?> = AntiLostAlarmManager.activeAlarm

  val peripherals: StateFlow<List<TrackedPeripheralEntity>> = repository.allPeripherals
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val connectedPeripherals: StateFlow<List<TrackedPeripheralEntity>> = repository.connectedPeripherals
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val connectedCount: StateFlow<Int> = repository.connectedCount
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  val totalEventsCount: StateFlow<Int> = repository.totalEventCount
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  val recentEvents: StateFlow<List<BluetoothEventEntity>> = repository.getRecentEvents(8)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _historyFilter = MutableStateFlow(HistoryFilter.ALL)
  val historyFilter: StateFlow<HistoryFilter> = _historyFilter.asStateFlow()

  val allEvents: StateFlow<List<BluetoothEventEntity>> = repository.allEvents
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Filtered events stream for the History screen
  val filteredEvents: StateFlow<List<BluetoothEventEntity>> = combine(
    repository.allEvents,
    _searchQuery,
    _historyFilter
  ) { events, query, filter ->
    val calendar = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val todayStartMillis = calendar.timeInMillis

    events.filter { event ->
      val matchesQuery = query.isBlank() ||
        event.deviceName.contains(query, ignoreCase = true) ||
        event.deviceAddress.contains(query, ignoreCase = true) ||
        event.deviceType.contains(query, ignoreCase = true)

      val matchesFilter = when (filter) {
        HistoryFilter.ALL -> true
        HistoryFilter.CONNECTED -> event.eventType == "CONNECTED"
        HistoryFilter.DISCONNECTED -> event.eventType == "DISCONNECTED"
        HistoryFilter.TODAY -> event.timestamp >= todayStartMillis
      }

      matchesQuery && matchesFilter
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Today's events count
  private val todayStartMillis = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  }.timeInMillis

  val todayEventsCount: StateFlow<Int> = repository.getEventCountSince(todayStartMillis)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  // Hardware paired devices list
  private val _pairedDevices = MutableStateFlow<List<PairedDeviceItem>>(emptyList())
  val pairedDevices: StateFlow<List<PairedDeviceItem>> = _pairedDevices.asStateFlow()

  init {
    refreshPairedDevices()
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setHistoryFilter(filter: HistoryFilter) {
    _historyFilter.value = filter
  }

  fun toggleService(enabled: Boolean) {
    prefs.setServiceEnabled(enabled)
    val context = getApplication<Application>()
    if (enabled) {
      if (BluetoothMonitorService.hasForegroundServicePermission(context)) {
        BluetoothMonitorService.startService(context)
      }
    } else {
      BluetoothMonitorService.stopService(context)
    }
  }

  fun setNotifyOnConnect(enabled: Boolean) = prefs.setNotifyOnConnect(enabled)
  fun setNotifyOnDisconnect(enabled: Boolean) = prefs.setNotifyOnDisconnect(enabled)
  fun setVibrateOnAlert(enabled: Boolean) = prefs.setVibrateOnAlert(enabled)
  fun setAutoStartOnBoot(enabled: Boolean) = prefs.setAutoStartOnBoot(enabled)
  fun setGlobalAlertByDefault(enabled: Boolean) = prefs.setGlobalAlertByDefault(enabled)

  fun updateNickname(address: String, nickname: String?) {
    viewModelScope.launch {
      repository.updateNickname(address, nickname)
    }
  }

  fun toggleFavorite(address: String) {
    viewModelScope.launch {
      repository.toggleFavorite(address)
    }
  }

  fun toggleMuted(address: String) {
    viewModelScope.launch {
      repository.toggleMuted(address)
    }
  }

  fun updateAlertMode(address: String, mode: String) {
    viewModelScope.launch {
      repository.updateAlertMode(address, mode)
    }
  }

  fun updateAntiLostConfig(
    address: String,
    enabled: Boolean,
    alarmDisconnect: Boolean,
    alarmConnect: Boolean
  ) {
    viewModelScope.launch {
      repository.updateAntiLostConfig(address, enabled, alarmDisconnect, alarmConnect)
    }
  }

  fun dismissActiveAlarm() {
    AntiLostAlarmManager.dismiss(getApplication())
  }

  fun snoozeActiveAlarm(minutes: Int = 2) {
    AntiLostAlarmManager.snooze(getApplication(), minutes)
  }

  fun testAntiLostAlarm(name: String, address: String) {
    AntiLostAlarmManager.startAlarm(
      context = getApplication(),
      deviceName = name,
      deviceAddress = address,
      triggerType = "DISCONNECTED"
    )
  }

  fun deleteEvent(id: Long) {
    viewModelScope.launch {
      repository.deleteEvent(id)
    }
  }

  fun clearAllEvents() {
    viewModelScope.launch {
      repository.clearAllEvents()
    }
  }

  fun deletePeripheral(address: String) {
    viewModelScope.launch {
      repository.deletePeripheral(address)
    }
  }

  fun simulateEvent(
    name: String,
    address: String,
    deviceType: String,
    isConnect: Boolean
  ) {
    viewModelScope.launch {
      if (isConnect) {
        repository.handleDeviceConnected(
          deviceName = name,
          deviceAddress = address,
          deviceType = deviceType,
          isSimulated = true
        )
      } else {
        repository.handleDeviceDisconnected(
          deviceName = name,
          deviceAddress = address,
          deviceType = deviceType,
          isSimulated = true
        )
      }
    }
  }

  fun testNotificationAlert(isConnected: Boolean) {
    val context = getApplication<Application>()
    NotificationHelper.showPeripheralAlert(
      context = context,
      deviceName = if (isConnected) "AirPods Pro (Test)" else "Sony WH-1000XM5 (Test)",
      deviceAddress = "AA:BB:CC:DD:EE:01",
      deviceType = "Headphones",
      isConnected = isConnected,
      durationMs = if (isConnected) null else 1845000L,
      isMuted = false
    )
  }

  @SuppressLint("MissingPermission")
  fun refreshPairedDevices() {
    val context = getApplication<Application>()
    val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.BLUETOOTH_CONNECT
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }

    if (!hasPermission) {
      _pairedDevices.value = emptyList()
      return
    }

    try {
      val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
      val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
      if (adapter != null && adapter.isEnabled) {
        val bonded = adapter.bondedDevices
        if (bonded != null) {
          val list = bonded.map { dev ->
            val name = dev.name ?: "Unnamed Device"
            val type = BluetoothTrackerRepository.parseDeviceType(dev.bluetoothClass)
            PairedDeviceItem(name = name, address = dev.address, type = type)
          }
          _pairedDevices.value = list
        }
      }
    } catch (e: Exception) {
      _pairedDevices.value = emptyList()
    }
  }

  fun generateExportData(): String {
    val events = allEvents.value
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val sb = StringBuilder()
    sb.append("Timestamp,Event,Device Name,MAC Address,Type,Duration (seconds),Simulated\n")
    for (event in events) {
      val formattedDate = sdf.format(Date(event.timestamp))
      val durationSec = event.durationMs?.let { it / 1000 } ?: 0
      sb.append("\"$formattedDate\",")
      sb.append("\"${event.eventType}\",")
      sb.append("\"${event.deviceName.replace("\"", "\"\"")}\",")
      sb.append("\"${event.deviceAddress}\",")
      sb.append("\"${event.deviceType}\",")
      sb.append("$durationSec,")
      sb.append("${event.isSimulated}\n")
    }
    return sb.toString()
  }
}
