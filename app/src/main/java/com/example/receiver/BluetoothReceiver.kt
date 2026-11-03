package com.example.receiver

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.repository.BluetoothTrackerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BluetoothReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action ?: return
    val repository = BluetoothTrackerRepository.getInstance(context)

    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
    } else {
      @Suppress("DEPRECATION")
      intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
    }

    if (device == null) return

    val (deviceName, deviceAddress, deviceType) = extractDeviceInfo(context, device, intent)

    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        when (action) {
          BluetoothDevice.ACTION_ACL_CONNECTED -> {
            repository.handleDeviceConnected(
              deviceName = deviceName,
              deviceAddress = deviceAddress,
              deviceType = deviceType,
              isSimulated = false
            )
          }
          BluetoothDevice.ACTION_ACL_DISCONNECTED,
          BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED -> {
            repository.handleDeviceDisconnected(
              deviceName = deviceName,
              deviceAddress = deviceAddress,
              deviceType = deviceType,
              isSimulated = false
            )
          }
        }
      } finally {
        pendingResult.finish()
      }
    }
  }

  @SuppressLint("MissingPermission")
  private fun extractDeviceInfo(
    context: Context,
    device: BluetoothDevice,
    intent: Intent
  ): Triple<String, String, String> {
    val hasConnectPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.BLUETOOTH_CONNECT
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }

    val extraName = intent.getStringExtra(BluetoothDevice.EXTRA_NAME)
    val name = try {
      if (hasConnectPermission) {
        device.name ?: extraName ?: "Peripheral (${device.address.takeLast(5)})"
      } else {
        extraName ?: "Peripheral (${device.address.takeLast(5)})"
      }
    } catch (e: SecurityException) {
      extraName ?: "Bluetooth Device"
    }

    val address = try {
      device.address ?: "00:00:00:00:00:00"
    } catch (e: SecurityException) {
      "Unknown MAC"
    }

    val deviceType = try {
      if (hasConnectPermission) {
        BluetoothTrackerRepository.parseDeviceType(device.bluetoothClass)
      } else {
        "Bluetooth Peripheral"
      }
    } catch (e: SecurityException) {
      "Bluetooth Peripheral"
    }

    return Triple(name, address, deviceType)
  }
}
