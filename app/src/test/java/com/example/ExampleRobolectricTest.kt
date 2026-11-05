package com.example

import com.example.data.model.TrackedPeripheralEntity
import com.example.data.repository.BluetoothTrackerRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTestLogic {

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun parseDeviceType_nullClass_returnsDefault() {
    val result = BluetoothTrackerRepository.parseDeviceType(null)
    assertEquals("Bluetooth Device", result)
  }

  @Test
  fun trackedPeripheral_defaultAlertModeAndAntiLost() {
    val peripheral = TrackedPeripheralEntity(
      deviceAddress = "AA:BB:CC:DD:EE:FF",
      deviceName = "Sony Headphones",
      deviceType = "Headphones"
    )
    assertEquals(TrackedPeripheralEntity.ALERT_MODE_DEFAULT, peripheral.alertMode)
    assertFalse(peripheral.isAntiLostEnabled)
    assertTrue(peripheral.alarmOnDisconnect)
    assertFalse(peripheral.alarmOnConnect)
    assertEquals("Sony Headphones", peripheral.displayName)
  }

  @Test
  fun trackedPeripheral_customNicknameDisplay() {
    val peripheral = TrackedPeripheralEntity(
      deviceAddress = "AA:BB:CC:DD:EE:FF",
      deviceName = "WH-1000XM5",
      customNickname = "My Daily Driver",
      deviceType = "Headphones",
      isAntiLostEnabled = true
    )
    assertEquals("My Daily Driver", peripheral.displayName)
    assertTrue(peripheral.isAntiLostEnabled)
  }
}
